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
    private static final String SOURCE = "source";
    private static final String APPROX = "approx";
    private static final String PARKING_NAME = "parking_name";
    private static final String PHOTO = "parking_photo";

    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_AUTO = "auto";
    public static final String SOURCE_GARAGE = "garage";

    private CarStorage() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean hasCar(Context context) {
        return prefs(context).getBoolean(HAS, false);
    }

    public static void save(Context context, double lat, double lon, String address, long time) {
        save(context, lat, lon, address, time, SOURCE_MANUAL);
    }

    public static void save(Context context, double lat, double lon, String address, long time, String source) {
        prefs(context).edit()
                .putBoolean(HAS, true)
                .putLong(LAT, Double.doubleToRawLongBits(lat))
                .putLong(LON, Double.doubleToRawLongBits(lon))
                .putString(ADDRESS, address == null ? "" : address)
                .putLong(TIME, time)
                .putString(SOURCE, source == null ? SOURCE_MANUAL : source)
                .putBoolean(APPROX, false)
                .putString(PARKING_NAME, "")
                .putString(PHOTO, "")
                .apply();
    }

    public static void saveParking(Context context, double lat, double lon, String address, long time, String parkingName, boolean approximate, String photoBase64) {
        prefs(context).edit()
                .putBoolean(HAS, true)
                .putLong(LAT, Double.doubleToRawLongBits(lat))
                .putLong(LON, Double.doubleToRawLongBits(lon))
                .putString(ADDRESS, address == null ? parkingName : address)
                .putLong(TIME, time)
                .putString(SOURCE, SOURCE_GARAGE)
                .putBoolean(APPROX, approximate)
                .putString(PARKING_NAME, parkingName == null ? "Parking" : parkingName)
                .putString(PHOTO, photoBase64 == null ? "" : photoBase64)
                .apply();
    }

    public static boolean approximate(Context context) { return prefs(context).getBoolean(APPROX, false); }
    public static String parkingName(Context context) { return prefs(context).getString(PARKING_NAME, ""); }
    public static String photo(Context context) { return prefs(context).getString(PHOTO, ""); }
    public static void markPrecise(Context context, double lat, double lon, String address) {
        prefs(context).edit()
                .putLong(LAT, Double.doubleToRawLongBits(lat))
                .putLong(LON, Double.doubleToRawLongBits(lon))
                .putString(ADDRESS, address == null ? "" : address)
                .putBoolean(APPROX, false).apply();
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

    public static String source(Context context) {
        return prefs(context).getString(SOURCE, SOURCE_MANUAL);
    }
}
