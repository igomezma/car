# Mi Coche v1.4 beta — Aparcamiento automático por Bluetooth

Esta versión conserva la app v1.3 y añade una primera prueba de aparcamiento automático.

## Qué hace

1. En **Ajustes > Bluetooth de mi coche**, muestra los dispositivos Bluetooth que ya están emparejados en el teléfono.
2. Seleccionas el Bluetooth del coche.
3. Android lo asocia como dispositivo compañero de Mi Coche.
4. Cuando Android detecta que ese Bluetooth se ha conectado, Mi Coche queda "armado".
5. Cuando después se desconecta, intenta guardar automáticamente la ubicación del teléfono.
6. Actualiza la ficha y el widget, y muestra una notificación si están permitidas.

## Tracker Bluetooth

La opción aparece en Ajustes como **Tracker Bluetooth · Próximamente**, pero todavía no tiene lógica programada, tal como se pidió para esta prueba.

## Permiso importante

Para guardar la posición cuando la aplicación está cerrada, en Android moderno debes ir a:

**Ajustes del teléfono > Apps > Mi Coche > Permisos > Ubicación > Permitir siempre**

La aplicación incluye un botón que te lleva a los permisos.

## Primera prueba recomendada

- Empareja normalmente el teléfono con el Bluetooth del coche.
- Abre Mi Coche > Ajustes > Bluetooth de mi coche.
- Elige tu coche y acepta la asociación.
- Activa **Permitir siempre** para ubicación.
- Permite notificaciones.
- Pulsa **Probar guardado automático** para verificar GPS/permisos.
- Luego haz la prueba real:
  - entra al coche y deja que el Bluetooth se conecte;
  - conduce/aparca;
  - apaga el coche;
  - al desconectarse el Bluetooth debería guardarse la posición.

## Seguridad contra falsos guardados

La app no guarda automáticamente por una simple ausencia del Bluetooth recién configurado. Primero tiene que haber detectado una conexión real del dispositivo y después una desconexión.

También evita repetir guardados automáticos durante 90 segundos.
