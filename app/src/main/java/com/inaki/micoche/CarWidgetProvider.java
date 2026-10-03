package com.inaki.micoche;
import android.app.*;import android.appwidget.*;import android.content.*;import android.net.Uri;import android.widget.*;
public class CarWidgetProvider extends AppWidgetProvider {
 public static final String ACTION_NAVIGATE="com.inaki.micoche.widget.NAVIGATE";
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)update(c,m,id);}
 @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);if(ACTION_NAVIGATE.equals(i.getAction()))navigate(c);}
 public static void updateAll(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);ComponentName n=new ComponentName(c,CarWidgetProvider.class);for(int id:m.getAppWidgetIds(n))update(c,m,id);}
 private static void update(Context c,AppWidgetManager m,int id){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget_car); boolean has=CarStorage.hasCar(c);
  boolean pending=AutoParkingPrefs.parkingPending(c);
  v.setTextViewText(R.id.widgetStatus,pending?"Parking sin GPS":(has?(CarStorage.SOURCE_AUTO.equals(CarStorage.source(c))?c.getString(R.string.widget_saved_auto):c.getString(R.string.widget_saved)):c.getString(R.string.widget_no_location)));
  v.setTextViewText(R.id.widgetAddress,pending?"Completa dónde has aparcado":(has?CarStorage.address(c):c.getString(R.string.widget_empty_help)));
  v.setOnClickPendingIntent(R.id.widgetSave,act(c,id*10+1,MainActivity.ACTION_SAVE_NOW));
  v.setTextViewText(R.id.widgetParking,pending?"Completar":c.getString(R.string.widget_parking));
  v.setOnClickPendingIntent(R.id.widgetParking,act(c,id*10+2,pending?MainActivity.ACTION_RESOLVE_PARKING:MainActivity.ACTION_PARKING_NOW));
  Intent ni=new Intent(c,CarWidgetProvider.class).setAction(ACTION_NAVIGATE);
  v.setOnClickPendingIntent(R.id.widgetNavigate,PendingIntent.getBroadcast(c,id*10+3,ni,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));m.updateAppWidget(id,v);
 }
 private static PendingIntent act(Context c,int code,String action){Intent i=new Intent(c,MainActivity.class).setAction(action).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);return PendingIntent.getActivity(c,code,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 private static void navigate(Context c){if(!CarStorage.hasCar(c)){Toast.makeText(c,R.string.save_first,Toast.LENGTH_SHORT).show();return;}Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("google.navigation:q="+CarStorage.lat(c)+","+CarStorage.lon(c)+"&mode=w")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(i);}
}
