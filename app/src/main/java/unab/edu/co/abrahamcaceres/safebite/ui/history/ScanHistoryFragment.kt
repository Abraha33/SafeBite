package unab.edu.co.abrahamcaceres.safebite.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentScanHistoryBinding
import unab.edu.co.abrahamcaceres.safebite.model.ProductScan
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ScanHistoryViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ScanHistoryViewModelFactory

/**
 * Historial de productos analizados: RecyclerView + Room + MVVM.
 * Desde aquÃ­ se navega al detalle usando Safe Args.
 */
class ScanHistoryFragment : Fragment() {

    private var _binding: FragmentScanHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanHistoryViewModel by viewModels {
        ScanHistoryViewModelFactory(requireActivity().application)
    }

    private val adapter = ScanHistoryAdapter { scan ->
        onScanItemClick(scan)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarHistory.setupWithNavController(findNavController())

        binding.rvScanHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvScanHistory.adapter = adapter

        binding.buttonAddSampleScan.setOnClickListener {
            viewModel.insertSampleScan()
        }

        viewModel.scans.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            val empty = list.isNullOrEmpty()
            binding.textEmptyHistory.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvScanHistory.visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    private fun onScanItemClick(scan: ProductScan) {
        val action = ScanHistoryFragmentDirections.actionScanHistoryToScanDetail(scan)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
