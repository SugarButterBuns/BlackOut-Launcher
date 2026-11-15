package dev.blackhome.launcher;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityOptions;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Google TV sends the Home button straight to its own launcher, ignoring the default
 * Home app (on this TV the key never even reaches accessibility key filtering).
 * So this service watches the Google TV launcher and the moment its home screen shows,
 * replaces it with BlackOut Launcher. Only the home screen is redirected, so the launcher's
 * settings panel and other screens keep working.
 */
public class HomeKeyService extends AccessibilityService {

    private static final String GTV_LAUNCHER = "com.google.android.apps.tv.launcherx";

    /** True while the last screen Google TV's launcher showed was its home screen. */
    private boolean gtvHomeShowing;

    @Override
    protected void onServiceConnected() {
        // Runs at boot and after an update: make sure we start on BlackOut Launcher.
        goHome();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // Kept in case a future firmware lets HOME through to accessibility.
        if (event.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) goHome();
        return true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        CharSequence pkg = e.getPackageName();
        if (pkg == null || !GTV_LAUNCHER.contentEquals(pkg)) return;

        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence cls = e.getClassName();
            gtvHomeShowing = cls != null && cls.toString().endsWith("home.HomeActivity");
            if (gtvHomeShowing) goHome();
        } else if (gtvHomeShowing) {
            // Google TV home is somehow still on screen (Home pressed again while it
            // was already in front, so no new window event): any activity on it sends us home.
            goHome();
        }
    }

    private void goHome() {
        gtvHomeShowing = false;
        Intent i = new Intent(this, HomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        try {
            startActivity(i, ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle());
        } catch (Exception ignored) { }
    }

    @Override
    public void onInterrupt() { }
}
