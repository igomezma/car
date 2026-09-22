package com.inaki.micoche;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

/** Draws original, brand-free but realistically proportioned top-view cars. */
public final class CarAppearance {
    private static final String PREFS = "car_appearance";
    private static final String KEY_MODEL = "model";
    private static final String KEY_COLOR = "color";
    public static final String[] MODELS = {"Urban", "Sedan", "SUV", "Sport", "Off-road", "Van"};
    public static final String[] COLORS = {"Orange", "Red", "Blue", "Black", "White", "Green", "Yellow"};
    private static final int[] COLOR_VALUES = {
            0xffff7900, 0xffe33131, 0xff2477e8, 0xff202329,
            0xfff2f3f5, 0xff149562, 0xffffc928
    };

    private CarAppearance() { }
    public static int model(Context c) { return prefs(c).getInt(KEY_MODEL, 0); }
    public static int color(Context c) { return prefs(c).getInt(KEY_COLOR, 0); }
    public static void save(Context c, int model, int color) {
        prefs(c).edit().putInt(KEY_MODEL, clamp(model, MODELS.length))
                .putInt(KEY_COLOR, clamp(color, COLORS.length)).apply();
    }

    public static Bitmap render(Context context, int width, int height) {
        int model = clamp(model(context), MODELS.length);
        int body = COLOR_VALUES[clamp(color(context), COLOR_VALUES.length)];
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        float cx = width / 2f;
        float top = height * (model == 3 ? .10f : .055f);
        float bottom = height * .945f;
        float halfFront, halfRear, cabinStart, cabinEnd;
        switch (model) {
            case 1: // sedan: long bonnet and boot
                halfFront = width*.31f; halfRear = width*.33f; cabinStart=.29f; cabinEnd=.66f; break;
            case 2: // SUV: wide, tall cabin
                halfFront = width*.40f; halfRear = width*.41f; cabinStart=.20f; cabinEnd=.75f; break;
            case 3: // sport: low, tapered and cab-rearward
                halfFront = width*.32f; halfRear = width*.37f; cabinStart=.34f; cabinEnd=.70f; break;
            case 4: // off-road: widest, square shoulders
                halfFront = width*.41f; halfRear = width*.42f; cabinStart=.23f; cabinEnd=.72f; break;
            case 5: // van: almost rectangular
                halfFront = width*.37f; halfRear = width*.39f; cabinStart=.16f; cabinEnd=.78f; break;
            default: // urban: short and compact
                halfFront = width*.34f; halfRear = width*.35f; cabinStart=.22f; cabinEnd=.72f;
        }

        // Ground shadow and four visible tyres.
        p.setColor(0x42000000);
        canvas.drawOval(new RectF(cx-halfRear-width*.05f, top+height*.035f,
                cx+halfRear+width*.07f, bottom+height*.025f), p);
        p.setColor(0xff17191d);
        float tyreW=width*.105f, tyreH=height*(model==4?.18f:.15f);
        drawTyre(canvas,p,cx-halfRear-width*.025f,top+height*.17f,tyreW,tyreH);
        drawTyre(canvas,p,cx+halfRear-width*.08f,top+height*.17f,tyreW,tyreH);
        drawTyre(canvas,p,cx-halfRear-width*.025f,bottom-height*.31f,tyreW,tyreH);
        drawTyre(canvas,p,cx+halfRear-width*.08f,bottom-height*.31f,tyreW,tyreH);

        // Sculpted body silhouette, different for every body type.
        Path bodyPath = new Path();
        float shoulder=height*.13f;
        bodyPath.moveTo(cx, top);
        bodyPath.cubicTo(cx-halfFront*.72f, top, cx-halfFront, top+shoulder*.48f, cx-halfFront, top+shoulder);
        bodyPath.lineTo(cx-halfRear, bottom-height*.17f);
        bodyPath.cubicTo(cx-halfRear, bottom-height*.05f, cx-halfRear*.72f, bottom, cx, bottom);
        bodyPath.cubicTo(cx+halfRear*.72f, bottom, cx+halfRear, bottom-height*.05f, cx+halfRear, bottom-height*.17f);
        bodyPath.lineTo(cx+halfFront, top+shoulder);
        bodyPath.cubicTo(cx+halfFront, top+shoulder*.48f, cx+halfFront*.72f, top, cx, top);
        bodyPath.close();
        p.setShader(new LinearGradient(cx-halfRear,0,cx+halfRear,0,
                new int[]{darken(body,.72f),body,lighten(body,1.22f),body,darken(body,.70f)},
                new float[]{0f,.20f,.50f,.78f,1f}, Shader.TileMode.CLAMP));
        canvas.drawPath(bodyPath,p); p.setShader(null);

        // Bonnet and boot contour lines give the car believable panels.
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(1.5f,width*.018f));
        p.setColor(withAlpha(darken(body,.62f),125));
        canvas.drawArc(new RectF(cx-halfFront*.80f,top+height*.045f,cx+halfFront*.80f,top+height*.30f),8,164,false,p);
        canvas.drawArc(new RectF(cx-halfRear*.82f,bottom-height*.25f,cx+halfRear*.82f,bottom-height*.035f),188,164,false,p);
        p.setStyle(Paint.Style.FILL);

