package com.inaki.micoche;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AutoParkingManager {

    public static final String CHANNEL_ID = "auto_parking";
    private static final int NOTIFICATION_ID = 1401;

    private AutoParkingManager() {}

    public static boolean supported(Context context) {
        return Build.VERSION.SDK_INT >= 31
                && context.getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_COMPANION_DEVICE_SETUP);
    }

    @SuppressWarnings("deprecation")
    public static void ensureObservation(Context context) {
        if (!supported(context) || !AutoParkingPrefs.enabled(context)) return;

        String address = AutoParkingPrefs.address(context);
        if (address == null || address.trim().isEmpty()) return;

        try {
            CompanionDeviceManager manager =
                    (CompanionDeviceManager) context.getSystemService(Context.COMPANION_DEVICE_SERVICE);
            manager.startObservingDevicePresence(address);
        } catch (Exception ignored) {
            // Si ya se estaba observando, o la asociación todavía no está lista,
            // no rompemos la app. Al abrir Ajustes se mostrará el estado.
        }
    }

    @SuppressWarnings("deprecation")
    public static void stopObservation(Context context) {
        if (Build.VERSION.SDK_INT < 31) return;

        String address = AutoParkingPrefs.address(context);
        if (address == null || address.trim().isEmpty()) return;

        try {
            CompanionDeviceManager manager =
                    (CompanionDeviceManager) context.getSystemService(Context.COMPANION_DEVICE_SERVICE);
            manager.stopObservingDevicePresence(address);
        } catch (Exception ignored) {}
    }

    public static boolean hasForegroundLocation(Context context) {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasBackgroundLocation(Context context) {
        if (Build.VERSION.SDK_INT < 29) return hasForegroundLocation(context);
        return context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void saveAutomaticParking(Context context, boolean testMode) {
        if (!testMode && !AutoParkingPrefs.enabled(context)) return;

        if (!hasForegroundLocation(context) || !hasBackgroundLocation(context)) {
            AutoParkingPrefs.setLastEvent(context,
                    "No se guardó: falta permitir ubicación todo el tiempo.");
            notifyStatus(context,
                    "No pude guardar el coche",
                    "Activa «Permitir siempre» para la ubicación de Mi Coche.");
            return;
        }

        if (!testMode && AutoParkingPrefs.recentlyAutoSaved(context)) {
            return;
        }

        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        Location best = bestRecentLocation(locationManager);
        if (best != null) {
            saveLocation(context, best, testMode);
            return;
        }

        String provider = bestProvider(locationManager);
        if (provider == null) {
            AutoParkingPrefs.setLastEvent(context,
                    "No se guardó: la ubicación del teléfono estaba desactivada.");
            notifyStatus(context,
                    "No pude guardar el coche",
                    "La ubicación del teléfono estaba desactivada.");
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= 30) {
                locationManager.getCurrentLocation(
                        provider,
                        null,
                        context.getMainExecutor(),
                        location -> {
                            if (location != null) {
                                saveLocation(context, location, testMode);
                            } else {
                                saveFallbackOrFail(context, locationManager, testMode);
                            }
                        });
            } else {
                saveFallbackOrFail(context, locationManager, testMode);
            }
        } catch (SecurityException e) {
            AutoParkingPrefs.setLastEvent(context,
                    "No se guardó: Android bloqueó el acceso a la ubicación.");
        }
    }

    private static Location bestRecentLocation(LocationManager manager) {
        try {
            Location best = null;
            for (String provider : manager.getProviders(true)) {
                Location l = manager.getLastKnownLocation(provider);
                if (l == null) continue;

                long age = Math.abs(System.currentTimeMillis() - l.getTime());
                if (age > 120_000L) continue;
                if (l.hasAccuracy() && l.getAccuracy() > 120f) continue;

                if (best == null
                        || (!best.hasAccuracy() && l.hasAccuracy())
                        || (l.hasAccuracy() && best.hasAccuracy()
                        && l.getAccuracy() < best.getAccuracy())) {
                    best = l;
                }
            }
            return best;
        } catch (SecurityException e) {
            return null;
        }
    }

    private static void saveFallbackOrFail(
            Context context,
            LocationManager manager,
            boolean testMode) {

        Location fallback = bestRecentLocation(manager);
        if (fallback != null) {
            saveLocation(context, fallback, testMode);
        } else {
            AutoParkingPrefs.setLastEvent(context,
                    "No se guardó: no había una posición reciente disponible.");
            notifyStatus(context,
                    "No pude guardar el coche",
                    "No había una posición GPS reciente. Puedes usar «Guardar aquí».");
        }
    }

    private static String bestProvider(LocationManager manager) {
        try {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                return LocationManager.GPS_PROVIDER;
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                return LocationManager.NETWORK_PROVIDER;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void saveLocation(Context context, Location location, boolean testMode) {
        final double lat = location.getLatitude();
        final double lon = location.getLongitude();
        final long time = System.currentTimeMillis();

        String initialAddress = String.format(
                Locale.US, "%.6f, %.6f", lat, lon);

        CarStorage.save(
                context,
                lat,
                lon,
                initialAddress,
                time,
                CarStorage.SOURCE_AUTO);

        AutoParkingPrefs.markAutoSaved(context);
        AutoParkingPrefs.setLastEvent(
                context,
                testMode
                        ? "Prueba correcta: ubicación guardada automáticamente."
                        : "Bluetooth desconectado: coche guardado automáticamente.");

        CarWidgetProvider.updateAll(context);

        notifyStatus(
                context,
                testMode ? "Prueba de aparcamiento automático" : "Coche guardado automáticamente",
                testMode
                        ? "La posición actual se ha guardado correctamente."
                        : "He guardado la posición al desconectarse el Bluetooth del coche.");

        reverseGeocode(context.getApplicationContext(), lat, lon, time);
    }

    private static void reverseGeocode(Context context, double lat, double lon, long time) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, new Locale("es", "ES"));
                List<Address> list = geocoder.getFromLocation(lat, lon, 1);

                if (list != null && !list.isEmpty()) {
                    Address a = list.get(0);
                    StringBuilder sb = new StringBuilder();

                    for (int i = 0; i <= a.getMaxAddressLineIndex(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(a.getAddressLine(i));
                    }

                    String address = sb.toString().trim();
                    if (!address.isEmpty()) {
                        CarStorage.save(
                                context,
                                lat,
                                lon,
                                address,
                                time,
                                CarStorage.SOURCE_AUTO);
                        CarWidgetProvider.updateAll(context);
                    }
                }
            } catch (IOException | IllegalArgumentException ignored) {
            } finally {
                executor.shutdown();
            }
        });
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.auto_channel),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.auto_channel_desc));
            manager.createNotificationChannel(channel);
        }
    }

    private static void notifyStatus(Context context, String title, String text) {
        createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1401,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification.Builder builder =
                Build.VERSION.SDK_INT >= 26
                        ? new android.app.Notification.Builder(context, CHANNEL_ID)
                        : new android.app.Notification.Builder(context);

        builder.setSmallIcon(R.drawable.ic_car_top)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
