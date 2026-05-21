package unab.edu.co.abrahamcaceres.safebite.ui.blacklist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentBlacklistBinding
import unab.edu.co.abrahamcaceres.safebite.model.cloud.ProductModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.BlacklistViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.BlacklistViewModelFactory

class BlacklistFragment : Fragment() {

    private var _binding: FragmentBlacklistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BlacklistViewModel by viewModels {
        BlacklistViewModelFactory(requireActivity().application)
    }

    private val adapter = BlacklistAdapter { product ->
        showRemoveConfirmation(product)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlacklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        binding.toolbarBlacklist.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.rvBlacklist.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBlacklist.adapter = adapter

        viewModel.blacklist.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { empty ->
            binding.layoutEmptyBlacklist.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvBlacklist.visibility = if (empty) View.GONE else View.VISIBLE
        }

        viewModel.observeBlacklist()
    }

    private fun showRemoveConfirmation(product: ProductModel) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.blacklist_remove_cd)
            .setMessage("${product.productName}")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.removeProduct(product.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
