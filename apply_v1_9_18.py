from pathlib import Path
import re
p=Path('app/build.gradle'); s=p.read_text(); s=re.sub(r'versionCode\s+\d+','versionCode 28',s); s=re.sub(r'versionName\s+"[^"]+"','versionName "1.9.18"',s); p.write_text(s)
car='''package com.inaki.micoche;
import android.content.Context;
import android.graphics.*;
public final class CarAppearance {
 private static final String PREFS="car_appearance", KEY_MODEL="model";
 public static final String[] MODELS={"Troncomóvil","Candy Car","Cloud Car","Space Car","Bubble Car","Cardboard Car"};
 private static final int[] TOP={R.drawable.car_log,R.drawable.car_candy,R.drawable.car_cloud,R.drawable.car_space,R.drawable.car_bubble,R.drawable.car_cardboard};
 private static final int[] SIDE={R.drawable.car_log_side,R.drawable.car_candy_side,R.drawable.car_cloud_side,R.drawable.car_space_side,R.drawable.car_bubble_side,R.drawable.car_cardboard_side};
 private CarAppearance(){}
 public static int model(Context c){return Math.max(0,Math.min(c.getSharedPreferences(PREFS,0).getInt(KEY_MODEL,0),5));}
 public static int color(Context c){return 0;}
 public static void save(Context c,int m,int ignored){c.getSharedPreferences(PREFS,0).edit().putInt(KEY_MODEL,Math.max(0,Math.min(m,5))).apply();}
 private static Bitmap draw(Context c,int[] res,int w,int h){Bitmap src=BitmapFactory.decodeResource(c.getResources(),res[model(c)]); Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); if(src==null)return out; Canvas cv=new Canvas(out); Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG); float k=Math.min(w*.96f/src.getWidth(),h*.96f/src.getHeight()); float dw=src.getWidth()*k,dh=src.getHeight()*k; cv.drawBitmap(src,null,new RectF((w-dw)/2f,(h-dh)/2f,(w+dw)/2f,(h+dh)/2f),p); return out;}
 public static Bitmap render(Context c,int w,int h){return draw(c,TOP,w,h);}
 public static Bitmap renderSide(Context c,int w,int h){return draw(c,SIDE,w,h);}
}'''
Path('app/src/main/java/com/inaki/micoche/CarAppearance.java').write_text(car)
p=Path('app/src/main/java/com/inaki/micoche/MainActivity.java'); s=p.read_text(); start=s.index('    private void configureCarAppearance(View view) {'); end=s.index('    private void applyCarAppearance()',start)
method='''    private void configureCarAppearance(View view) {
        ImageView preview=view.findViewById(R.id.carPreview);
        int[] ids={R.id.carChoice0,R.id.carChoice1,R.id.carChoice2,R.id.carChoice3,R.id.carChoice4,R.id.carChoice5};
        preview.setImageBitmap(CarAppearance.renderSide(this,dp(280),dp(160)));
        for(int i=0;i<ids.length;i++){ final int choice=i; View b=view.findViewById(ids[i]); b.setAlpha(choice==CarAppearance.model(this)?1f:.62f); b.setOnClickListener(v->{ CarAppearance.save(MainActivity.this,choice,0); preview.setImageBitmap(CarAppearance.renderSide(MainActivity.this,dp(280),dp(160))); for(int j=0;j<ids.length;j++)view.findViewById(ids[j]).setAlpha(j==choice?1f:.62f); applyCarAppearance(); }); }
    }

'''
s=s[:start]+method+s[end:]; p.write_text(s)
