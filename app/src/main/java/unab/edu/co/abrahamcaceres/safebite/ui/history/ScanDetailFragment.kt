package unab.edu.co.abrahamcaceres.safebite.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentScanDetailBinding
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ScanHistoryViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ScanHistoryViewModelFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ScanDetailFragment : Fragment() {

    private var _binding: FragmentScanDetailBinding? = null
    private val binding get() = _binding!!

    private val args: ScanDetailFragmentArgs by navArgs()

    private val viewModel: ScanHistoryViewModel by viewModels {
        ScanHistoryViewModelFactory(requireActivity().application)
    }

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

        binding.toolbarDetail.setupWithNavController(findNavController())

        val scanId = args.scanId

        viewModel.scans.observe(viewLifecycleOwner) { list ->
            val scan = list.find { it.getId() == scanId }
            scan?.let { item ->
                bindVerdict(item.getRiskLevel())
                binding.textScanDate.text = dateFormatter.format(
                    Instant.ofEpochMilli(item.getCreatedAtMs())
                )
                binding.textRawOcr.text = item.getDetectedText()
            }
        }
    }

    private fun bindVerdict(riskLevel: String) {
        val ctx = requireContext()
        val (bgRes, textColorRes, title, description) = when (riskLevel) {
            ScanRisk.DANGER -> arrayOf(
                R.color.risk_danger_container,
                R.color.risk_danger_on,
                getString(R.string.verdict_danger_title),
                getString(R.string.verdict_danger_body)
            )
            ScanRisk.WARNING -> arrayOf(
                R.color.risk_warning_container,
                R.color.risk_warning_on,
                getString(R.string.verdict_warning_title),
                getString(R.string.verdict_warning_body)
            )
            else -> arrayOf(
                R.color.risk_safe_container,
                R.color.risk_safe_on,
                getString(R.string.verdict_safe_title),
                getString(R.string.verdict_safe_body)
            )
        }

        binding.cardVerdictContainer.setCardBackgroundColor(
            ContextCompat.getColor(ctx, bgRes as Int)
        )
        binding.textVerdictTitle.text = title as String
        binding.textVerdictTitle.setTextColor(ContextCompat.getColor(ctx, textColorRes as Int))
        binding.textVerdictDescription.text = description as String
        binding.textVerdictDescription.setTextColor(ContextCompat.getColor(ctx, textColorRes))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
