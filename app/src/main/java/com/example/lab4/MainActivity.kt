package com.example.lab4

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    // private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure RetrofitClient is initialized to prevent crashes if restored directly
        RetrofitClient.init(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: MainActivity created.")

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Dynamically set start destination based on Intent
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        if (intent.getStringExtra("DESTINATION") == "HOME") {
            navGraph.setStartDestination(R.id.homeFragment)
        }
        navController.graph = navGraph

        // ActionBar setup removed for NoActionBar theme
        /*
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.profileFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        */

        // Helper to setup navigation with custom behavior for Home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    // Navigate to home logic
                    // Pop everything up to home (exclusive) so we return to the home fragment instance
                    navController.popBackStack(R.id.homeFragment, false)
                    true
                }
                else -> {
                    // Let the default NavigationUI handle other items
                    androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
                }
            }
        }
        
        // Reselect listener to refresh (optional, but good UX)
        binding.bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.homeFragment) {
                // If already on home, maybe scroll to today? 
                // For now, simple re-navigate effectively resets
            }
        }

        // Hide bottom navigation for login and register screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment, R.id.resetPasswordFragment -> {
                    binding.bottomNav.visibility = View.GONE
                }
                else -> {
                    binding.bottomNav.visibility = View.VISIBLE
                }
            }
        }
    }

    /*
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    */

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: MainActivity started.")
    }
}
