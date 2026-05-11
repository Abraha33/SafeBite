package unab.edu.co.abrahamcaceres.safebite.ui.auth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.navigation.fragment.findNavController
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

/**
 * Pantalla de escaneo en tiempo real.
 *
 * Responsabilidades de presentaciÃ³n:
 * - Solicitar permisos de cÃ¡mara de forma segura.
 * - Vincular CameraX (Preview + ImageAnalysis) al ciclo de vida del Fragment.
 * - Observar los alÃ©rgenos activos desde Room mediante [AllergenViewModel].
 * - Actualizar el HUD segÃºn el resultado del OCR de ML Kit.
 * - Persistir los resultados relevantes en Room a travÃ©s de [ScanHistoryViewModel].
 */
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
                renderBanner(
                    message = getString(R.string.scanner_status_permission_denied),
                    bannerStyle = BannerStyle.WARNING
                )
            }
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
        renderBanner(
            message = getString(R.string.scanner_status_waiting),
            bannerStyle = BannerStyle.SAFE
        )

        binding.buttonOpenHistory.setOnClickListener {
            findNavController().navigate(R.id.action_scanner_to_history)
        }
        binding.buttonManageAllergens.setOnClickListener {
            findNavController().navigate(R.id.action_scanner_to_allergens)
        }

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

        renderBanner(
            message = getString(R.string.scanner_status_permission_required),
            bannerStyle = BannerStyle.WARNING
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
            .also { analysis ->
                analysis.setAnalyzer(
                    cameraExecutor,
                    LabelTextAnalyzer(
                        onTextDetected = ::handleRecognizedText,
                        onAnalysisError = ::handleAnalysisError
                    )
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

    private fun handleRecognizedText(recognizedText: String) {
        val currentBinding = _binding ?: return
        val normalizedText = recognizedText.trim().lowercase(Locale.getDefault())

        if (normalizedText.isBlank()) {
            currentBinding.root.post {
                renderBanner(
                    message = getString(R.string.scanner_status_waiting),
                    bannerStyle = BannerStyle.SAFE
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
                renderBanner(
                    message = getString(
                        R.string.scanner_status_danger,
                        detectedAllergens.joinToString(", ")
                    ),
                    bannerStyle = BannerStyle.DANGER
                )
                persistScanIfNeeded(
                    detectedText = recognizedText,
                    riskLevel = ScanRisk.DANGER
                )
            } else {
                renderBanner(
                    message = getString(R.string.scanner_status_safe),
                    bannerStyle = BannerStyle.SAFE
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
            renderBanner(
                message = getString(R.string.scanner_status_error),
                bannerStyle = BannerStyle.WARNING
            )
        }
    }

    /**
     * Evita llenar el historial con cientos de filas idÃ©nticas cuando la cÃ¡mara mantiene
     * el mismo encuadre durante varios frames consecutivos.
     */
    private fun persistScanIfNeeded(detectedText: String, riskLevel: String) {
        val normalizedText = detectedText.trim().lowercase(Locale.getDefault())
        if (normalizedText.isBlank()) return

        val fingerprint = "$riskLevel|$normalizedText"
        if (fingerprint == lastPersistedFingerprint) return

        lastPersistedFingerprint = fingerprint
        scanHistoryViewModel.insertScan(
            ProductScan.crear(
                textoDetectado = detectedText,
                nivelRiesgo = riskLevel
            )
        )
    }

    private fun renderBanner(message: String, bannerStyle: BannerStyle) {
        val currentBinding = _binding ?: return
        currentBinding.textStatusBanner.text = message

        val backgroundColor = ContextCompat.getColor(
            requireContext(),
            when (bannerStyle) {
                BannerStyle.DANGER -> R.color.risk_danger_container
                BannerStyle.WARNING -> R.color.risk_warning_container
                BannerStyle.SAFE -> R.color.risk_safe_container
            }
        )
        val textColor = ContextCompat.getColor(
            requireContext(),
            when (bannerStyle) {
                BannerStyle.DANGER -> R.color.risk_danger_on
                BannerStyle.WARNING -> R.color.risk_warning_on
                BannerStyle.SAFE -> R.color.risk_safe_on
            }
        )

        currentBinding.statusBanner.setCardBackgroundColor(backgroundColor)
        currentBinding.textStatusBanner.setTextColor(textColor)
    }

    private data class AllergenKeyword(
        val displayName: String,
        val normalizedName: String
    )

    private enum class BannerStyle {
        SAFE,
        WARNING,
        DANGER
    }

    /**
     * Adaptador entre CameraX y ML Kit.
     *
     * Se limita a extraer el texto completo del frame y delegar la decisiÃ³n de negocio
     * al [ScannerFragment], para mantener la UI desacoplada del OCR.
     */
    private inner class LabelTextAnalyzer(
        private val onTextDetected: (String) -> Unit,
        private val onAnalysisError: (Exception) -> Unit
    ) : ImageAnalysis.Analyzer {

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

            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    onTextDetected(visionText.text)
                }
                .addOnFailureListener { throwable ->
                    onAnalysisError(Exception(throwable))
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}
