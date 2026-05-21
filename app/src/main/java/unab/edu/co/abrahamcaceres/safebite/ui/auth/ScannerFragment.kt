package unab.edu.co.abrahamcaceres.safebite.ui.auth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var scanFrozen = false

    @Volatile
    private var allergenKeywords: List<AllergenKeyword> = emptyList()

    private var lastPersistedFingerprint: String? = null
    private var latestRecognizedText: String? = null

    private var stableFrameCount = 0
    private var lastFrameFingerprint: String? = null
    private var autoTriggered = false

    companion object {
        private const val MIN_STABLE_FRAMES = 5
    }

    private val labelStructuralKeywords = setOf(
        "ingredientes", "contiene", "composición", "composicion",
        "tabla", "valores", "nutricional", "nutrition", "ingredients",
        "elaborado", "fabricado", "puede contener", "trazas",
        "conservar", "almacenar", "peso", "neto", "lote",
        "fecha", "vencimiento", "consumir"
    )

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
        renderNeutralHUD()
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
            triggerManualScan()
        }

        binding.buttonProfile.setOnClickListener {
            findNavController().navigate(R.id.action_scanner_to_profile)
        }
    }

    private fun triggerManualScan() {
        val text = latestRecognizedText
        if (text.isNullOrBlank()) {
            vibrateDangerFeedback()
            showWarningHUD(
                title = getString(R.string.scanner_status_waiting),
                message = getString(R.string.hud_error_message)
            )
            return
        }
        autoTriggered = true
        freezeCamera()
        finalizeScan(text)
    }

    private fun autoTriggerScan(text: String) {
        if (autoTriggered || scanFrozen) return
        autoTriggered = true
        freezeCamera()
        finalizeScan(text)
    }

    private fun finalizeScan(text: String) {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100).apply {
            startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            release()
        }

        val riskLevel = evaluateRiskLevel(text)

        val scan = ProductScan.crear(
            textoDetectado = text,
            nivelRiesgo = riskLevel
        )
        scanHistoryViewModel.insertScan(scan) { scanId ->
            uploadScanToFirestore(text, riskLevel)
            val bundle = Bundle().apply { putLong("scanId", scanId) }
            findNavController().navigate(R.id.action_scanner_to_detail, bundle)
        }
    }

    private fun freezeCamera() {
        scanFrozen = true
        imageAnalysis?.clearAnalyzer()
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    renderNeutralHUD(
                        title = getString(R.string.gallery_analyzing_title),
                        message = getString(R.string.gallery_analyzing_message)
                    )
                }

                val bitmap = BitmapFactory.decodeStream(
                    requireContext().contentResolver.openInputStream(imageUri)
                ) ?: throw IllegalStateException("Bitmap null")

                val image = InputImage.fromBitmap(bitmap, 0)

                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        analyzeGalleryText(visionText.text)
                    }
                    .addOnFailureListener { throwable ->
                        handleAnalysisError(throwable as? Exception ?: RuntimeException(throwable))
                    }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    handleAnalysisError(e)
                }
            }
        }
    }

    private fun analyzeGalleryText(recognizedText: String) {
        val currentBinding = _binding ?: return
        latestRecognizedText = recognizedText

        val cleaned = sanitizeText(recognizedText)
        if (cleaned.isBlank() || !hasNutritionalContent(cleaned)) {
            currentBinding.root.post {
                renderNeutralHUD(
                    title = getString(R.string.gallery_no_info_title),
                    message = getString(R.string.gallery_no_info_message)
                )
            }
            return
        }

        analyzeIngredientsFromGallery(cleaned)
    }

    private fun hasNutritionalContent(cleaned: String): Boolean {
        if (cleaned.length < 10) return false
        val galleryNutritionalKeywords = setOf(
            "ingredientes", "contiene", "composición", "composicion",
            "tabla", "información", "informacion", "valores",
            "nutricional", "ingredients", "nutrition", "nutritional",
            "elaborado", "fabricado", "puede contener", "trazas"
        )
        return galleryNutritionalKeywords.any { cleaned.contains(it) }
    }

    private fun analyzeIngredientsFromGallery(cleaned: String) {
        val riskLevel = evaluateRiskLevel(cleaned)
        val detectedAllergens = findMatchedAllergens(cleaned)

        _binding?.root?.post {
            when (riskLevel) {
                ScanRisk.DANGER -> {
                    val allergensStr = detectedAllergens.joinToString(", ")
                    showDetectedChip(allergensStr)
                    renderDynamicHUD(riskLevel = riskLevel, matchedAllergen = allergensStr)
                    vibrateDangerFeedback()
                }
                ScanRisk.WARNING -> {
                    renderDynamicHUD(
                        riskLevel = riskLevel,
                        matchedAllergen = detectedAllergens.firstOrNull()
                    )
                }
                else -> {
                    renderDynamicHUD(
                        title = getString(R.string.hud_safe_title),
                        message = getString(R.string.gallery_safe_message),
                        riskLevel = riskLevel,
                        matchedAllergen = null
                    )
                    ToneGenerator(AudioManager.STREAM_MUSIC, 100).apply {
                        startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                        release()
                    }
                }
            }
        }
    }

    private fun analyzeText(recognizedText: String) {
        val currentBinding = _binding ?: return
        latestRecognizedText = recognizedText

        val cleaned = sanitizeText(recognizedText)

        if (cleaned.isBlank() || !isValidLabelText(cleaned)) {
            currentBinding.root.post {
                hideDetectedChip()
                hideTrackingIndicator()
                renderNeutralHUD()
            }
            resetStableCounter()
            return
        }

        val riskLevel = evaluateRiskLevel(cleaned)
        val detectedAllergens = findMatchedAllergens(cleaned)

        val fingerprint = "$riskLevel|$cleaned"
        if (fingerprint == lastFrameFingerprint) {
            stableFrameCount++
        } else {
            stableFrameCount = 1
            lastFrameFingerprint = fingerprint
        }

        currentBinding.root.post {
            showTrackingIndicator()

            if (riskLevel != ScanRisk.SAFE) {
                val allergensStr = detectedAllergens.joinToString(", ")
                showDetectedChip(allergensStr)
                renderDynamicHUD(riskLevel = riskLevel, matchedAllergen = allergensStr)
            } else {
                hideDetectedChip()
                renderDynamicHUD(riskLevel = ScanRisk.SAFE, matchedAllergen = null)
            }
        }

        if (stableFrameCount >= MIN_STABLE_FRAMES && !autoTriggered && !scanFrozen) {
            currentBinding.root.post {
                autoTriggerScan(recognizedText)
            }
        }
    }

    private fun isValidLabelText(cleaned: String): Boolean {
        if (cleaned.length < 10) return false
        return labelStructuralKeywords.any { cleaned.contains(it) }
    }

    private fun sanitizeText(text: String): String {
        return text.lowercase(Locale.getDefault())
            .replace(Regex("[,.\\(\\)]"), " ")
            .trim()
    }

    private fun evaluateRiskLevel(cleaned: String): String {
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

    private fun findMatchedAllergens(cleaned: String): List<String> {
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

    private fun showTrackingIndicator() {
        val b = _binding ?: return
        b.scanOverlay.visibility = View.VISIBLE
        b.textTrackingStatus.apply {
            text = getString(R.string.tracking_label_found)
            visibility = View.VISIBLE
        }
    }

    private fun hideTrackingIndicator() {
        val b = _binding ?: return
        b.scanOverlay.visibility = View.GONE
        b.textTrackingStatus.visibility = View.GONE
    }

    private fun resetStableCounter() {
        stableFrameCount = 0
        lastFrameFingerprint = null
    }

    private fun showDetectedChip(allergenText: String) {
        val b = _binding ?: return
        b.chipDetectedIngredient.apply {
            text = "\u26A0\uFE0F $allergenText"
            visibility = View.VISIBLE
        }
    }

    private fun hideDetectedChip() {
        _binding?.chipDetectedIngredient?.visibility = View.GONE
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

    private fun uploadScanToFirestore(detectedText: String, riskLevel: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val matchedAllergens = if (riskLevel != ScanRisk.SAFE) {
            findMatchedAllergens(sanitizeText(detectedText))
        } else {
            emptyList()
        }
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

    private fun renderNeutralHUD(
        title: String = getString(R.string.hud_neutral_title),
        message: String = getString(R.string.hud_neutral_message)
    ) {
        val ctx = requireContext()
        binding.hudStatusCard.setCardBackgroundColor(
            ContextCompat.getColor(ctx, R.color.hud_neutral_bg)
        )
        binding.textHudTitle.setTextColor(ContextCompat.getColor(ctx, R.color.hud_neutral_text))
        binding.textHudMessage.setTextColor(ContextCompat.getColor(ctx, R.color.hud_neutral_text))
        binding.textHudTitle.text = title
        binding.textHudMessage.text = message
    }

    private fun renderDynamicHUD(
        riskLevel: String,
        matchedAllergen: String?,
        title: String? = null,
        message: String? = null
    ) {
        val card = binding.hudStatusCard
        val ctx = requireContext()

        val isDanger = riskLevel == ScanRisk.DANGER
        val isWarning = riskLevel == ScanRisk.WARNING
        val bgRes = when {
            isDanger -> R.color.hud_danger_bg
            isWarning -> R.color.hud_warning_bg
            else -> R.color.hud_safe_bg
        }
        val textRes = when {
            isDanger -> R.color.hud_danger_text
            isWarning -> R.color.hud_warning_text
            else -> R.color.hud_safe_text
        }

        card.setCardBackgroundColor(ContextCompat.getColor(ctx, bgRes))
        binding.textHudTitle.setTextColor(ContextCompat.getColor(ctx, textRes))
        binding.textHudMessage.setTextColor(ContextCompat.getColor(ctx, textRes))

        binding.textHudTitle.text = title ?: when {
            isDanger -> getString(R.string.hud_danger_title)
            isWarning -> getString(R.string.hud_warning_title)
            else -> getString(R.string.hud_safe_title)
        }

        binding.textHudMessage.text = message ?: when {
            isDanger && matchedAllergen != null ->
                getString(R.string.hud_danger_message, matchedAllergen)
            isWarning && matchedAllergen != null ->
                getString(R.string.hud_warning_message, matchedAllergen)
            else -> getString(R.string.hud_safe_message)
        }
    }

    private fun showWarningHUD(title: String, message: String) {
        val ctx = requireContext()
        binding.hudStatusCard.setCardBackgroundColor(
            ContextCompat.getColor(ctx, R.color.hud_warning_bg)
        )
        binding.textHudTitle.setTextColor(ContextCompat.getColor(ctx, R.color.hud_warning_text))
        binding.textHudMessage.setTextColor(ContextCompat.getColor(ctx, R.color.hud_warning_text))
        binding.textHudTitle.text = title
        binding.textHudMessage.text = message
    }

    private data class AllergenKeyword(
        val displayName: String,
        val normalizedName: String
    )

    private inner class LabelTextAnalyzer : ImageAnalysis.Analyzer {

        override fun analyze(imageProxy: ImageProxy) {
            if (scanFrozen) {
                imageProxy.close()
                return
            }

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
