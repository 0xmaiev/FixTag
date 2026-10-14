package com.example.fixtag.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.fragment
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.ui.fragments.IncidentDetailFragment
import com.example.fixtag.ui.fragments.IncidentsFragment
import com.example.fixtag.ui.fragments.MachineDetailFragment
import com.example.fixtag.ui.fragments.MachinesFragment
import com.example.fixtag.ui.login.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class MainActivity : AppCompatActivity() {

    // Instantiate ViewModel with the custom factory
    private val viewModel: MainViewModel by viewModels {
        val app = applicationContext as FixTag
        MainViewModelFactory(app.repository, app.syncScheduler)
    }

    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHostFragment.navController
        bottomNavigationView = findViewById(R.id.bottom_nav)

        // Interaction must happen on a background thread (e.g., using Coroutines)
        lifecycleScope.launch {
            viewModel.navigationState.collect { state ->
                when (state) {
                    is MainViewModel.NavigationState.Loading -> {
                        // Optional: Show a progress bar or keep splash screen drawn
                    }
                    is MainViewModel.NavigationState.GoToLogin -> {
                        Log.d("MainActivity", "Navigating to login activity")
                        navigateToLogin()
                    }
                    is MainViewModel.NavigationState.GoToMachines -> {
                        Log.d("MainActivity", "Changing view to display machines")
                        prepareNavigationGraph(R.id.machines)
                    }
                    is MainViewModel.NavigationState.GoToIncidents -> {
                        Log.d("MainActivity", "Changing view to display incidents")
                        prepareNavigationGraph(R.id.incidents)
                    }
                }
            }
        }

    }

    private fun prepareNavigationGraph(startDestination: Int) {
        // Inflate layout
        navController.graph = navController.createGraph(
            id = R.id.nav_host,
            startDestination = startDestination
        ) {
            fragment<IncidentsFragment>(R.id.incidents) {
                label = resources.getString(R.string.incidents_fragment_title)
            }
            fragment<MachinesFragment>(R.id.machines)  {
                label = resources.getString(R.string.machines_fragment_title)
            }
            fragment<MachineDetailFragment>(R.id.machine_details)  {
                label = resources.getString(R.string.machine_details_fragment_title)
            }
            fragment<IncidentDetailFragment>(R.id.incident_details)  {
                label = resources.getString(R.string.incident_details_fragment_title)
            }
        }
        bottomNavigationView.setupWithNavController(navController)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            // Prevent users from clicking "Back" and coming back to an empty MainActivity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Closes the current MainActivity instance
    }

    private fun navigateToIncidentDetails(incidentId: Uuid) {
        //navController.navigate(route = IncidentDetail(id = incidentId))
    }
}