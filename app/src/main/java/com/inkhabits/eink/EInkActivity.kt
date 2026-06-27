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

    /** True only for the first onResume after onCreate (a freshly opened screen). */
    private var freshCreate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        freshCreate = true
        try {
            EInkUtils.setWindowMode(this, UpdateMode.GU)
        } catch (_: Throwable) {
        }
    }

    override fun onResume() {
        super.onResume()
        if (freshCreate) {
            // First show of a newly created screen: the window's own GU paint already
            // renders the new content cleanly. The old code fired a SECOND full-screen
            // GU repaint 50ms later — a redundant, delayed flash that was the main cause
            // of sluggish-feeling tab switches. Skip it; the screen is already clean.
            freshCreate = false
        } else {
            // Returning to an existing screen (e.g. back from a spoke): one immediate
            // full GU repaint clears any ghosting left by the screen we came back from.
            EInkUtils.lightRefresh(window.decorView)
        }
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
