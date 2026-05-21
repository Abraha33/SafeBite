package unab.edu.co.abrahamcaceres.safebite.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentLoginBinding
import unab.edu.co.abrahamcaceres.safebite.utils.InputValidators
import unab.edu.co.abrahamcaceres.safebite.utils.SessionManager
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AuthViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AuthViewModelFactory

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(requireActivity().application)
    }

    private lateinit var sessionManager: SessionManager

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

        sessionManager = SessionManager(requireContext())

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        observePrefillEmail()
        loadRememberedCredentials()
        setupObservers()
        setupListeners()
    }

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

    private fun loadRememberedCredentials() {
        val email = sessionManager.getRememberedEmail()
        val password = sessionManager.getRememberedPassword()
        if (email != null) {
            binding.inputEmail.setText(email)
            binding.checkboxRememberMe.isChecked = true
        }
        if (password != null) {
            binding.inputPassword.setText(password)
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
                if (binding.checkboxRememberMe.isChecked) {
                    val email = binding.inputEmail.text?.toString().orEmpty()
                    val password = binding.inputPassword.text?.toString().orEmpty()
                    sessionManager.saveRememberedCredentials(email, password)
                } else {
                    sessionManager.clearRememberedCredentials()
                }
                Toast.makeText(requireContext(), R.string.login_ok, Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_login_to_scanner)
            }
        }
    }

    private fun setupListeners() {
        binding.inputEmail.doAfterTextChanged { binding.layoutEmail.error = null }
        binding.inputPassword.doAfterTextChanged { binding.layoutPassword.error = null }

        binding.btnLogin.setOnClickListener {
            hideKeyboard()
            val email = binding.inputEmail.text?.toString().orEmpty()
            val password = binding.inputPassword.text?.toString().orEmpty()

            if (!validateFields(email, password)) return@setOnClickListener
            viewModel.validateAndLogin(email, password)
        }

        binding.btnGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun validateFields(email: String, password: String): Boolean {
        var valid = true

        if (!InputValidators.isNotBlank(email) || !InputValidators.isValidEmail(email)) {
            binding.layoutEmail.error = getString(R.string.error_email_invalid)
            valid = false
        }
        if (!InputValidators.isNotBlank(password) || !InputValidators.isValidPassword(password)) {
            binding.layoutPassword.error = getString(R.string.error_password_short)
            valid = false
        }
        return valid
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.btnLogin.windowToken, 0)
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
        const val KEY_PREFILL_EMAIL = "prefill_email"
    }
}
