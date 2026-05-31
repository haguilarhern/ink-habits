package com.inkhabits.eink

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ScrollView
import androidx.recyclerview.widget.RecyclerView

/**
 * Central e-ink refresh policy for Onyx Boox devices.
 *
 * Boox screens look best when the update mode matches the interaction (a lesson
 * from the Onyx SDK demos): the fast, ghosting-prone DU mode for transient motion
 * like scrolling, the higher-quality GU mode at rest, and an occasional full GC
 * "clean" to clear accumulated ghosting. Doing a full GC flash on every small
 * change is what makes Boox apps feel slow and flickery — so most changes here
 * just ride the current default mode, and we promote to a clean refresh only
 * periodically. All EPD calls go through [EInkUtils] (Java), which reaches the
 * restricted Onyx APIs and no-ops on non-Boox hardware.
 */
object EInk {

    /** Promote to a full clean refresh after this many small changes. */
    private const val CLEAN_EVERY = 10
    private var changes = 0
    private val idle = Handler(Looper.getMainLooper())

    /** Smooth-motion mode (ANIMATION) — use during scrolling/dragging. */
    fun fast(view: View) = EInkUtils.setScrollMode(view)

    /** Higher-quality updates — use at rest. */
    fun quality(view: View) = EInkUtils.setGeneralMode(view)

    /** Full-screen clean refresh: clears ghosting. */
    fun clean(view: View) {
        changes = 0
        EInkUtils.cleanRefresh(view)
    }

    /**
     * Call after a small content change (e.g. a toggle). Cheap most of the time;
     * every [CLEAN_EVERY] calls it does a full clean to keep the panel crisp.
     */
    fun afterChange(view: View) {
        if (++changes >= CLEAN_EVERY) clean(view)
    }

    /**
     * Make a RecyclerView scroll smoothly on e-ink: fast DU updates while dragging /
     * flinging, then quality + a clean refresh once it settles. (Onyx SDK demo pattern.)
     */
    fun attachFastScroll(rv: RecyclerView) {
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(r: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING,
                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        EInkUtils.systemFastMode(true)
                        fast(r)
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        EInkUtils.systemFastMode(false)
                        quality(r)
                        clean(r)
                    }
                }
            }
        })
    }

    /**
     * Same idea for a plain ScrollView, which has no scroll-state callback: go fast
     * on scroll, then debounce back to quality + clean once scrolling stops.
     */
    fun attachFastScroll(sv: ScrollView) {
        val settle = Runnable { EInkUtils.systemFastMode(false); quality(sv); clean(sv) }
        sv.setOnScrollChangeListener { _, _, _, _, _ ->
            EInkUtils.systemFastMode(true)
            fast(sv)
            idle.removeCallbacks(settle)
            idle.postDelayed(settle, 350)
        }
    }
}
