package com.inkhabits.eink

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.onyx.android.sdk.api.device.epd.UpdateMode

/**
 * Base activity that configures Boox e-ink update behavior and clears
 * ghosting on resume. No-ops gracefully on non-Boox devices.
 */
open class EInkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            EInkUtils.setWindowMode(this, UpdateMode.GU)
        } catch (_: Throwable) {
        }
    }

    override fun onResume() {
        super.onResume()
        // Light GU repaint on entry (no full-screen GC flash) keeps tab switches snappy.
        window.decorView.postDelayed({ EInkUtils.lightRefresh(window.decorView) }, 50L)
    }

    override fun onPause() {
        super.onPause()
        // Make sure a scroll's system fast-mode never lingers into the next screen
        // (a likely cause of sluggish-feeling navigation).
        EInkUtils.systemFastMode(false)
    }

    protected fun cleanRefresh(view: View) {
        try {
            EInkUtils.cleanRefresh(view)
        } catch (_: Throwable) {
        }
    }
}
