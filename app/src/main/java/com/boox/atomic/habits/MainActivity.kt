package com.boox.atomic.habits

import android.content.Intent
import android.os.Bundle
import com.boox.atomic.habits.ui.EInkActivity
import com.boox.atomic.habits.ui.dashboard.DashboardActivity
import com.boox.atomic.habits.ui.setup.SetupActivity

class MainActivity : EInkActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("boox_habits_prefs", MODE_PRIVATE)
        val isOnboarded = prefs.getBoolean("is_onboarded", false)

        if (!isOnboarded) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}