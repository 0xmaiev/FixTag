package com.example.fixtag.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.ScrollingActivity
import com.example.fixtag.ui.login.LoginActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Instantiate ViewModel with the custom factory
    private val viewModel: MainViewModel by viewModels {
        val app = applicationContext as FixTag
        MainViewModelFactory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
                    is MainViewModel.NavigationState.Stay -> {
                        Log.d("MainActivity", "Changing view to display incidents")
                        // Inflate your actual layout only if the user stays
                        setContentView(R.layout.activity_main)
                    }
                }
            }
        }

    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            // Prevent users from clicking "Back" and coming back to an empty MainActivity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Closes the current MainActivity instance
    }

    private fun navigateToIncidents() {
        val intent = Intent(this, ScrollingActivity::class.java).apply {
            // Prevent users from clicking "Back" and coming back to an empty MainActivity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Closes the current MainActivity instance
    }

    private fun navigateToNewIncident() {

    }

    private fun navigateToIncidentDetails() {

    }
}