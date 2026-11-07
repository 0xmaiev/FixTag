package com.example.fixtag.ui.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.fixtag.BuildConfig
import com.example.fixtag.FixTag
import com.example.fixtag.databinding.ActivityLoginBinding
import com.example.fixtag.R
import com.example.fixtag.ui.main.MainActivity
import kotlinx.coroutines.launch

class LoginActivity: AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels {
        val app = applicationContext as FixTag
        LoginViewModelFactory(app.repository)
    }

    private lateinit var binding: ActivityLoginBinding
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            result.filterValues { granted -> !granted }.keys.forEach {
                Log.d("LoginActivity", "Permission $it not granted")
            }
        }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null && !BuildConfig.OFFLINE_DEMO) {
            val missing = buildList {
                if (!isGranted(Manifest.permission.INTERNET)) {
                    add(Manifest.permission.INTERNET)
                }
                if (Build.VERSION.SDK_INT >= 37 && !isGranted(Manifest.permission.ACCESS_LOCAL_NETWORK)) {
                    add(Manifest.permission.ACCESS_LOCAL_NETWORK)
                }
            }
            if (missing.isNotEmpty()) requestPermissions.launch(missing.toTypedArray())
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.login_nav_host) as NavHostFragment
        navController = navHostFragment.navController
        // the login form is the graph's start destination
        navController.setGraph(R.navigation.login_nav_graph)

        // only while started: a login that finishes in the background would otherwise navigate
        // after onSaveInstanceState, and that fragment transaction throws
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.navigationState.collect { state ->
                    when (state) {
                        is LoginViewModel.NavigationState.Loading -> { }
                        is LoginViewModel.NavigationState.GoToLogin -> { }  // already there
                        is LoginViewModel.NavigationState.GoToPasswordChange -> {
                            Log.d("LoginActivity", "Changing view to password change")
                            goToPasswordChange()
                        }
                        is LoginViewModel.NavigationState.GoToMain -> {
                            Log.d("LoginActivity", "Changing view to main activity")
                            goToMain()
                        }
                    }
                }
            }
        }
    }

    // the state is still GoToPasswordChange after a rotation, and the nav controller restores
    // its back stack by itself, so navigating again would stack a second form
    private fun goToPasswordChange() {
        if (navController.currentDestination?.id == R.id.password_change) return
        // the form replaces the login one, so Back leaves the app. The account stays signed in,
        // and the next launch comes back to this form (MainViewModel sends it here)
        navController.navigate(R.id.password_change, null, navOptions {
            popUpTo(R.id.login) { inclusive = true }
        })
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            // Clear the stack so this new MainActivity becomes the root screen
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        setResult(RESULT_OK)

        finish()
    }
}