        // Glasshouse with front/rear windscreens and side glass.
        float glassTop=height*cabinStart, glassBottom=height*cabinEnd;
        float glassHalf=Math.min(halfFront,halfRear)*(model==3?.66f:model==5?.82f:.74f);
        Path glass = new Path();
        glass.moveTo(cx-glassHalf*.70f,glassTop);
        glass.quadTo(cx-glassHalf,glassTop+height*.07f,cx-glassHalf,glassTop+height*.14f);
        glass.lineTo(cx-glassHalf*.91f,glassBottom-height*.06f);
        glass.quadTo(cx,glassBottom+height*.02f,cx+glassHalf*.91f,glassBottom-height*.06f);
        glass.lineTo(cx+glassHalf,glassTop+height*.14f);
        glass.quadTo(cx+glassHalf,glassTop+height*.07f,cx+glassHalf*.70f,glassTop);
        glass.close();
        p.setShader(new LinearGradient(0,glassTop,0,glassBottom,
                0xffbfe7f5,0xff243b4b,Shader.TileMode.CLAMP));
        canvas.drawPath(glass,p); p.setShader(null);
        p.setColor(0x55ffffff);
        Path reflection=new Path(); reflection.moveTo(cx-glassHalf*.55f,glassTop+height*.025f);
        reflection.lineTo(cx-glassHalf*.18f,glassTop+height*.025f);
        reflection.lineTo(cx+glassHalf*.25f,glassBottom-height*.06f);
        reflection.lineTo(cx-glassHalf*.05f,glassBottom-height*.06f); reflection.close();
        canvas.drawPath(reflection,p);

        // Roof divider, mirrors, lamps, grille and model-specific details.
        p.setColor(withAlpha(lighten(body,1.30f),165));
        canvas.drawRoundRect(new RectF(cx-width*.018f,glassTop+height*.03f,cx+width*.018f,glassBottom-height*.025f),8,8,p);
        p.setColor(body);
        canvas.drawOval(new RectF(cx-halfFront-width*.085f,top+height*.24f,cx-halfFront+width*.035f,top+height*.31f),p);
        canvas.drawOval(new RectF(cx+halfFront-width*.035f,top+height*.24f,cx+halfFront+width*.085f,top+height*.31f),p);
        p.setColor(0xfffff1b5);
        canvas.drawRoundRect(new RectF(cx-halfFront*.72f,top+height*.025f,cx-halfFront*.25f,top+height*.078f),9,9,p);
        canvas.drawRoundRect(new RectF(cx+halfFront*.25f,top+height*.025f,cx+halfFront*.72f,top+height*.078f),9,9,p);
        p.setColor(0xffb31518);
        canvas.drawRoundRect(new RectF(cx-halfRear*.72f,bottom-height*.070f,cx-halfRear*.25f,bottom-height*.022f),8,8,p);
        canvas.drawRoundRect(new RectF(cx+halfRear*.25f,bottom-height*.070f,cx+halfRear*.72f,bottom-height*.022f),8,8,p);
        p.setColor(0xff252a30);
        canvas.drawRoundRect(new RectF(cx-halfFront*.37f,top+height*.095f,cx+halfFront*.37f,top+height*.125f),6,6,p);
        if(model==4){ // off-road roof rails
            p.setColor(0xff3c4148);
            canvas.drawRoundRect(new RectF(cx-glassHalf-width*.045f,glassTop,cx-glassHalf-width*.015f,glassBottom),6,6,p);
            canvas.drawRoundRect(new RectF(cx+glassHalf+width*.015f,glassTop,cx+glassHalf+width*.045f,glassBottom),6,6,p);
        } else if(model==3){ // sport centre stripe
            p.setColor(0x55ffffff);
            canvas.drawRoundRect(new RectF(cx-width*.025f,top,cx+width*.025f,glassTop-height*.015f),5,5,p);
        }
        return bitmap;
    }

    private static void drawTyre(Canvas c,Paint p,float x,float y,float w,float h){c.drawRoundRect(new RectF(x,y,x+w,y+h),w*.30f,w*.30f,p);}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private static int clamp(int v,int n){return Math.max(0,Math.min(v,n-1));}
    private static int withAlpha(int c,int a){return Color.argb(a,Color.red(c),Color.green(c),Color.blue(c));}
    private static int darken(int c,float f){return Color.rgb((int)(Color.red(c)*f),(int)(Color.green(c)*f),(int)(Color.blue(c)*f));}
    private static int lighten(int c,float f){return Color.rgb(Math.min(255,(int)(Color.red(c)*f)),Math.min(255,(int)(Color.green(c)*f)),Math.min(255,(int)(Color.blue(c)*f)));}
}
