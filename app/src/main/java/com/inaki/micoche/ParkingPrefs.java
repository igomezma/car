package com.inaki.micoche;

import android.content.Context;

public final class ParkingPrefs {
    private static final String PREFS = "parking_pending";
    private ParkingPrefs() {}
    public static void begin(Context c, String photoUri, long time) { c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("pending", true).putString("photo", photoUri == null ? "" : photoUri).putLong("time", time).apply(); }
    public static boolean pending(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("pending", false); }
    public static long time(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("time", 0L); }
    public static String photo(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("photo", ""); }
    public static void clear(Context c) { c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply(); }
}
