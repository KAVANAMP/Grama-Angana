package com.gramaangana.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.gramaangana.R
import com.gramaangana.databinding.ActivityMainBinding
import com.gramaangana.databinding.ActivitySplashBinding
import com.gramaangana.ui.login.LoginActivity


class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivLogo.alpha = 0f
        binding.tvAppName.alpha = 0f
        binding.tvTagline.alpha = 0f

        binding.ivLogo.animate().alpha(1f).setDuration(800).start()
        binding.tvAppName.animate().alpha(1f).setDuration(800).setStartDelay(300).start()
        binding.tvTagline.animate().alpha(1f).setDuration(800).setStartDelay(600).start()

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("GramaAnganaPrefs", MODE_PRIVATE)
            val role = prefs.getString("user_role", null)
            if (role != null) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2500)
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        val prefs = getSharedPreferences("GramaAnganaPrefs", MODE_PRIVATE)
        val isAdmin = prefs.getString("user_role", "") == "ADMIN"
        binding.bottomNavigation.menu.findItem(R.id.adminFragment)?.isVisible = isAdmin

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            navController.popBackStack(navController.graph.startDestinationId, false)
            when (item.itemId) {
                R.id.homeFragment -> navController.navigate(R.id.homeFragment)
                R.id.calendarFragment -> navController.navigate(R.id.calendarFragment)
                R.id.maintenanceFragment -> navController.navigate(R.id.maintenanceFragment)
                R.id.eventsFragment -> navController.navigate(R.id.eventsFragment)
                R.id.adminFragment -> navController.navigate(R.id.adminFragment)
            }
            true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
