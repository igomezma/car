# Mi Coche 1.1

Aplicación Android para guardar la ubicación del coche, navegar hasta él y compartir su posición.

## Cambios de esta versión

- Corrección de la parte superior: el encabezado respeta la barra de estado de Android y deja 12 dp extra de separación.
- Diseño oscuro con naranja Atacama.
- Mapa OpenStreetMap sin clave API.
- Guardado de ubicación GPS y dirección aproximada.
- Distancia al coche.
- Botón para abrir navegación.
- Compartir ubicación.
- Borrar ubicación.
- Widget funcional con Guardar e Ir al coche.
- Compilación automática gratuita con GitHub Actions.

## Compilar gratis en GitHub

1. Sube el contenido de esta carpeta a la raíz del repositorio.
2. En GitHub abre la pestaña **Actions**.
3. Entra en **Compilar APK Mi Coche**.
4. Pulsa **Run workflow**.
5. Cuando termine, abre la ejecución y baja el artefacto **MiCoche-APK**.
6. Dentro del ZIP está `app-debug.apk`, listo para instalar en Android.

También se compila automáticamente cada vez que haces un cambio en `main` o `master`.
