# Mi Coche v1.3

Aplicación Android para guardar la ubicación del coche, verla en el mapa, navegar hasta ella, compartirla y usar un widget en la pantalla de inicio.

## Cambios principales de v1.3

- Pantalla principal completamente fija: **sin ScrollView y sin desplazamiento vertical**.
- Respeta la barra superior del sistema (hora, batería, Wi‑Fi, etc.).
- El mapa usa `layout_weight=1` y se adapta al espacio libre del teléfono.
- Interfaz más minimalista y compacta.
- Nuevo icono naranja/charcoal usado también en la cabecera y el widget.
- Botones principales en una sola fila para ahorrar altura.
- Widget conservado y actualizado visualmente.

## Compilar con GitHub Actions

Sube todo el contenido de este proyecto al repositorio `car` en la rama `main`.
El workflow `.github/workflows/build-apk.yml` compila automáticamente y crea el artefacto `MiCoche-v1.2-APK`.


### Corrección del widget v1.3
Se ha rehecho el layout del widget usando únicamente vistas compatibles con RemoteViews y se ha reforzado el manejo de los PendingIntent.
