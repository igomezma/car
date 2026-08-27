package com.inaki.micoche;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

public class CarWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_NAVIGATE =
            "com.inaki.micoche.widget.NAVIGATE";

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        updateAll(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateOne(context, manager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_NAVIGATE.equals(intent.getAction())) {
            navigate(context);
        }
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, CarWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(provider);

        for (int id : ids) {
            updateOne(context, manager, id);
        }
    }

    private static void updateOne(
            Context context,
            AppWidgetManager manager,
            int appWidgetId) {

        RemoteViews views =
                new RemoteViews(context.getPackageName(), R.layout.widget_car);

        boolean hasCar = CarStorage.hasCar(context);

        if (hasCar) {
            String address = CarStorage.address(context);

            if (address == null || address.trim().isEmpty()) {
                address = String.format(
                        java.util.Locale.US,
                        "%.6f, %.6f",
                        CarStorage.lat(context),
                        CarStorage.lon(context));
            }

            views.setTextViewText(R.id.widgetStatus, "Coche guardado");
            views.setTextViewText(R.id.widgetAddress, address);
            views.setFloat(R.id.widgetNavigate, "setAlpha", 1f);

        } else {
            views.setTextViewText(R.id.widgetStatus, "Sin ubicación guardada");
            views.setTextViewText(
                    R.id.widgetAddress,
                    "Pulsa Guardar para registrar dónde está el coche");
            views.setFloat(R.id.widgetNavigate, "setAlpha", 0.45f);
        }

        // Abrir la aplicación tocando cabecera, dirección o fondo.
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent openPending = PendingIntent.getActivity(
                context,
                appWidgetId * 100,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.widgetRoot, openPending);
        views.setOnClickPendingIntent(R.id.widgetHeader, openPending);
        views.setOnClickPendingIntent(R.id.widgetAddress, openPending);

        // Guardar coche: abre MainActivity indicando que debe guardar inmediatamente.
        Intent saveIntent = new Intent(context, MainActivity.class);
        saveIntent.setAction(MainActivity.ACTION_SAVE_NOW);
        saveIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent savePending = PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 1,
                saveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.widgetSave, savePending);

        // Ir al coche: broadcast al AppWidgetProvider.
        Intent navIntent = new Intent(context, CarWidgetProvider.class);
        navIntent.setAction(ACTION_NAVIGATE);
        navIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        PendingIntent navPending = PendingIntent.getBroadcast(
                context,
                appWidgetId * 100 + 2,
                navIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.widgetNavigate, navPending);

        manager.updateAppWidget(appWidgetId, views);
    }

    private static void navigate(Context context) {
        if (!CarStorage.hasCar(context)) {
            Toast.makeText(
                    context,
                    "Primero guarda la ubicación del coche",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        double lat = CarStorage.lat(context);
        double lon = CarStorage.lon(context);

        Intent googleMaps = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=" + lat + "," + lon + "&mode=w"));

        googleMaps.setPackage("com.google.android.apps.maps");
        googleMaps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(googleMaps);
        } catch (Exception e) {
            Intent fallback = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=" + lat + "," + lon + "(Mi%20Coche)"));

            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(fallback);
        }
    }
}
