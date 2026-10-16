package com.example.fixtag.ui.login

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.fixtag.AppViewModelFactory
import com.example.fixtag.FixTag
import com.example.fixtag.databinding.ActivityLoginBinding
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.User
import com.example.fixtag.ui.main.MainActivity
import com.example.fixtag.ui.afterTextChanged
import kotlinx.coroutines.launch

class LoginActivity: AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels {
        val app = applicationContext as FixTag
        LoginViewModelFactory(app.repository)
    }

    private lateinit var binding: ActivityLoginBinding

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

        if (savedInstanceState == null) {
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

        val username = binding.etEmail
        val password = binding.etPassword
        val login = binding.login
        val loading = binding.loading

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.emailError != null) {
                username.error = getString(loginState.emailError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            } else if (loginResult.success != null) {
                handleLoginSuccess(loginResult.success)
            }
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }
        }

        login.setOnClickListener {
            loading.visibility = View.VISIBLE
            loginViewModel.login(username.text.toString(), password.text.toString())
        }
    }

    private fun handleLoginSuccess(user: User?) {
        val user = user!!

        val welcome = getString(R.string.welcome, user.name)

        Toast.makeText(
            applicationContext,
            welcome,
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(this, MainActivity::class.java).apply {
            // Clear the stack so this new MainActivity becomes the root screen
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        setResult(RESULT_OK)

        finish()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}
