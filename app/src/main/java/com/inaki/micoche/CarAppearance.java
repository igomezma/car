package com.inaki.micoche;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public final class CarAppearance {
    private static final String PREFS = "car_appearance";
    private static final String KEY_MODEL = "model";
    private static final String KEY_COLOR = "color";
    public static final String[] MODELS = {"Urban", "Sedan", "SUV", "Sport", "Off-road", "Van"};
    public static final String[] COLORS = {"Orange", "Red", "Blue", "Black", "White", "Silver", "Green", "Yellow"};
    private static final int[] COLOR_VALUES = {0xffff7a00, 0xffe53935, 0xff2979ff, 0xff24262b, 0xfff5f5f5, 0xffaeb5bd, 0xff19a76f, 0xffffc928};

    private CarAppearance() {}
    public static int model(Context c) { return prefs(c).getInt(KEY_MODEL, 0); }
    public static int color(Context c) { return prefs(c).getInt(KEY_COLOR, 0); }
    public static void save(Context c, int model, int color) { prefs(c).edit().putInt(KEY_MODEL, clamp(model, MODELS.length)).putInt(KEY_COLOR, clamp(color, COLORS.length)).apply(); }

    public static Bitmap render(Context context, int width, int height) {
        int model = clamp(model(context), MODELS.length);
        int body = COLOR_VALUES[clamp(color(context), COLOR_VALUES.length)];
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float cx = width / 2f, top = height * .05f, bottom = height * .95f, half;
        switch (model) {
            case 1: half = width * .31f; break;
            case 2: half = width * .40f; break;
            case 3: half = width * .35f; top = height * .12f; break;
            case 4: half = width * .42f; break;
            case 5: half = width * .39f; break;
            default: half = width * .34f;
        }
        paint.setColor(Color.argb(85, 0, 0, 0));
        canvas.drawRoundRect(new RectF(cx-half+3, top+5, cx+half+3, bottom+5), width*.18f, width*.18f, paint);
        paint.setColor(body);
        float radius = model == 5 ? width*.11f : width*.20f;
        canvas.drawRoundRect(new RectF(cx-half, top, cx+half, bottom), radius, radius, paint);
        paint.setColor(darken(body));
        float cabinTop = model == 5 ? height*.18f : height*.25f;
        float cabinBottom = model == 3 ? height*.66f : height*.70f;
        float cabinHalf = half * ((model == 2 || model == 4) ? .78f : .72f);
        canvas.drawRoundRect(new RectF(cx-cabinHalf, cabinTop, cx+cabinHalf, cabinBottom), width*.12f, width*.12f, paint);
        paint.setColor(0xffa9d9ef);
        float inset = width*.035f;
        canvas.drawRoundRect(new RectF(cx-cabinHalf+inset, cabinTop+inset, cx+cabinHalf-inset, height*.43f), width*.07f, width*.07f, paint);
        canvas.drawRoundRect(new RectF(cx-cabinHalf+inset, height*.52f, cx+cabinHalf-inset, cabinBottom-inset), width*.07f, width*.07f, paint);
        paint.setColor(0xff202329);
        float ww=width*.105f, wh=height*.16f, wx=half+width*.02f;
        canvas.drawRoundRect(new RectF(cx-wx,height*.20f,cx-wx+ww,height*.20f+wh),5,5,paint);
        canvas.drawRoundRect(new RectF(cx+wx-ww,height*.20f,cx+wx,height*.20f+wh),5,5,paint);
        canvas.drawRoundRect(new RectF(cx-wx,height*.66f,cx-wx+ww,height*.66f+wh),5,5,paint);
        canvas.drawRoundRect(new RectF(cx+wx-ww,height*.66f,cx+wx,height*.66f+wh),5,5,paint);
        paint.setColor(0xfffff3b0);
        canvas.drawCircle(cx-half*.55f, top+height*.06f, width*.055f, paint);
        canvas.drawCircle(cx+half*.55f, top+height*.06f, width*.055f, paint);
        return bitmap;
    }
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    private static int clamp(int v, int n) { return Math.max(0, Math.min(v, n-1)); }
    private static int darken(int c) { return Color.rgb((int)(Color.red(c)*.55f),(int)(Color.green(c)*.55f),(int)(Color.blue(c)*.55f)); }
}
