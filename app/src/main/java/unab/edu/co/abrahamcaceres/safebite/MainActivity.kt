package unab.edu.co.abrahamcaceres.safebite

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import unab.edu.co.abrahamcaceres.safebite.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.bottomNav.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

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
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.profileFragment -> {
                    if (FirebaseAuth.getInstance().currentUser != null) {
                        navController.navigate(R.id.profileFragment)
                    } else {
                        navController.navigate(R.id.loginFragment)
                    }
                    true
                }
                R.id.communityFragment -> { navController.navigate(R.id.communityFragment); true }
                R.id.scannerFragment -> { navController.navigate(R.id.scannerFragment); true }
                R.id.scanHistoryFragment -> { navController.navigate(R.id.scanHistoryFragment); true }
                R.id.allergensFragment -> { navController.navigate(R.id.allergensFragment); true }
                else -> false
            }
        }
    }

    private fun observeNavChanges() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility = when (destination.id) {
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.scanDetailFragment -> View.GONE
                else -> View.VISIBLE
            }
            binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
        }
    }
}
