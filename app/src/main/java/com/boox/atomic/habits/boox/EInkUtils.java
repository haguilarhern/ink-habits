package com.boox.atomic.habits.boox;

import android.app.Activity;
import android.view.View;
import android.webkit.WebView;
import androidx.recyclerview.widget.RecyclerView;
import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.api.device.epd.UpdateMode;

/**
 * Utility class for configuring Boox E-Ink display update modes.
 * <p>
 * Each method is wrapped in a try/catch to gracefully handle
 * non-Boox devices where the Onyx SDK classes may not be present.
 */
public final class EInkUtils {

    private EInkUtils() {
        // Utility class — prevent instantiation
    }

    /**
     * Sets ANIMATION mode on a RecyclerView for smooth scrolling.
     * Best for list/timeline scrolling where motion smoothness is desired.
     */
    public static void setFastMode(RecyclerView recyclerView) {
        try {
            EpdController.setViewDefaultUpdateMode(recyclerView, UpdateMode.ANIMATION);
        } catch (Exception e) {
            // Non-Boox device or SDK not available
        }
    }

    /**
     * Sets DU (Direct Update) mode — the lowest latency mode for stylus input.
     * Use for drawing/scribble surfaces where pen responsiveness is critical.
     * DU mode sacrifices gray levels for speed (only black/white).
     */
    public static void setPenMode(View view) {
        try {
            EpdController.setViewDefaultUpdateMode(view, UpdateMode.DU);
        } catch (Exception e) {
            // Non-Boox device or SDK not available
        }
    }

    /**
     * Sets GU (Gray Update) mode with 16-level gray rendering.
     * Best for general-purpose UI where gray levels and moderate refresh
     * speed are needed.
     */
    public static void setGeneralMode(View view) {
        try {
            EpdController.setViewDefaultUpdateMode(view, UpdateMode.GU);
        } catch (Exception e) {
            // Non-Boox device or SDK not available
        }
    }

    /**
     * Sets REGAL mode optimized for text rendering.
     * REGAL provides high-contrast text with minimal ghosting,
     * ideal for reading-focused views like readers and text-heavy screens.
     */
    public static void setRegalMode(View view) {
        try {
            EpdController.setViewDefaultUpdateMode(view, UpdateMode.REGAL);
        } catch (Exception e) {
            // Non-Boox device or SDK not available
        }
    }

    /**
     * Performs a full clean refresh (GC — Global Clear) on the given view.
     * Use this to eliminate any residual ghosting from previous screen content.
     * Typically called after finishing a drawing stroke or clearing a canvas.
     */
    public static void cleanRefresh(View view) {
        try {
            EpdController.invalidate(view, UpdateMode.GC);
        } catch (Exception e) {
            // Non-Boox device or SDK not available
        }
    }

    /**
     * Sets the default update mode for an entire Activity window.
     * This affects all views within the activity unless overridden per-view.
     *
     * @param activity   the target Activity
     * @param updateMode the desired UpdateMode constant
     */
    public static void setWindowMode(Activity activity, UpdateMode updateMode) {
        try {
            EpdController.setViewDefaultUpdateMode(activity.getWindow().getDecorView(), updateMode);
        } catch (Exception e) {
            // Non-Boox device or SDK not available
        }
    }

    /**
     * Enables or disables contrast optimization for a WebView.
     * When enabled, the E-Ink controller applies extra contrast to improve
     * text readability on web content.
     *
     * @param webView the WebView to configure
     * @param enable  true to enable contrast optimization, false to disable
     */
    public static void optimizeWebView(WebView webView, boolean enable) {
        try {
            EpdController.setWebViewContrastOptimize(webView, enable);
        } catch (Exception e) {
            // Non-Boox device or SDK not available
        }
    }

    /**
     * Enters fast system-wide refresh mode.
     * Useful during animations, page turns, or high-frequency UI updates.
     * Must be paired with a corresponding call to {@link #leaveFastMode()}.
     */
    public static void enterFastMode() {
        try {
            EpdController.applySystemFastMode(true);
        } catch (Exception e) {
            // Non-Boox device or SDK not available
        }
    }

    /**
     * Leaves fast system-wide refresh mode and returns to the default
     * update strategy. Call when fast updates are no longer needed.
     */
    public static void leaveFastMode() {
        try {
            EpdController.applySystemFastMode(false);
        } catch (Exception e) {
            // Non-Boox device or SDK not available
        }
    }

    /**
     * Convenience wrapper that runs a {@link Runnable} inside fast mode.
     * Automatically enters fast mode, executes the action, then leaves fast mode.
     *
     * @param action the work to perform while in fast mode
     */
    public static void runInFastMode(Runnable action) {
        try {
            EpdController.applySystemFastMode(true);
        } catch (Exception e) {
            // Non-Boox device — proceed without fast mode
        }
        try {
            action.run();
        } finally {
            try {
                EpdController.applySystemFastMode(false);
            } catch (Exception e) {
                // Non-Boox device or SDK not available
            }
        }
    }
}