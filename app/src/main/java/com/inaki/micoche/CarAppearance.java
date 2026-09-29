package com.inaki.micoche;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/** Renders six original, brand-free, realistic top-view vehicle illustrations. */
public final class CarAppearance {
    private static final String PREFS = "car_appearance";
    private static final String KEY_MODEL = "model";
    private static final String KEY_COLOR = "color";

    public static final String[] MODELS = {"Urbano", "Berlina", "SUV", "Furgoneta"};
    public static final String[] COLORS = {"Orange", "Red", "Blue", "Black", "White", "Green", "Yellow"};

    private static final int[] MODEL_RESOURCES = {
            R.drawable.car_urban, R.drawable.car_sedan, R.drawable.car_suv, R.drawable.car_van
    };
    private static final int[] COLOR_VALUES = {
            0xffff7900, 0xffdc2626, 0xff2477e8, 0xff202329,
            0xfff2f3f5, 0xff15965f, 0xffffc928
    };

    private CarAppearance() { }
    public static int model(Context c) { return prefs(c).getInt(KEY_MODEL, 0); }
    public static int color(Context c) { return prefs(c).getInt(KEY_COLOR, 0); }
    public static void save(Context c, int model, int color) {
        prefs(c).edit().putInt(KEY_MODEL, clamp(model, MODELS.length))
                .putInt(KEY_COLOR, clamp(color, COLORS.length)).apply();
    }

    public static Bitmap render(Context context, int width, int height) {
        int modelIndex = clamp(model(context), MODEL_RESOURCES.length);
        int colorIndex = clamp(color(context), COLOR_VALUES.length);
        Bitmap source = BitmapFactory.decodeResource(context.getResources(), MODEL_RESOURCES[modelIndex]);
        if (source == null) return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int[] pixels = new int[sourceWidth * sourceHeight];
        source.getPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);
        recolorBody(pixels, COLOR_VALUES[colorIndex], colorIndex);

        int left = sourceWidth, top = sourceHeight, right = -1, bottom = -1;
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < sourceWidth; x++) {
                if (Color.alpha(pixels[y * sourceWidth + x]) > 20) {
                    left = Math.min(left, x); top = Math.min(top, y);
                    right = Math.max(right, x); bottom = Math.max(bottom, y);
                }
            }
        }

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        if (right < left || bottom < top) return result;
        Bitmap colored = Bitmap.createBitmap(pixels, sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888);
        Rect sourceRect = new Rect(left, top, right + 1, bottom + 1);
        float scale = Math.min(width * .94f / sourceRect.width(), height * .94f / sourceRect.height());
        float drawnWidth = sourceRect.width() * scale;
        float drawnHeight = sourceRect.height() * scale;
        RectF destination = new RectF((width - drawnWidth) / 2f, (height - drawnHeight) / 2f,
                (width + drawnWidth) / 2f, (height + drawnHeight) / 2f);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(colored, sourceRect, destination,
                new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        return result;
    }

    private static void recolorBody(int[] pixels, int target, int colorIndex) {
        int targetR = Color.red(target), targetG = Color.green(target), targetB = Color.blue(target);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int alpha = Color.alpha(pixel);
            if (alpha == 0) continue;
            int r = Color.red(pixel), g = Color.green(pixel), b = Color.blue(pixel);
            // Replace only the orange painted metal. Glass, tyres, lights,
            // reflections and panel details keep their original appearance.
            boolean paintedMetal = r > 105 && g > 42 && r > g * 1.22f && g > b * 1.18f;
            if (!paintedMetal) continue;
            float brightness = Math.max(r, Math.max(g, b)) / 255f;
            int outR, outG, outB;
            if (colorIndex == 3) {
                int shade = clampChannel(Math.round(12 + 72 * brightness));
                outR = shade; outG = shade + 2; outB = shade + 5;
            } else if (colorIndex == 4) {
                int shade = clampChannel(Math.round(105 + 145 * brightness));
                outR = shade; outG = shade; outB = Math.min(255, shade + 3);
            } else {
                float shade = .30f + .74f * brightness;
                outR = clampChannel(Math.round(targetR * shade));
                outG = clampChannel(Math.round(targetG * shade));
                outB = clampChannel(Math.round(targetB * shade));
            }
            pixels[i] = Color.argb(alpha, outR, outG, outB);
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    private static int clamp(int value, int count) { return Math.max(0, Math.min(value, count - 1)); }
    private static int clampChannel(int value) { return Math.max(0, Math.min(value, 255)); }
}
