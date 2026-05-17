package unab.edu.co.abrahamcaceres.safebite

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.data.repository.UserRepository
import unab.edu.co.abrahamcaceres.safebite.utils.SessionManager

/**
 * Pantalla de arranque con el branding de SafeBite.
 * Consulta Room para decidir si redirigir al Scanner (usuario ya registrado)
 * o al Login (primera ejecución).
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(applicationContext)
        val db = (application as SafeBiteApplication).database
        val userRepository = UserRepository(db.userDao())

        lifecycleScope.launch {
            delay(SPLASH_MIN_MS)

            val sessionEmail = sessionManager.getCurrentUserEmail()
            val userCount = userRepository.getLoggedUserCount()

            val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                if (sessionEmail != null && userCount > 0) {
                    putExtra(START_DESTINATION_EXTRA, R.id.scannerFragment)
                }
            }
            startActivity(intent)
            finish()
        }
    }

    companion object {
        private const val SPLASH_MIN_MS = 1_200L
        const val START_DESTINATION_EXTRA = "START_DESTINATION"
    }
}
