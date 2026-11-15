package dev.blackhome.launcher;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

/**
 * Pure black home screen: date and time top center, a fixed row of apps.
 * No animations, no highlights. The focused app's icon is drawn 5% larger.
 */
public class HomeActivity extends Activity {

    /** The apps shown, in order. Missing ones are skipped. */
    private static final String[] APPS = {
            "com.android.vending",   // Play Store
            "org.smarttube.beta",    // SmartTube beta
            "com.stremio.one",       // Stremio
    };

    private static final float FOCUS_SCALE = 1.05f;

    private LinearLayout row;
    private TextClock clock;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        build();
    }

    private void build() {
        int w = getResources().getDisplayMetrics().widthPixels;   // 1920 on this TV
        int margin = Math.round(w * 0.035f);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.setClipChildren(false);
        root.setClipToPadding(false);

        // Date and time, top center.
        clock = new TextClock(this);
        clock.setFormat12Hour("EEE, MMM d   h:mm a");
        clock.setFormat24Hour("EEE, MMM d   HH:mm");
        clock.setTextColor(0xFFD9D9D9);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, w * 0.0185f);
        clock.setIncludeFontPadding(false);
        // Display only: the clock can't be focused or selected.
        clock.setFocusable(false);
        clock.setClickable(false);
        clock.setLongClickable(false);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        clp.topMargin = margin;
        root.addView(clock, clp);

        // App row, bottom left.
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START);
        rlp.leftMargin = margin * 2;
        rlp.bottomMargin = margin * 2;
        root.addView(row, rlp);

        fillApps(w);

        setContentView(root);
        if (row.getChildCount() > 0) row.getChildAt(0).requestFocus();
    }

    private void fillApps(int w) {
        row.removeAllViews();
        PackageManager pm = getPackageManager();
        int bw = Math.round(w * 0.16f);            // banner 16:9
        int bh = Math.round(bw * 9f / 16f);
        int gap = Math.round(w * 0.022f);
        float radius = bh * 0.14f;

        for (String pkg : APPS) {
            Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (launch == null) launch = pm.getLaunchIntentForPackage(pkg);
            if (launch == null) continue;   // not installed or disabled

            Drawable art;
            CharSequence label;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                label = pm.getApplicationLabel(ai);
                art = pm.getActivityBanner(launch);
                if (art == null) art = pm.getApplicationBanner(ai);
                if (art == null) art = pm.getApplicationIcon(ai);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }

            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER_HORIZONTAL);
            tile.setClipChildren(false);
            tile.setClipToPadding(false);
            tile.setFocusable(true);
            tile.setDefaultFocusHighlightEnabled(false);
            tile.setBackground(null);

            ImageView img = new ImageView(this);
            img.setImageDrawable(art);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            img.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
                }
            });
            img.setClipToOutline(true);
            tile.addView(img, new LinearLayout.LayoutParams(bw, bh));

            TextView name = new TextView(this);
            name.setText(label);
            name.setTextColor(0xFF8C8C8C);
            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, w * 0.0105f);
            name.setSingleLine(true);
            name.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(bw, LinearLayout.LayoutParams.WRAP_CONTENT);
            nlp.topMargin = Math.round(bh * 0.12f);
            tile.addView(name, nlp);

            final Intent target = launch;
            tile.setOnClickListener(v -> open(target));
            tile.setOnFocusChangeListener((v, has) -> scale(img, has));

            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (row.getChildCount() > 0) tlp.leftMargin = gap;
            row.addView(tile, tlp);
        }
    }

    /** Instant size change, no animation. */
    private static void scale(View v, boolean focused) {
        float s = focused ? FOCUS_SCALE : 1f;
        v.setScaleX(s);
        v.setScaleY(s);
    }

    private void open(Intent intent) {
        Intent i = new Intent(intent);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(i, ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle());
        } catch (Exception ignored) { }
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        // Home screen: Back does nothing.
    }
}
