package com.zbapp.locationtrack.ui

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zbapp.locationtrack.R

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupBottomNavigationBar()
    }

    private fun setupBottomNavigationBar() {

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.nav_host_fragment
        ) as NavHostFragment
        navController = navHostFragment.navController

        // Setup the bottom navigation view with navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNavigationView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    fun navigateDeeplink(deeplink: String) {
        navigateDeeplink(Uri.parse(deeplink))
    }

    fun navigateDeeplink(uri: Uri) {
        navController.navigate(
            uri,
            navOptions {
                restoreState = true
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
        )
    }

}
