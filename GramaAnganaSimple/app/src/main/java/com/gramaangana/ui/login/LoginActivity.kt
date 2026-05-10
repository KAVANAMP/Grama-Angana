package com.gramaangana.ui.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.gramaangana.databinding.ActivityLoginBinding
import com.gramaangana.ui.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREF_NAME = "GramaAnganaPrefs"
        const val KEY_ROLE = "user_role"
        const val KEY_NAME = "user_name"
        const val KEY_EMAIL = "user_email"
        const val ROLE_ADMIN = "ADMIN"
        const val ROLE_USER = "USER"

        // Admin credentials (Panchayat)
        const val ADMIN_EMAIL = "admin@gramaangana.com"
        const val ADMIN_PASSWORD = "panchayat123"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        // Check if already logged in
        if (prefs.getString(KEY_ROLE, null) != null) {
            goToMain()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Toggle between Admin and User login
        binding.btnAdmin.setOnClickListener {
            binding.btnAdmin.isSelected = true
            binding.btnUser.isSelected = false
            binding.tilEmail.hint = "Admin Email"
            binding.tilPassword.hint = "Admin Password"
            binding.tvRoleHint.text = "Login as Panchayat Admin"
            binding.tvRoleHint.visibility = View.VISIBLE
        }

        binding.btnUser.setOnClickListener {
            binding.btnAdmin.isSelected = false
            binding.btnUser.isSelected = true
            binding.tilEmail.hint = "Your Name"
            binding.tilPassword.hint = "Mobile Number (last 4 digits)"
            binding.tvRoleHint.text = "Login as Community Member"
            binding.tvRoleHint.visibility = View.VISIBLE
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.tilEmail.error = "Required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.tilPassword.error = "Required"
                return@setOnClickListener
            }

            binding.tilEmail.error = null
            binding.tilPassword.error = null

            if (binding.btnAdmin.isSelected) {
                loginAsAdmin(email, password)
            } else {
                loginAsUser(email, password)
            }
        }
    }

    private fun loginAsAdmin(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        // Check hardcoded admin credentials
        if (email == ADMIN_EMAIL && password == ADMIN_PASSWORD) {
            saveSession(ROLE_ADMIN, "Panchayat Admin", email)
            goToMain()
        } else {
            // Try Firebase auth for admin
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    saveSession(ROLE_ADMIN, "Panchayat Admin", email)
                    goToMain()
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, "❌ Invalid admin credentials!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loginAsUser(name: String, mobile: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        // Simple validation for community member
        if (name.length < 2) {
            binding.tilEmail.error = "Enter your full name"
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            return
        }
        if (mobile.length < 4) {
            binding.tilPassword.error = "Enter last 4 digits of mobile"
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            return
        }

        // Save user session
        saveSession(ROLE_USER, name, "")
        goToMain()
    }

    private fun saveSession(role: String, name: String, email: String) {
        prefs.edit()
            .putString(KEY_ROLE, role)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
