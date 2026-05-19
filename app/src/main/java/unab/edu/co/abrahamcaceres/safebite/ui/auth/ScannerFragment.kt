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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentScannerBinding
import unab.edu.co.abrahamcaceres.safebite.model.ProductScan
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk
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

    @Volatile
    private var allergenKeywords: List<AllergenKeyword> = emptyList()

    private var lastPersistedFingerprint: String? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!isAdded) return@registerForActivityResult
            if (granted) {
                startCameraFlow()
            } else {
                renderDynamicHUD(
                    icon = "\uD83D\uDEAB",
                    title = getString(R.string.scanner_status_permission_denied),
                    message = getString(R.string.scanner_status_permission_body),
                    style = HudStyle.WARNING
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
        renderDynamicHUD(
            icon = "\uD83D\uDEE1\uFE0F",
            title = getString(R.string.hud_waiting_title),
            message = getString(R.string.hud_waiting_message),
            style = HudStyle.SAFE
        )
        ensureCameraPermissionAndStart()
    }

    override fun onDestroyView() {
        imageAnalysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        imageAnalysis = null
        cameraProvider = null
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
        renderDynamicHUD(
            icon = "\uD83D\uDCF7",
            title = getString(R.string.scanner_status_permission_required),
            message = getString(R.string.scanner_status_permission_body),
            style = HudStyle.WARNING
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
        provider.bindToLifecycle(
            viewLifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis
        )
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

    private fun processImage(image: InputImage, onComplete: (() -> Unit)? = null) {
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                handleRecognizedText(visionText.text)
            }
            .addOnFailureListener { throwable ->
                handleAnalysisError(Exception(throwable))
            }
            .addOnCompleteListener {
                onComplete?.invoke()
            }
    }

    private fun handleRecognizedText(recognizedText: String) {
        val currentBinding = _binding ?: return
        val normalizedText = recognizedText.trim().lowercase(Locale.getDefault())

        if (normalizedText.isBlank()) {
            currentBinding.root.post {
                renderDynamicHUD(
                    icon = "\uD83D\uDEE1\uFE0F",
                    title = getString(R.string.hud_waiting_title),
                    message = getString(R.string.hud_waiting_message),
                    style = HudStyle.SAFE
                )
            }
            return
        }

        val detectedAllergens = allergenKeywords
            .filter { normalizedText.contains(it.normalizedName) }
            .map { it.displayName }
            .distinct()

        currentBinding.root.post {
            if (detectedAllergens.isNotEmpty()) {
                val allergensStr = detectedAllergens.joinToString(", ")
                renderDynamicHUD(
                    icon = "\uD83D\uDEA8",
                    title = getString(R.string.hud_danger_title),
                    message = getString(R.string.hud_danger_message, allergensStr),
                    style = HudStyle.DANGER
                )
                persistScanIfNeeded(
                    detectedText = recognizedText,
                    riskLevel = ScanRisk.DANGER
                )
            } else {
                renderDynamicHUD(
                    icon = "\uD83D\uDEE1\uFE0F",
                    title = getString(R.string.hud_safe_title),
                    message = getString(R.string.hud_safe_message),
                    style = HudStyle.SAFE
                )
                persistScanIfNeeded(
                    detectedText = recognizedText,
                    riskLevel = ScanRisk.SAFE
                )
            }
        }
    }

    private fun handleAnalysisError(error: Exception) {
        val currentBinding = _binding ?: return
        error.printStackTrace()
        currentBinding.root.post {
            renderDynamicHUD(
                icon = "\u26A0\uFE0F",
                title = getString(R.string.scanner_status_error),
                message = getString(R.string.hud_error_message),
                style = HudStyle.WARNING
            )
        }
    }

    private fun persistScanIfNeeded(detectedText: String, riskLevel: String) {
        val normalizedText = detectedText.trim().lowercase(Locale.getDefault())
        if (normalizedText.isBlank()) return

        val fingerprint = "$riskLevel|$normalizedText"
        if (fingerprint == lastPersistedFingerprint) return

        lastPersistedFingerprint = fingerprint
        if (riskLevel == ScanRisk.DANGER) {
            vibrateDangerFeedback()
        }

        scanHistoryViewModel.insertScan(
            ProductScan.crear(
                textoDetectado = detectedText,
                nivelRiesgo = riskLevel
            )
        )
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

    private fun renderDynamicHUD(
        icon: String,
        title: String,
        message: String,
        style: HudStyle
    ) {
        val card = binding.hudStatusCard
        val ctx = requireContext()

        val (bgColor, textColor) = when (style) {
            HudStyle.DANGER -> R.color.risk_danger_container to R.color.risk_danger_on
            HudStyle.WARNING -> R.color.risk_warning_container to R.color.risk_warning_on
            HudStyle.SAFE -> R.color.risk_safe_container to R.color.risk_safe_on
        }

        val newBg = ContextCompat.getColor(ctx, bgColor)
        val newText = ContextCompat.getColor(ctx, textColor)

        val currentBg = (card.cardBackgroundColor.defaultColor ?: newBg).let {
            card.cardBackgroundColor.defaultColor
        }

        ValueAnimator.ofObject(ArgbEvaluator(), currentBg, newBg).apply {
            duration = 350
            addUpdateListener { animator ->
                card.setCardBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }

        binding.textHudIcon.text = icon
        binding.textHudIcon.setTextColor(newText)
        binding.textHudTitle.text = title
        binding.textHudTitle.setTextColor(newText)
        binding.textHudMessage.text = message
        binding.textHudMessage.setTextColor(newText)
    }

    private data class AllergenKeyword(
        val displayName: String,
        val normalizedName: String
    )

    private enum class HudStyle {
        SAFE,
        WARNING,
        DANGER
    }

    private inner class LabelTextAnalyzer : ImageAnalysis.Analyzer {

        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return
            }

            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            processImage(image) {
                imageProxy.close()
            }
        }
    }
}
