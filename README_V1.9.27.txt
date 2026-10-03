Where Is My Car v1.9.27 / versionCode 37

Cambios principales:
- Aparcamiento automático: NUNCA reutiliza una ubicación anterior a la desconexión Bluetooth.
- Al desconectar Bluetooth solicita GPS_PROVIDER y verifica location.getTime() >= instante de desconexión.
- Si no hay GPS posterior, notificación para elegir Casa / Casa 2 / Trabajo / Hacer foto.
- Nuevo botón Parking en la pantalla principal.
- Nuevo botón Parking en el widget.
- Foto del parking almacenada localmente como parking_photo.jpg.
- Botones secundarios antracita; naranja reservado a acciones principales.

- Widget: si no hay GPS posterior, muestra «Parking sin GPS» y «Completar».
- Completar abre directamente Casa / Casa 2 / Trabajo / Hacer foto.
