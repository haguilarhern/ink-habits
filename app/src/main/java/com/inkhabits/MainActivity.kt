package com.inkhabits

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.inkhabits.data.AppDatabase
import com.inkhabits.eink.EInkActivity
import com.inkhabits.ui.dashboard.DashboardActivity
import com.inkhabits.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.launch

/** Entry point: routes to onboarding on first run, otherwise the dashboard. */
class MainActivity : EInkActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.get(this)
        lifecycleScope.launch {
            val hasIdentities = db.identityGoalDao().count() > 0
            val next = if (hasIdentities) DashboardActivity::class.java
            else OnboardingActivity::class.java
            startActivity(Intent(this@MainActivity, next))
            finish()
        }
    }
}
