package com.inaki.micoche;
import android.content.*;
public final class CarStorage {
 private static final String P="mi_coche";
 public static final String SOURCE_MANUAL="manual",SOURCE_AUTO="auto",SOURCE_GARAGE="garage";
 private static SharedPreferences p(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}
 public static void save(Context c,double lat,double lon,String a,long t,String s){p(c).edit().putBoolean("has",true).putLong("lat",Double.doubleToRawLongBits(lat)).putLong("lon",Double.doubleToRawLongBits(lon)).putString("address",a==null?"":a).putLong("time",t).putString("source",s).apply();}
 public static boolean hasCar(Context c){return p(c).getBoolean("has",false);}
 public static double lat(Context c){return Double.longBitsToDouble(p(c).getLong("lat",0));}
 public static double lon(Context c){return Double.longBitsToDouble(p(c).getLong("lon",0));}
 public static String address(Context c){return p(c).getString("address","");}
 public static long time(Context c){return p(c).getLong("time",0);}
 public static String source(Context c){return p(c).getString("source",SOURCE_MANUAL);}
 public static void clear(Context c){p(c).edit().clear().apply();}
}
