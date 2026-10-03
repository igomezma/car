package com.inaki.micoche;
import android.companion.CompanionDeviceService;
public class AutoParkingCompanionService extends CompanionDeviceService {
 @Override @SuppressWarnings("deprecation") public void onDeviceAppeared(String a){
   if(match(a)){AutoParkingPrefs.setSeenConnected(this,true);AutoParkingPrefs.setLastEvent(this,"Bluetooth conectado. Esperando aparcamiento.");}
 }
 @Override @SuppressWarnings("deprecation") public void onDeviceDisappeared(String a){
   if(!match(a)||!AutoParkingPrefs.enabled(this)||!AutoParkingPrefs.seenConnected(this))return;
   AutoParkingPrefs.setSeenConnected(this,false);
   long disconnected=System.currentTimeMillis();
   AutoParkingPrefs.markDisconnect(this,disconnected);
   AutoParkingPrefs.setLastEvent(this,"Bluetooth desconectado. Buscando GPS NUEVO…");
   AutoParkingManager.saveAfterDisconnect(this,disconnected);
 }
 private boolean match(String a){return a!=null&&a.equalsIgnoreCase(AutoParkingPrefs.address(this));}
}
