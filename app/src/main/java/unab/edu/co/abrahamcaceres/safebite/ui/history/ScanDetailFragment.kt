package unab.edu.co.abrahamcaceres.safebite.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentScanDetailBinding
import unab.edu.co.abrahamcaceres.safebite.model.ProductScan
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Detalle de un escaneo persistido en Room.
 * Consume el objeto completo mediante Safe Args para mantener navegaciÃ³n type-safe.
 */
class ScanDetailFragment : Fragment() {

    private var _binding: FragmentScanDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ScanDetailFragmentArgs by navArgs()

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-ES"))
            .withZone(ZoneId.systemDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbarScanDetail.setupWithNavController(findNavController())
        bindScan(args.scan)
    }

    private fun bindScan(scan: ProductScan) {
        binding.textProductName.text = scan.getProductName()
        binding.textProductDate.text = dateFormatter.format(Instant.ofEpochMilli(scan.getCreatedAtMs()))
        binding.textRiskValue.text = resolveRiskText(scan.getRiskLevel())
        binding.textIngredientsValue.text = buildIngredientsList(scan.getDetectedText())
        bindRiskIcon(scan.getRiskLevel())
    }

    private fun bindRiskIcon(riskLevel: String) {
        val iconRes = when (riskLevel) {
            ScanRisk.DANGER -> android.R.drawable.presence_busy
            ScanRisk.WARNING -> android.R.drawable.presence_away
            else -> android.R.drawable.presence_online
        }

        val tintRes = when (riskLevel) {
            ScanRisk.DANGER -> R.color.risk_danger_on
            ScanRisk.WARNING -> R.color.risk_warning_on
            else -> R.color.risk_safe_on
        }

        binding.imageRiskState.setImageResource(iconRes)
        binding.imageRiskState.setColorFilter(ContextCompat.getColor(requireContext(), tintRes))
    }

    private fun resolveRiskText(riskLevel: String): String {
        return when (riskLevel) {
            ScanRisk.DANGER -> getString(R.string.risk_danger)
            ScanRisk.WARNING -> getString(R.string.risk_warning)
            else -> getString(R.string.risk_safe)
        }
    }

    private fun buildIngredientsList(detectedText: String): String {
        val tokens = detectedText
            .replace("\r", "")
            .split("\n", ",")
            .map { token ->
                token
                    .trim()
                    .removePrefix("Ingredientes:")
                    .removePrefix("ingredientes:")
                    .trim()
            }
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return getString(R.string.history_detail_empty_ingredients)

        return tokens.joinToString(separator = "\n") { ingredient -> "\u2022 $ingredient" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
