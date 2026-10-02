package com.inaki.micoche;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

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

    /*
     * Una imagen INDEPENDIENTE para cada coche.
     *
     * TOP  -> imagen utilizada en el mapa.
     * SIDE -> imagen utilizada en el selector grande.
     */
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

    private CarAppearance() {
    }

    public static int model(Context context) {
        int value = context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_MODEL, 0);

        return Math.max(0, Math.min(value, MODELS.length - 1));
    }

    public static int color(Context context) {
        return 0;
    }

    public static void save(Context context, int model, int ignored) {
        int safeModel =
                Math.max(0, Math.min(model, MODELS.length - 1));

        context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_MODEL, safeModel)
                .apply();
    }

    /**
     * Dibuja UN PNG completo dentro del bitmap final.
     *
     * IMPORTANTE:
     * - No recorta la imagen.
     * - No usa cropTop/cropBottom.
     * - Conserva la relación de aspecto.
     * - Centra el coche.
     * - Deja un pequeño margen de seguridad.
     */
    private static Bitmap drawFull(
            Context context,
            int resourceId,
            int width,
            int height) {

        Bitmap source = BitmapFactory.decodeResource(
                context.getResources(),
                resourceId
        );

        Bitmap result = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
        );

        if (source == null) {
            return result;
        }

        Canvas canvas = new Canvas(result);

        Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG |
                Paint.FILTER_BITMAP_FLAG |
                Paint.DITHER_FLAG
        );

        /*
         * 92 % del espacio disponible.
         *
         * El coche entra SIEMPRE entero.
         * No se corta ni arriba, ni abajo,
         * ni a izquierda ni a derecha.
         */
        float availableWidth = width * 0.92f;
        float availableHeight = height * 0.92f;

        float scale = Math.min(
                availableWidth / source.getWidth(),
                availableHeight / source.getHeight()
        );

        float drawWidth = source.getWidth() * scale;
        float drawHeight = source.getHeight() * scale;

        float left = (width - drawWidth) / 2f;
        float top = (height - drawHeight) / 2f;

        RectF destination = new RectF(
                left,
                top,
                left + drawWidth,
                top + drawHeight
        );

        canvas.drawBitmap(
                source,
                null,
                destination,
                paint
        );

        return result;
    }

    /**
     * Imagen del coche utilizada en el mapa.
     */
    public static Bitmap render(
            Context context,
            int width,
            int height) {

        int selectedModel = model(context);

        return drawFull(
                context,
                TOP[selectedModel],
                width,
                height
        );
    }

    /**
     * Imagen grande del selector.
     *
     * Cada modelo utiliza SU PROPIO PNG.
     */
    public static Bitmap renderSide(
            Context context,
            int width,
            int height) {

        int selectedModel = model(context);

        return drawFull(
                context,
                SIDE[selectedModel],
                width,
                height
        );
    }
}
