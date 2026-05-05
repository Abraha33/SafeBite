package unab.edu.co.abrahamcaceres.safebite

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Actividad única que hospeda el [NavHostFragment] (Navigation Component).
 * El flujo Login → Registro → (tras login) Escáner se define en res/navigation/nav_graph.xml.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
