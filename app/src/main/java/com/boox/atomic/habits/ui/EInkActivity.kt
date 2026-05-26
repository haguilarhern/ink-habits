package com.boox.atomic.habits.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.boox.atomic.habits.boox.EInkUtils
import com.onyx.android.sdk.api.device.epd.UpdateMode

/**
 * Base activity for the Boox Ink Habits app.
 *
 * Configures Onyx Boox E-Ink display update modes for optimal
 * visual quality and performance across all screens.
 */
open class EInkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWindowMode()
    }

    override fun onResume() {
        super.onResume()
        // Post a clean refresh shortly after resume to clear ghosting
        window.decorView.postDelayed({
            cleanRefresh(window.decorView)
        }, 100L)
    }

    /**
     * Sets the window's default update mode to GU (Gray Update)
     * for 16-level gray rendering across the entire activity.
     */
    private fun setWindowMode() {
        try {
            EInkUtils.setWindowMode(this, UpdateMode.GU)
        } catch (_: Exception) {
            // Non-Boox device — gracefully ignore
        }
    }

    /**
     * Optimises a RecyclerView by setting ANIMATION mode for smooth
     * scrolling and scheduling a clean refresh after scrolling stops.
     */
    protected fun optimizeRecyclerView(recyclerView: RecyclerView) {
        try {
            EInkUtils.setFastMode(recyclerView)
        } catch (_: Exception) {
            // Non-Boox device — gracefully ignore
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    cleanRefresh(recyclerView)
                }
            }
        })
    }

    /**
     * Performs a full clean refresh (GC) on the given view to eliminate ghosting.
     */
    protected fun cleanRefresh(view: View) {
        try {
            EInkUtils.cleanRefresh(view)
        } catch (_: Exception) {
            // Non-Boox device — gracefully ignore
        }
    }
}
