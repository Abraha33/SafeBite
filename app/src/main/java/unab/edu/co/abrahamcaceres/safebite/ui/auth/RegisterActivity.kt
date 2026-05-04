package unab.edu.co.abrahamcaceres.safebite.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.ActivityRegisterBinding
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AuthViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AuthViewModelFactory

/**
 * Pantalla de registro (nombre, email, contraseña) con validación y persistencia en Room.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.nameError.observe(this) { message ->
            setError(binding.layoutName, message)
        }
        viewModel.emailError.observe(this) { message ->
            setError(binding.layoutEmail, message)
        }
        viewModel.passwordError.observe(this) { message ->
            setError(binding.layoutPassword, message)
        }
        viewModel.registerSuccess.observe(this) { success ->
            if (success) {
                viewModel.consumeRegisterSuccess()
                val email = binding.inputEmail.text?.toString().orEmpty().trim()
                Toast.makeText(this, R.string.register_ok, Toast.LENGTH_SHORT).show()
                // Navegación por Intent hacia Login tras registro exitoso (requisito del curso).
                startActivity(
                    Intent(this, LoginActivity::class.java).apply {
                        putExtra(LoginActivity.EXTRA_EMAIL, email)
                    }
                )
                finish()
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
            finish()
        }
    }

    private fun setError(layout: TextInputLayout, message: String?) {
        layout.error = message
        layout.isErrorEnabled = message != null
    }
}
