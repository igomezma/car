package com.inaki.micoche;
import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;
import android.location.*;import android.os.*;import java.util.*;import java.io.IOException;import java.util.concurrent.*;
public final class AutoParkingManager {
 public static final String CHANNEL_ID="auto_parking"; private static final int N=1401;
 public static boolean supported(Context c){return Build.VERSION.SDK_INT>=31&&c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP);}
 @SuppressWarnings("deprecation") public static void ensureObservation(Context c){
  if(!supported(c)||!AutoParkingPrefs.enabled(c)||AutoParkingPrefs.address(c).isEmpty())return;
  try{((android.companion.CompanionDeviceManager)c.getSystemService(Context.COMPANION_DEVICE_SERVICE)).startObservingDevicePresence(AutoParkingPrefs.address(c));}catch(Exception ignored){}
 }
 public static void saveAfterDisconnect(Context c,long disconnectedAt){
  if(c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){fallback(c);return;}
  LocationManager lm=(LocationManager)c.getSystemService(Context.LOCATION_SERVICE);
  try{
   if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){fallback(c);return;}
   if(Build.VERSION.SDK_INT>=30){
    lm.getCurrentLocation(LocationManager.GPS_PROVIDER,null,c.getMainExecutor(),l->{
      // Regla v1.9.27: jamás aceptar una ubicación anterior a la desconexión.
      if(l!=null && l.getTime()>=disconnectedAt) save(c,l); else fallback(c);
    });
   }else fallback(c);
  }catch(Exception e){fallback(c);}
 }
 private static void save(Context c,Location l){
  double lat=l.getLatitude(),lon=l.getLongitude(); long t=System.currentTimeMillis();
  CarStorage.save(c,lat,lon,String.format(Locale.US,"%.6f, %.6f",lat,lon),t,CarStorage.SOURCE_AUTO); AutoParkingPrefs.setParkingPending(c,false);
  AutoParkingPrefs.setLastEvent(c,"Bluetooth desconectado: GPS posterior obtenido y coche guardado.");
  CarWidgetProvider.updateAll(c); notify(c,"Coche guardado","GPS obtenido después de desconectar el Bluetooth.");
  Executors.newSingleThreadExecutor().execute(()->{try{
    List<Address> a=new Geocoder(c,new Locale("es","ES")).getFromLocation(lat,lon,1);
    if(a!=null&&!a.isEmpty()){String s=a.get(0).getAddressLine(0);if(s!=null&&!s.isEmpty()){CarStorage.save(c,lat,lon,s,t,CarStorage.SOURCE_AUTO);CarWidgetProvider.updateAll(c);}}
  }catch(Exception ignored){}});
 }
 private static void fallback(Context c){
  AutoParkingPrefs.setParkingPending(c,true); AutoParkingPrefs.setLastEvent(c,"Sin GPS posterior a la desconexión. Pendiente de resolver parking."); CarWidgetProvider.updateAll(c);
  Intent i=new Intent(c,MainActivity.class).setAction(MainActivity.ACTION_RESOLVE_PARKING);
  PendingIntent pi=PendingIntent.getActivity(c,1402,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  createNotificationChannel(c);
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL_ID):new Notification.Builder(c);
  b.setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("Sin GPS al aparcar")
   .setContentText("Toca para elegir Casa, Casa 2, Trabajo o hacer una foto.").setContentIntent(pi).setAutoCancel(true);
  ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(N,b.build());
 }
 private static void notify(Context c,String title,String txt){createNotificationChannel(c);if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL_ID):new Notification.Builder(c);
  b.setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle(title).setContentText(txt).setAutoCancel(true);
  ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(N,b.build());
 }
 public static void createNotificationChannel(Context c){if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL_ID,c.getString(R.string.auto_channel),NotificationManager.IMPORTANCE_DEFAULT);((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);}}
}
