package unab.edu.co.abrahamcaceres.safebite.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentProfileBinding
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ProfileViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ProfileViewModelFactory

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val statusBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).top
            val navBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom
            v.setPadding(0, statusBars, 0, navBars)
            insets
        }

        setupObservers()
        setupLogout()
        viewModel.loadCurrentUser()
    }

    private fun setupObservers() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            binding.textProfileName.text = state.name
            binding.textProfileEmail.text = state.email
            renderAllergenChips(state.allergens)
        }

        viewModel.shouldReturnToLogin.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                viewModel.consumeReturnToLogin()
                findNavController().navigate(R.id.action_global_login)
            }
        }
    }

    private fun renderAllergenChips(allergens: List<String>) {
        binding.chipGroupProfileAllergens.removeAllViews()
        if (allergens.isEmpty()) {
            binding.textAllergensEmpty.visibility = View.VISIBLE
        } else {
            binding.textAllergensEmpty.visibility = View.GONE
            allergens.forEach { allergen ->
                val chip = Chip(requireContext()).apply {
                    text = allergen
                    isClickable = false
                    isFocusable = false
                    setChipBackgroundColorResource(R.color.risk_safe_container)
                    setTextColor(resources.getColor(R.color.risk_safe_on, null))
                }
                binding.chipGroupProfileAllergens.addView(chip)
            }
        }
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
