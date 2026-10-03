package com.inaki.micoche;
import android.content.*;
public final class GaragePrefs {
 private static SharedPreferences p(Context c){return c.getSharedPreferences("garages",Context.MODE_PRIVATE);}
 public static boolean has(Context c,String k){return p(c).getBoolean(k+"_has",false);}
 public static void save(Context c,String k,double lat,double lon,float acc){p(c).edit().putBoolean(k+"_has",true).putLong(k+"_lat",Double.doubleToRawLongBits(lat)).putLong(k+"_lon",Double.doubleToRawLongBits(lon)).putFloat(k+"_acc",acc).apply();}
 public static double lat(Context c,String k){return Double.longBitsToDouble(p(c).getLong(k+"_lat",0));}
 public static double lon(Context c,String k){return Double.longBitsToDouble(p(c).getLong(k+"_lon",0));}
}
