package unab.edu.co.abrahamcaceres.safebite.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import unab.edu.co.abrahamcaceres.safebite.MainActivity
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.ActivityLoginBinding
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AuthViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AuthViewModelFactory

/**
 * Pantalla de inicio de sesión (email + contraseña) con validación y consulta a Room.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Si venimos del registro, precargamos el correo recibido por Intent.
        intent.getStringExtra(EXTRA_EMAIL)?.let { email ->
            binding.inputEmail.setText(email)
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.emailError.observe(this) { message ->
            setError(binding.layoutEmail, message)
        }
        viewModel.passwordError.observe(this) { message ->
            setError(binding.layoutPassword, message)
        }
        viewModel.loginSuccess.observe(this) { success ->
            if (success) {
                viewModel.consumeLoginSuccess()
                Toast.makeText(this, R.string.login_ok, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
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
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setError(layout: TextInputLayout, message: String?) {
        layout.error = message
        layout.isErrorEnabled = message != null
    }

    companion object {
        /** Extra opcional para rellenar el correo tras un registro exitoso. */
        const val EXTRA_EMAIL = "extra_email"
    }
}
