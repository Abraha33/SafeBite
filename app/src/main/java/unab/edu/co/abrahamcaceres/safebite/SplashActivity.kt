package unab.edu.co.abrahamcaceres.safebite

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla de arranque con el branding de SafeBite antes de entrar al flujo principal ([MainActivity]).
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Breve pausa para que el splash se perciba (evita parpadeo en dispositivos muy rápidos).
        lifecycleScope.launch {
            delay(SPLASH_MIN_MS)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }

    companion object {
        private const val SPLASH_MIN_MS = 1_200L
    }
}
