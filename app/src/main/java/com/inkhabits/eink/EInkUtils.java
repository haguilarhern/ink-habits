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

    /** Fastest scroll mode: DU (fast B/W direct update) — lowest latency motion. */
    public static void setScrollMode(View view) {
        try {
            EpdController.setViewDefaultUpdateMode(view, UpdateMode.DU);
        } catch (Throwable ignored) {}
    }

    /**
     * App-scope fast refresh for the duration of a scroll/fling: REGAL partial
     * updates with a clear-to-white to limit ghosting (Onyx's recommended recipe).
     * Pair appScopeFast(pkg,true) on scroll start with appScopeFast(pkg,false) plus
     * a clean GC refresh on scroll stop.
     */
    public static void appScopeFast(String packageName, boolean enable) {
        try {
            if (enable) {
                EpdController.applyAppScopeUpdate(packageName, true, true, UpdateMode.REGAL, 0);
            } else {
                EpdController.clearAppScopeUpdate();
            }
        } catch (Throwable ignored) {}
    }

    /** Light, flash-free repaint (GU) — use when entering a screen. */
    public static void lightRefresh(View view) {
        try {
            EpdController.invalidate(view, UpdateMode.GU);
        } catch (Throwable ignored) {}
    }

    /**
     * Toggle the panel's system fast mode for fluid motion. Kept for completeness;
     * the app-scope REGAL path above is preferred for lists.
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
