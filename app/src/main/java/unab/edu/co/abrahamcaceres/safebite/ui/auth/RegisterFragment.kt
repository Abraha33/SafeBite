package unab.edu.co.abrahamcaceres.safebite.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentRegisterBinding
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AuthViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AuthViewModelFactory

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        setupToolbar()
        setupObservers()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbarRegister.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupObservers() {
        viewModel.nameError.observe(viewLifecycleOwner) { message ->
            setError(binding.layoutName, message)
        }
        viewModel.emailError.observe(viewLifecycleOwner) { message ->
            setError(binding.layoutEmail, message)
        }
        viewModel.passwordError.observe(viewLifecycleOwner) { message ->
            setError(binding.layoutPassword, message)
        }
        viewModel.registerSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.consumeRegisterSuccess()
                val email = binding.inputEmail.text?.toString().orEmpty().trim()
                Toast.makeText(requireContext(), R.string.register_ok, Toast.LENGTH_SHORT).show()
                findNavController().previousBackStackEntry?.savedStateHandle?.set(
                    LoginFragment.KEY_PREFILL_EMAIL,
                    email
                )
                findNavController().popBackStack()
            }
        }
    }

    private fun setupListeners() {
        binding.inputName.doAfterTextChanged { binding.layoutName.error = null }
        binding.inputEmail.doAfterTextChanged { binding.layoutEmail.error = null }
        binding.inputPassword.doAfterTextChanged { binding.layoutPassword.error = null }

        binding.buttonRegister.setOnClickListener {
            val name = binding.inputName.text?.toString().orEmpty()
            val email = binding.inputEmail.text?.toString().orEmpty()
            val password = binding.inputPassword.text?.toString().orEmpty()
            val allergens = getSelectedAllergens()
            viewModel.validateAndRegister(name, email, password, city = "Bucaramanga", allergens = allergens)
        }
    }

    private fun getSelectedAllergens(): List<String> {
        val checkedIds = binding.chipGroupDiet.checkedChipIds
        val labels = mutableListOf<String>()
        for (id in checkedIds) {
            val chip = binding.root.findViewById<com.google.android.material.chip.Chip>(id)
            chip?.let { labels.add(it.text.toString()) }
        }
        return labels
    }

    private fun setError(layout: TextInputLayout, message: String?) {
        layout.error = message
        layout.isErrorEnabled = message != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
