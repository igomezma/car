package com.inaki.micoche;

import android.content.Context;
import android.content.SharedPreferences;

public final class GaragePrefs {
    private static final String PREFS = "garages";
    private GaragePrefs() {}
    private static SharedPreferences p(Context c){ return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public static boolean has(Context c,String key){ return p(c).getBoolean(key+"_has",false); }
    public static void save(Context c,String key,double lat,double lon,float accuracy){ p(c).edit().putBoolean(key+"_has",true).putLong(key+"_lat",Double.doubleToRawLongBits(lat)).putLong(key+"_lon",Double.doubleToRawLongBits(lon)).putFloat(key+"_acc",accuracy).apply(); }
    public static void clear(Context c,String key){ p(c).edit().remove(key+"_has").remove(key+"_lat").remove(key+"_lon").remove(key+"_acc").apply(); }
    public static double lat(Context c,String key){ return Double.longBitsToDouble(p(c).getLong(key+"_lat",0L)); }
    public static double lon(Context c,String key){ return Double.longBitsToDouble(p(c).getLong(key+"_lon",0L)); }
    public static float accuracy(Context c,String key){ return p(c).getFloat(key+"_acc",0f); }
}
