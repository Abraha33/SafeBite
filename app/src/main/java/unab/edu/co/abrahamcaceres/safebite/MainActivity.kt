package unab.edu.co.abrahamcaceres.safebite

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import unab.edu.co.abrahamcaceres.safebite.databinding.ActivityMainBinding

/**
 * Actividad única que hospeda el [NavHostFragment] con BottomNavigationView.
 * La BottomNav permite navegar entre Scanner, Historial y Alérgenos.
 * Se oculta automáticamente en pantallas de autenticación y detalle.
 * Soporta arranque dinámico: si SplashActivity envía un START_DESTINATION,
 * el NavController se configura para comenzar allí (usuario autenticado).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val startDest = intent.getIntExtra(
            SplashActivity.START_DESTINATION_EXTRA, -1
        )
        if (startDest != -1) {
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            navGraph.setStartDestination(startDest)
            navController.graph = navGraph
        }

        setupBottomNav()
        observeNavChanges()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setupWithNavController(navController)
    }

    private fun observeNavChanges() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility = when (destination.id) {
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.scanDetailFragment -> View.GONE

                else -> View.VISIBLE
            }
        }
    }
}
