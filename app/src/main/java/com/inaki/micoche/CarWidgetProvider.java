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
    public static final String ACTION_NAVIGATE = "com.inaki.micoche.widget.NAVIGATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) updateOne(context, manager, appWidgetId);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_NAVIGATE.equals(intent.getAction())) navigate(context);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName name = new ComponentName(context, CarWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(name);
        for (int id : ids) updateOne(context, manager, id);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_car);
        boolean has = CarStorage.hasCar(context);

        if (has) {
            String address = CarStorage.address(context);
            if (address == null || address.trim().isEmpty()) {
                address = String.format(java.util.Locale.US, "%.6f, %.6f", CarStorage.lat(context), CarStorage.lon(context));
            }
            views.setTextViewText(R.id.widgetStatus, "Coche guardado");
            views.setTextViewText(R.id.widgetAddress, address);
        } else {
            views.setTextViewText(R.id.widgetStatus, "Sin ubicación guardada");
            views.setTextViewText(R.id.widgetAddress, "Toca Guardar para registrar dónde está el coche");
        }

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(context, appWidgetId * 10, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetAddress, openPending);

        Intent save = new Intent(context, MainActivity.class);
        save.setAction(MainActivity.ACTION_SAVE_NOW);
        PendingIntent savePending = PendingIntent.getActivity(context, appWidgetId * 10 + 1, save,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetSave, savePending);

        Intent nav = new Intent(context, CarWidgetProvider.class);
        nav.setAction(ACTION_NAVIGATE);
        PendingIntent navPending = PendingIntent.getBroadcast(context, appWidgetId * 10 + 2, nav,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetNavigate, navPending);

        manager.updateAppWidget(appWidgetId, views);
    }

    private static void navigate(Context context) {
        if (!CarStorage.hasCar(context)) {
            Toast.makeText(context, "Primero guarda la ubicación del coche", Toast.LENGTH_SHORT).show();
            return;
        }
        double lat = CarStorage.lat(context);
        double lon = CarStorage.lon(context);
        Uri googleNav = Uri.parse("google.navigation:q=" + lat + "," + lon + "&mode=w");
        Intent intent = new Intent(Intent.ACTION_VIEW, googleNav);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.google.android.apps.maps");
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Intent fallback = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=" + lat + "," + lon + "(Mi%20Coche)"));
            fallback.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(fallback);
        }
    }
}
