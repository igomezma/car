package com.inaki.micoche;

import android.companion.CompanionDeviceService;

public class AutoParkingCompanionService extends CompanionDeviceService {

    @Override
    @SuppressWarnings("deprecation")
    public void onDeviceAppeared(String address) {
        if (!matchesSelectedCar(address)) return;

        AutoParkingPrefs.setSeenConnected(this, true);
        AutoParkingPrefs.setLastEvent(
                this,
                "Bluetooth del coche conectado. Esperando a que aparques.");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onDeviceDisappeared(String address) {
        if (!matchesSelectedCar(address)) return;
        if (!AutoParkingPrefs.enabled(this)) return;

        // Evita guardar una posición simplemente por configurar un dispositivo
        // que todavía no se había conectado al teléfono desde la configuración.
        if (!AutoParkingPrefs.seenConnected(this)) {
            AutoParkingPrefs.setLastEvent(
                    this,
                    "Bluetooth no disponible, pero todavía no se había detectado una conexión.");
            return;
        }

        AutoParkingPrefs.setSeenConnected(this, false);
        AutoParkingPrefs.setLastEvent(
                this,
                "Bluetooth desconectado. Obteniendo ubicación…");

        AutoParkingManager.saveAutomaticParking(this, false);
    }

    private boolean matchesSelectedCar(String address) {
        if (address == null) return false;
        String selected = AutoParkingPrefs.address(this);
        return selected != null && selected.equalsIgnoreCase(address);
    }
}
