package com.inkhabits.eink;

import android.app.Activity;
import android.view.View;

import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.api.device.epd.UpdateMode;

/**
 * Thin wrapper over the Onyx EPD controller. Every call is guarded so the app
 * runs unchanged on non-Boox hardware (the SDK no-ops / throws are swallowed).
 */
public final class EInkUtils {

    private EInkUtils() {}

    /** Low-latency black/white mode for stylus writing surfaces. */
    public static void setPenMode(View view) {
        try {
            EpdController.setViewDefaultUpdateMode(view, UpdateMode.DU);
        } catch (Throwable ignored) {}
    }

    /** 16-level gray mode for general UI. */
    public static void setGeneralMode(View view) {
        try {
            EpdController.setViewDefaultUpdateMode(view, UpdateMode.GU);
        } catch (Throwable ignored) {}
    }

    /** Full clean refresh (GC) to clear ghosting. */
    public static void cleanRefresh(View view) {
        try {
            EpdController.invalidate(view, UpdateMode.GC);
        } catch (Throwable ignored) {}
    }

    /** Smooth-scroll mode: ANIMATION is purpose-built for scrolling content on Boox. */
    public static void setScrollMode(View view) {
        try {
            EpdController.setViewDefaultUpdateMode(view, UpdateMode.ANIMATION);
        } catch (Throwable ignored) {}
    }

    /**
     * Toggle the panel's system fast mode for fluid motion (flings/scrolls). Must be
     * cleared once motion ends, followed by a clean refresh. Greatly smooths lists.
     */
    public static void systemFastMode(boolean enable) {
        try {
            EpdController.applySystemFastMode(enable);
        } catch (Throwable ignored) {}
    }

    /** Set the default update mode for an entire activity window. */
    public static void setWindowMode(Activity activity, UpdateMode updateMode) {
        try {
            EpdController.setViewDefaultUpdateMode(
                    activity.getWindow().getDecorView(), updateMode);
        } catch (Throwable ignored) {}
    }
}
