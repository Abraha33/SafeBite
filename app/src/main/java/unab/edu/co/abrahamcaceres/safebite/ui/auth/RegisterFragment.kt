package unab.edu.co.abrahamcaceres.safebite.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentRegisterBinding
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AuthViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AuthViewModelFactory

/**
 * Registro con validación en ViewModel y persistencia Room; vuelve al login vía Navigation.
 */
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
        setupObservers()
        setupListeners()
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
                // Pasa el correo al fragmento de login usando el back stack entry anterior.
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
            viewModel.validateAndRegister(name, email, password)
        }

        binding.buttonGoLogin.setOnClickListener {
            findNavController().popBackStack()
        }
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
