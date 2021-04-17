package com.zbapp.locationtrack.ui

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.shared.navigateDeeplink
import com.zbapp.locationtrack.ui.shared.setupWithNavController

class MainActivity : AppCompatActivity() {

    private var currentNavController: LiveData<NavController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setupBottomNavigationBar()
        currentNavController?.value?.restoreState(savedInstanceState.getBundle("nav_state"))
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBundle("nav_state", currentNavController?.value?.saveState())
    }

    private fun setupBottomNavigationBar() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNavigationView.selectedItemId = R.id.nav_graph_dashboard

        val navGraphIds = listOf(R.navigation.nav_graph_dashboard, R.navigation.nav_graph_map, R.navigation.nav_graph_settings)

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = intent
        )

        // Whenever the selected controller changes, setup the action bar.
//        controller.observe(this) { setupActionBarWithNavController(it) }
        currentNavController = controller
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    fun navigateDeeplink(deeplink: String) {
        navigateDeeplink(Uri.parse(deeplink))
    }

    fun navigateDeeplink(uri: Uri) {
        val navGraphIds = listOf(R.navigation.nav_graph_dashboard, R.navigation.nav_graph_map, R.navigation.nav_graph_settings)
        findViewById<BottomNavigationView>(R.id.bottomNav).navigateDeeplink(navGraphIds, supportFragmentManager, R.id.nav_host_fragment, uri)
    }

}
