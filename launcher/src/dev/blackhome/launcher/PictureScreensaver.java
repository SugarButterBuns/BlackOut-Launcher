package dev.blackhome.launcher;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.service.dreams.DreamService;
import android.widget.ImageView;

/**
 * Screensaver: one picture, fitted whole to the screen on pure black.
 * No drift, no dimming, no animation. Uses memory only while it is showing.
 *
 * The picture is whatever you put at launcher/res/raw/screensaver.(webp|jpg|png)
 * before building. With no picture, the screensaver is plain black (OLED pixels off).
 */
public class PictureScreensaver extends DreamService {

    private Bitmap picture;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any button press ends the screensaver
        setFullscreen(true);
        setScreenBright(true);

        int id = getResources().getIdentifier("screensaver", "raw", getPackageName());
        if (id != 0) picture = BitmapFactory.decodeResource(getResources(), id);

        ImageView view = new ImageView(this);
        view.setBackgroundColor(Color.BLACK);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);   // whole image, black bars if needed
        if (picture != null) view.setImageBitmap(picture);
        setContentView(view);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (picture != null) {
            picture.recycle();
            picture = null;
        }
    }
}
