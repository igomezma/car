package com.inaki.micoche;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceService;
import android.companion.DevicePresenceEvent;
import android.os.Build;

public class AutoParkingCompanionService extends CompanionDeviceService {
    private void connected() {
        AutoParkingPrefs.setSeenConnected(this, true);
        AutoParkingPrefs.setLastEvent(this, "Bluetooth del coche conectado. Esperando a que aparques.");
    }
    private void disconnected() {
        if (!AutoParkingPrefs.enabled(this) || !AutoParkingPrefs.seenConnected(this)) return;
        AutoParkingPrefs.setSeenConnected(this, false);
        AutoParkingPrefs.setLastEvent(this, "Bluetooth desconectado. Esperando ubicación nueva…");
        AutoParkingPrefs.setWaitingForFreshLocation(this, true);
        AutoParkingManager.saveAutomaticParking(this, false, System.currentTimeMillis());
    }
    @Override public void onDevicePresenceEvent(DevicePresenceEvent event) {
        if (Build.VERSION.SDK_INT < 36) return;
        int e=event.getEvent();
        if (e==DevicePresenceEvent.EVENT_BT_CONNECTED || e==DevicePresenceEvent.EVENT_BLE_APPEARED) connected();
        else if (e==DevicePresenceEvent.EVENT_BT_DISCONNECTED || e==DevicePresenceEvent.EVENT_BLE_DISAPPEARED) disconnected();
    }
    @Override public void onDeviceAppeared(AssociationInfo info) { if (Build.VERSION.SDK_INT < 36) connected(); }
    @Override public void onDeviceDisappeared(AssociationInfo info) { if (Build.VERSION.SDK_INT < 36) disconnected(); }
    @Override @SuppressWarnings("deprecation") public void onDeviceAppeared(String address) { if (Build.VERSION.SDK_INT < 33 && matches(address)) connected(); }
    @Override @SuppressWarnings("deprecation") public void onDeviceDisappeared(String address) { if (Build.VERSION.SDK_INT < 33 && matches(address)) disconnected(); }
    private boolean matches(String address) {
        String selected=AutoParkingPrefs.address(this);
        return address!=null && selected!=null && selected.equalsIgnoreCase(address);
    }
}
