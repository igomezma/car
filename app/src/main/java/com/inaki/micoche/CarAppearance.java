package com.inaki.micoche;

import android.content.Context;
import android.graphics.*;

public final class CarAppearance {

    private static final String PREFS = "car_appearance";
    private static final String KEY_MODEL = "model";

    public static final String[] MODELS = {
            "Troncomóvil",
            "Candy Car",
            "Cloud Car",
            "Space Car",
            "Bubble Car",
            "Cardboard Car"
    };

    private static final int[] TOP = {
            R.drawable.car_log,
            R.drawable.car_candy,
            R.drawable.car_cloud,
            R.drawable.car_space,
            R.drawable.car_bubble,
            R.drawable.car_cardboard
    };

    private static final int[] SIDE = {
            R.drawable.car_log_side,
            R.drawable.car_candy_side,
            R.drawable.car_cloud_side,
            R.drawable.car_space_side,
            R.drawable.car_bubble_side,
            R.drawable.car_cardboard_side
    };

    private CarAppearance() {}

    public static int model(Context c) {
        return Math.max(0, Math.min(
                c.getSharedPreferences(PREFS, 0).getInt(KEY_MODEL, 0), 5));
    }

    public static int color(Context c) {
        return 0;
    }

    public static void save(Context c, int m, int ignored) {
        c.getSharedPreferences(PREFS, 0)
                .edit()
                .putInt(KEY_MODEL, Math.max(0, Math.min(m, 5)))
                .apply();
    }

    private static Bitmap draw(Context c, int[] res, int w, int h) {

        Bitmap src = BitmapFactory.decodeResource(
                c.getResources(), res[model(c)]);

        Bitmap out = Bitmap.createBitmap(
                w, h, Bitmap.Config.ARGB_8888);

        if (src == null) return out;

        Canvas cv = new Canvas(out);

        Paint p = new Paint(
                Paint.ANTI_ALIAS_FLAG |
                Paint.FILTER_BITMAP_FLAG);

        float k = Math.min(
                w * .96f / src.getWidth(),
                h * .96f / src.getHeight());

        float dw = src.getWidth() * k;
        float dh = src.getHeight() * k;

        cv.drawBitmap(
                src,
                null,
                new RectF(
                        (w - dw) / 2f,
                        (h - dh) / 2f,
                        (w + dw) / 2f,
                        (h + dh) / 2f),
                p);

        return out;
    }

    /*
     * Vista lateral del coche.
     *
     * Las imágenes originales tienen pequeños restos gráficos
     * en los extremos superior e inferior. Recortamos esas zonas
     * antes de escalar para que únicamente aparezca el coche
     * seleccionado.
     */
    private static Bitmap drawSide(Context c, int w, int h) {

        Bitmap src = BitmapFactory.decodeResource(
                c.getResources(), SIDE[model(c)]);

        Bitmap out = Bitmap.createBitmap(
                w, h, Bitmap.Config.ARGB_8888);

        if (src == null) return out;

        Canvas cv = new Canvas(out);

        Paint p = new Paint(
                Paint.ANTI_ALIAS_FLAG |
                Paint.FILTER_BITMAP_FLAG);

        // Margen de seguridad:
        // 18 % arriba y 14 % abajo.
        int cropTop =
                Math.round(src.getHeight() * .18f);

        int cropBottom =
                Math.round(src.getHeight() * .14f);

        Rect srcRect = new Rect(
                0,
                cropTop,
                src.getWidth(),
                src.getHeight() - cropBottom);

        float k = Math.min(
                w * .96f / srcRect.width(),
                h * .96f / srcRect.height());

        float dw = srcRect.width() * k;
        float dh = srcRect.height() * k;

        cv.drawBitmap(
                src,
                srcRect,
                new RectF(
                        (w - dw) / 2f,
                        (h - dh) / 2f,
                        (w + dw) / 2f,
                        (h + dh) / 2f),
                p);

        return out;
    }

    public static Bitmap render(Context c, int w, int h) {
        return draw(c, TOP, w, h);
    }

    public static Bitmap renderSide(Context c, int w, int h) {
        return drawSide(c, w, h);
    }
}
