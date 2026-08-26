package com.inaki.micoche;

import android.content.Context;
import android.content.SharedPreferences;

public final class CarStorage {
    private static final String PREFS = "mi_coche";
    private static final String LAT = "lat";
    private static final String LON = "lon";
    private static final String ADDRESS = "address";
    private static final String TIME = "time";
    private static final String HAS = "has";

    private CarStorage() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean hasCar(Context context) {
        return prefs(context).getBoolean(HAS, false);
    }

    public static void save(Context context, double lat, double lon, String address, long time) {
        prefs(context).edit()
                .putBoolean(HAS, true)
                .putLong(LAT, Double.doubleToRawLongBits(lat))
                .putLong(LON, Double.doubleToRawLongBits(lon))
                .putString(ADDRESS, address == null ? "" : address)
                .putLong(TIME, time)
                .apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    public static double lat(Context context) {
        return Double.longBitsToDouble(prefs(context).getLong(LAT, Double.doubleToRawLongBits(0.0)));
    }

    public static double lon(Context context) {
        return Double.longBitsToDouble(prefs(context).getLong(LON, Double.doubleToRawLongBits(0.0)));
    }

    public static String address(Context context) {
        return prefs(context).getString(ADDRESS, "");
    }

    public static long time(Context context) {
        return prefs(context).getLong(TIME, 0L);
    }
}
