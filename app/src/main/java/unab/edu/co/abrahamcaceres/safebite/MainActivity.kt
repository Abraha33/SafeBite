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
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.nav_enter)
            .setExitAnim(R.anim.nav_exit)
            .setPopEnterAnim(R.anim.fade_in)
            .setPopExitAnim(R.anim.fade_out)
            .setLaunchSingleTop(true)
            .build()

        binding.bottomNav.setOnItemSelectedListener { item ->
            if (FirebaseAuth.getInstance().currentUser != null) {
                navController.navigate(item.itemId, null, navOptions)
            } else {
                navController.navigate(R.id.action_global_login, null, navOptions)
            }
            true
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
