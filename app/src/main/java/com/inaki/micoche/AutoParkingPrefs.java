package com.inaki.micoche;

import android.content.Context;
import android.content.SharedPreferences;

public final class AutoParkingPrefs {
    private static final String PREFS = "auto_parking";
    private static final String ENABLED = "enabled";
    private static final String DEVICE_ADDRESS = "device_address";
    private static final String DEVICE_NAME = "device_name";
    private static final String SEEN_CONNECTED = "seen_connected";
    private static final String LAST_EVENT = "last_event";
    private static final String LAST_AUTO_SAVE = "last_auto_save";

    private AutoParkingPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context context) {
        return prefs(context).getBoolean(ENABLED, false);
    }

    public static String address(Context context) {
        return prefs(context).getString(DEVICE_ADDRESS, "");
    }

    public static String deviceName(Context context) {
        return prefs(context).getString(DEVICE_NAME, "");
    }

    public static boolean seenConnected(Context context) {
        return prefs(context).getBoolean(SEEN_CONNECTED, false);
    }

    public static String lastEvent(Context context) {
        return prefs(context).getString(LAST_EVENT, "Todavía sin eventos");
    }

    public static long lastAutoSave(Context context) {
        return prefs(context).getLong(LAST_AUTO_SAVE, 0L);
    }

    public static void configure(Context context, String address, String name) {
        prefs(context).edit()
                .putBoolean(ENABLED, true)
                .putString(DEVICE_ADDRESS, address == null ? "" : address)
                .putString(DEVICE_NAME, name == null ? "Bluetooth del coche" : name)
                .putBoolean(SEEN_CONNECTED, false)
                .putString(LAST_EVENT, "Bluetooth configurado. Esperando conexión con el coche.")
                .apply();
    }

    public static void setSeenConnected(Context context, boolean seen) {
        prefs(context).edit().putBoolean(SEEN_CONNECTED, seen).apply();
    }

    public static void setLastEvent(Context context, String event) {
        prefs(context).edit().putString(LAST_EVENT, event == null ? "" : event).apply();
    }

    public static void markAutoSaved(Context context) {
        prefs(context).edit().putLong(LAST_AUTO_SAVE, System.currentTimeMillis()).apply();
    }

    public static boolean recentlyAutoSaved(Context context) {
        long last = lastAutoSave(context);
        return last > 0 && System.currentTimeMillis() - last < 90_000L;
    }

    public static void disable(Context context) {
        prefs(context).edit()
                .putBoolean(ENABLED, false)
                .putBoolean(SEEN_CONNECTED, false)
                .putString(LAST_EVENT, "Aparcamiento automático desactivado")
                .apply();
    }

    public static void clearDevice(Context context) {
        prefs(context).edit().clear().apply();
    }
}
