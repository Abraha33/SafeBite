package unab.edu.co.abrahamcaceres.safebite.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.BottomSheetProfileEditBinding
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentProfileBinding
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ProfileViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ProfileViewModelFactory

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(requireActivity().application)
    }

    private var editDialog: BottomSheetDialog? = null
    private var editSheetBinding: BottomSheetProfileEditBinding? = null

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
        setupButtons()
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

        viewModel.isSaving.observe(viewLifecycleOwner) { saving ->
            editSheetBinding?.btnSaveProfile?.isEnabled = !saving
            editSheetBinding?.btnSaveProfile?.text = if (saving) {
                getString(R.string.profile_saving)
            } else {
                getString(R.string.profile_save_button)
            }
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            when (success) {
                true -> {
                    editDialog?.dismiss()
                    Toast.makeText(requireContext(), R.string.profile_save_success, Toast.LENGTH_SHORT).show()
                    viewModel.consumeSaveSuccess()
                }
                false -> {
                    Toast.makeText(requireContext(), R.string.profile_save_error, Toast.LENGTH_SHORT).show()
                    viewModel.consumeSaveSuccess()
                }
                null -> {}
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

    private fun setupButtons() {
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        binding.btnEditProfile.setOnClickListener {
            showEditBottomSheet()
        }

        binding.btnBlacklist.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_blacklist)
        }
    }

    private fun showEditBottomSheet() {
        val currentState = viewModel.profileState.value ?: return

        editDialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetProfileEditBinding.inflate(layoutInflater)
        editSheetBinding = sheetBinding
        editDialog?.setContentView(sheetBinding.root)

        sheetBinding.inputEditName.setText(currentState.name)

        val predefinedAllergens = listOf("Sin Gluten", "Sin Lactosa", "Vegano", "Sin Mani", "Sin Azucar")
        sheetBinding.chipGroupEditAllergens.removeAllViews()
        predefinedAllergens.forEach { allergen ->
            val chip = Chip(requireContext()).apply {
                text = allergen
                isCheckable = true
                isChecked = currentState.allergens.contains(allergen)
            }
            sheetBinding.chipGroupEditAllergens.addView(chip)
        }

        sheetBinding.btnSaveProfile.setOnClickListener {
            val newName = sheetBinding.inputEditName.text?.toString().orEmpty()
            val checkedIds = sheetBinding.chipGroupEditAllergens.checkedChipIds
            val selectedAllergens = mutableListOf<String>()
            for (id in checkedIds) {
                val chip = sheetBinding.chipGroupEditAllergens.findViewById<Chip>(id)
                chip?.let { selectedAllergens.add(it.text.toString()) }
            }
            viewModel.saveProfileChanges(newName, selectedAllergens)
        }

        sheetBinding.btnCancelEdit.setOnClickListener { editDialog?.dismiss() }

        sheetBinding.btnSaveProfile.isEnabled = !(viewModel.isSaving.value ?: false)
        sheetBinding.btnSaveProfile.text = getString(R.string.profile_save_button)

        editDialog?.setOnDismissListener {
            editSheetBinding = null
            editDialog = null
        }

        editDialog?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
