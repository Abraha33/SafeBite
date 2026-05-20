package unab.edu.co.abrahamcaceres.safebite.ui.auth

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.data.repository.FirebaseFirestoreRepository
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentScannerBinding
import unab.edu.co.abrahamcaceres.safebite.model.ProductScan
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk
import unab.edu.co.abrahamcaceres.safebite.model.cloud.ScanHistoryModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AllergenViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AllergenViewModelFactory
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ScanHistoryViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ScanHistoryViewModelFactory

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val allergenViewModel: AllergenViewModel by viewModels {
        AllergenViewModelFactory(requireActivity().application)
    }

    private val scanHistoryViewModel: ScanHistoryViewModel by viewModels {
        ScanHistoryViewModelFactory(requireActivity().application)
    }

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null

    private val firestoreRepo = FirebaseFirestoreRepository()

    @Volatile
    private var allergenKeywords: List<AllergenKeyword> = emptyList()

    private var lastPersistedFingerprint: String? = null
    private var latestRecognizedText: String? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!isAdded) return@registerForActivityResult
            if (granted) {
                startCameraFlow()
            } else {
                showWarningHUD(
                    title = getString(R.string.scanner_status_permission_denied),
                    message = getString(R.string.scanner_status_permission_body)
                )
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri ->
            if (!isAdded || imageUri == null) return@registerForActivityResult
            analyzeGalleryImage(imageUri)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        configurePreview()
        observeAllergens()
        setupActions()
        renderDynamicHUD(riskLevel = ScanRisk.SAFE, matchedAllergen = null)
        ensureCameraPermissionAndStart()
    }

    override fun onDestroyView() {
        imageAnalysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        imageAnalysis = null
        cameraProvider = null
        camera = null
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        textRecognizer.close()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }

    private fun setupActions() {
        binding.buttonOpenGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.buttonCapture.setOnClickListener {
            captureCurrentScan()
        }

        binding.buttonProfile.setOnClickListener {
            findNavController().navigate(R.id.action_scanner_to_profile)
        }
    }

    private fun captureCurrentScan() {
        val text = latestRecognizedText
        if (text.isNullOrBlank()) {
            vibrateDangerFeedback()
            showWarningHUD(
                title = getString(R.string.scanner_status_waiting),
                message = getString(R.string.hud_error_message)
            )
            return
        }
        val riskLevel = evaluateRiskLevel(text)
        val fingerprint = "$riskLevel|${text.trim().lowercase(Locale.getDefault())}"
        if (fingerprint == lastPersistedFingerprint) return

        lastPersistedFingerprint = fingerprint
        if (riskLevel != ScanRisk.SAFE) {
            vibrateDangerFeedback()
        }

        scanHistoryViewModel.insertScan(
            ProductScan.crear(
                textoDetectado = text,
                nivelRiesgo = riskLevel
            )
        )
    }

    private fun configurePreview() {
        binding.viewFinder.scaleType = PreviewView.ScaleType.FILL_CENTER
        binding.viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }

    private fun observeAllergens() {
        allergenViewModel.allergens.observe(viewLifecycleOwner) { allergens ->
            allergenKeywords = allergens
                .mapNotNull { allergen ->
                    val normalized = allergen.getNormalizedName().trim()
                    val display = allergen.getDisplayName().trim()
                    if (normalized.isBlank() || display.isBlank()) {
                        null
                    } else {
                        AllergenKeyword(displayName = display, normalizedName = normalized)
                    }
                }
                .distinctBy { it.normalizedName }
        }
    }

    private fun ensureCameraPermissionAndStart() {
        if (hasCameraPermission()) {
            startCameraFlow()
            return
        }
        showWarningHUD(
            title = getString(R.string.scanner_status_permission_required),
            message = getString(R.string.scanner_status_permission_body)
        )
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCameraFlow() {
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener(
            {
                runCatching {
                    providerFuture.get()
                }.onSuccess { provider ->
                    cameraProvider = provider
                    bindCameraUseCases(provider)
                }.onFailure { throwable ->
                    handleAnalysisError(throwable as? Exception ?: RuntimeException(throwable))
                }
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun bindCameraUseCases(provider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also { useCase ->
            useCase.surfaceProvider = binding.viewFinder.surfaceProvider
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { useCase ->
                useCase.setAnalyzer(
                    cameraExecutor,
                    LabelTextAnalyzer()
                )
            }
        imageAnalysis = analysis

        provider.unbindAll()
        camera = provider.bindToLifecycle(
            viewLifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis
        )

        triggerAutoFocus()
    }

    private fun triggerAutoFocus() {
        val viewFinder = binding.viewFinder
        val cam = camera ?: return
        if (viewFinder.width <= 0 || viewFinder.height <= 0) return

        val factory: MeteringPointFactory = viewFinder.meteringPointFactory
        val point = factory.createPoint(
            viewFinder.width / 2f,
            viewFinder.height / 2f,
            0.15f
        )
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    private fun analyzeGalleryImage(imageUri: Uri) {
        runCatching {
            InputImage.fromFilePath(requireContext(), imageUri)
        }.onSuccess { image ->
            processImage(image)
        }.onFailure { throwable ->
            handleAnalysisError(throwable as? Exception ?: RuntimeException(throwable))
        }
    }

    private fun processImage(image: InputImage) {
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                analyzeText(visionText.text)
            }
            .addOnFailureListener { throwable ->
                handleAnalysisError(Exception(throwable))
            }
    }

    private fun analyzeText(recognizedText: String) {
        val currentBinding = _binding ?: return
        latestRecognizedText = recognizedText

        if (recognizedText.isBlank()) {
            currentBinding.root.post {
                renderDynamicHUD(riskLevel = ScanRisk.SAFE, matchedAllergen = null)
            }
            return
        }

        val riskLevel = evaluateRiskLevel(recognizedText)
        val detectedAllergens = findMatchedAllergens(recognizedText)

        currentBinding.root.post {
            if (riskLevel != ScanRisk.SAFE) {
                val allergensStr = detectedAllergens.joinToString(", ")
                showDetectedChip(allergensStr)
                renderDynamicHUD(riskLevel = riskLevel, matchedAllergen = allergensStr)
                persistScanIfNeeded(
                    detectedText = recognizedText,
                    riskLevel = riskLevel
                )
            } else {
                hideDetectedChip()
                renderDynamicHUD(riskLevel = ScanRisk.SAFE, matchedAllergen = null)
                persistScanIfNeeded(
                    detectedText = recognizedText,
                    riskLevel = ScanRisk.SAFE
                )
            }
        }
    }

    private fun sanitizeText(text: String): String {
        return text.lowercase(Locale.getDefault())
            .replace(Regex("[,.\\(\\)]"), " ")
            .trim()
    }

    private fun evaluateRiskLevel(text: String): String {
        val cleaned = sanitizeText(text)
        if (cleaned.isBlank()) return ScanRisk.SAFE

        var hasExact = false
        var hasPartial = false

        for (keyword in allergenKeywords) {
            val keywordNorm = keyword.normalizedName
            if (cleaned.contains(keywordNorm)) {
                hasExact = true
                break
            }
            if (!hasPartial && isPartialMatch(cleaned, keywordNorm)) {
                hasPartial = true
            }
        }

        return when {
            hasExact -> ScanRisk.DANGER
            hasPartial -> ScanRisk.WARNING
            else -> ScanRisk.SAFE
        }
    }

    private fun findMatchedAllergens(text: String): List<String> {
        val cleaned = sanitizeText(text)
        if (cleaned.isBlank()) return emptyList()

        val matched = mutableListOf<String>()
        for (keyword in allergenKeywords) {
            if (matched.contains(keyword.displayName)) continue
            if (cleaned.contains(keyword.normalizedName)) {
                matched.add(keyword.displayName)
            }
        }
        return matched
    }

    private fun isPartialMatch(cleanedText: String, keywordNorm: String): Boolean {
        val textWords = cleanedText.split(Regex("\\s+")).filter { it.length >= 3 }
        val keywordWords = keywordNorm.split(Regex("\\s+")).filter { it.length >= 3 }
        for (tw in textWords) {
            for (kw in keywordWords) {
                if (tw != kw && (tw.contains(kw) || kw.contains(tw))) return true
            }
        }
        return false
    }

    private fun showDetectedChip(allergenText: String) {
        binding.chipDetectedIngredient.apply {
            text = "\u26A0\uFE0F $allergenText"
            visibility = View.VISIBLE
        }
    }

    private fun hideDetectedChip() {
        binding.chipDetectedIngredient.visibility = View.GONE
    }

    private fun handleAnalysisError(error: Exception) {
        val currentBinding = _binding ?: return
        error.printStackTrace()
        currentBinding.root.post {
            showWarningHUD(
                title = getString(R.string.scanner_status_error),
                message = getString(R.string.hud_error_message)
            )
        }
    }

    private fun persistScanIfNeeded(detectedText: String, riskLevel: String) {
        val normalizedText = detectedText.trim().lowercase(Locale.getDefault())
        if (normalizedText.isBlank()) return

        val fingerprint = "$riskLevel|$normalizedText"
        if (fingerprint == lastPersistedFingerprint) return

        lastPersistedFingerprint = fingerprint
        if (riskLevel != ScanRisk.SAFE) {
            vibrateDangerFeedback()
        }

        val scan = ProductScan.crear(
            textoDetectado = detectedText,
            nivelRiesgo = riskLevel
        )

        scanHistoryViewModel.insertScan(scan)

        val matchedAllergens = if (riskLevel != ScanRisk.SAFE) {
            findMatchedAllergens(detectedText)
        } else {
            emptyList()
        }
        uploadScanToFirestore(detectedText, riskLevel, matchedAllergens)
    }

    private fun uploadScanToFirestore(
        detectedText: String,
        riskLevel: String,
        matchedAllergens: List<String>
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val scanModel = ScanHistoryModel(
            scanId = "",
            scanDate = System.currentTimeMillis(),
            productName = ProductScan.crear(detectedText, riskLevel).getProductName(),
            rawTextDetected = detectedText,
            status = riskLevel,
            matchedAllergens = matchedAllergens
        )
        viewLifecycleOwner.lifecycleScope.launch {
            firestoreRepo.addScanToHistory(uid, scanModel)
        }
    }

    private fun vibrateDangerFeedback() {
        val vibrationEffect = VibrationEffect.createOneShot(180L, VibrationEffect.DEFAULT_AMPLITUDE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibrationEffect)
        }
    }

    private fun renderDynamicHUD(riskLevel: String, matchedAllergen: String?) {
        val card = binding.hudStatusCard
        val ctx = requireContext()

        val isDanger = riskLevel == ScanRisk.DANGER
        val isWarning = riskLevel == ScanRisk.WARNING
        val bgColor = when {
            isDanger -> R.color.risk_danger_container
            isWarning -> R.color.risk_warning_container
            else -> R.color.risk_safe_container
        }
        val textColor = when {
            isDanger -> R.color.risk_danger_on
            isWarning -> R.color.risk_warning_on
            else -> R.color.risk_safe_on
        }

        val newBg = ContextCompat.getColor(ctx, bgColor)
        val newText = ContextCompat.getColor(ctx, textColor)

        ValueAnimator.ofObject(ArgbEvaluator(), card.cardBackgroundColor.defaultColor, newBg).apply {
            duration = 350
            addUpdateListener { animator ->
                card.setCardBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }

        binding.textHudTitle.text = when {
            isDanger -> getString(R.string.hud_danger_title)
            isWarning -> getString(R.string.hud_warning_title)
            else -> getString(R.string.hud_safe_title)
        }
        binding.textHudTitle.setTextColor(newText)

        binding.textHudMessage.text = when {
            isDanger && matchedAllergen != null ->
                getString(R.string.hud_danger_message, matchedAllergen)
            isWarning && matchedAllergen != null ->
                getString(R.string.hud_warning_message, matchedAllergen)
            else -> getString(R.string.hud_safe_message)
        }
        binding.textHudMessage.setTextColor(newText)
    }

    private fun showWarningHUD(title: String, message: String) {
        val card = binding.hudStatusCard
        val ctx = requireContext()
        val newBg = ContextCompat.getColor(ctx, R.color.risk_warning_container)
        val newText = ContextCompat.getColor(ctx, R.color.risk_warning_on)

        ValueAnimator.ofObject(ArgbEvaluator(), card.cardBackgroundColor.defaultColor, newBg).apply {
            duration = 350
            addUpdateListener { animator ->
                card.setCardBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }

        binding.textHudTitle.text = title
        binding.textHudTitle.setTextColor(newText)
        binding.textHudMessage.text = message
        binding.textHudMessage.setTextColor(newText)
    }

    private data class AllergenKeyword(
        val displayName: String,
        val normalizedName: String
    )

    private inner class LabelTextAnalyzer : ImageAnalysis.Analyzer {

        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return
            }

            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    analyzeText(visionText.text)
                }
                .addOnFailureListener { throwable ->
                    handleAnalysisError(Exception(throwable))
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}
