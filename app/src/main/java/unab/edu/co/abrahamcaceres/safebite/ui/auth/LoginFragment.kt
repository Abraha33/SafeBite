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
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentLoginBinding
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AuthViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AuthViewModelFactory

/**
 * Pantalla de inicio de sesión dentro del NavHost (Navigation Component + MVVM).
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observePrefillEmail()
        setupObservers()
        setupListeners()
    }

    /**
     * Si el registro guardó un correo en el SavedStateHandle del destino anterior,
     * lo aplicamos al campo (comunicación entre fragmentos sin Intent).
     */
    private fun observePrefillEmail() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<String>(KEY_PREFILL_EMAIL)
            ?.observe(viewLifecycleOwner) { email ->
                if (!email.isNullOrBlank()) {
                    binding.inputEmail.setText(email)
                    findNavController().currentBackStackEntry?.savedStateHandle
                        ?.remove<String>(KEY_PREFILL_EMAIL)
                }
            }
    }

    private fun setupObservers() {
        viewModel.emailError.observe(viewLifecycleOwner) { message ->
            setError(binding.layoutEmail, message)
        }
        viewModel.passwordError.observe(viewLifecycleOwner) { message ->
            setError(binding.layoutPassword, message)
        }
        viewModel.loginSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.consumeLoginSuccess()
                Toast.makeText(requireContext(), R.string.login_ok, Toast.LENGTH_SHORT).show()
                // Navigation Component: acción con popUpTo definido en nav_graph.xml
                findNavController().navigate(R.id.action_login_to_scanner)
            }
        }
    }

    private fun setupListeners() {
        binding.inputEmail.doAfterTextChanged { binding.layoutEmail.error = null }
        binding.inputPassword.doAfterTextChanged { binding.layoutPassword.error = null }

        binding.buttonLogin.setOnClickListener {
            val email = binding.inputEmail.text?.toString().orEmpty()
            val password = binding.inputPassword.text?.toString().orEmpty()
            viewModel.validateAndLogin(email, password)
        }

        binding.buttonGoRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
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

    companion object {
        /** Clave en SavedStateHandle para precargar el correo tras un registro exitoso. */
        const val KEY_PREFILL_EMAIL = "prefill_email"
    }
}
