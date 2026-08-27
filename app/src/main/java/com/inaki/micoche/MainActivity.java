package com.inaki.micoche;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

public class MainActivity extends Activity {
    public static final String ACTION_SAVE_NOW = "com.inaki.micoche.SAVE_NOW";
    private static final int REQ_LOCATION = 10;

    private View rootView;
    private WebView mapWeb;
    private ImageView mapCar;
    private TextView mapTip;
    private TextView statusTitle;
    private TextView savedAgo;
    private TextView distanceText;
    private TextView addressText;
    private TextView coordsText;
    private Button navButton;
    private LocationManager locationManager;
    private boolean saveImmediately;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.bg));
        getWindow().setNavigationBarColor(getColor(R.color.bg));
        setContentView(R.layout.activity_main);

        bindViews();
        configureSafeArea();
        configureMap();
        configureActions();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        saveImmediately = ACTION_SAVE_NOW.equals(getIntent().getAction());
        refreshUi();
        refreshDistanceFromLastKnown();

        if (saveImmediately) rootView.postDelayed(this::saveCurrentLocation, 250);
    }

    private void bindViews() {
        rootView = findViewById(R.id.rootView);
        mapWeb = findViewById(R.id.mapWeb);
        mapCar = findViewById(R.id.mapCar);
        mapTip = findViewById(R.id.mapTip);
        statusTitle = findViewById(R.id.statusTitle);
        savedAgo = findViewById(R.id.savedAgo);
        distanceText = findViewById(R.id.distanceText);
        addressText = findViewById(R.id.addressText);
        coordsText = findViewById(R.id.coordsText);
        navButton = findViewById(R.id.navButton);
    }

    /**
     * Pantalla fija sin ScrollView.
     * Android 15/16 puede dibujar edge-to-edge; aquí reservamos explícitamente
     * la barra superior (hora/batería) y la barra de navegación inferior.
     */
    private void configureSafeArea() {
        final int side = dp(16);
        final int extraTop = dp(6);
        final int extraBottom = dp(6);

        rootView.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets status = insets.getInsets(WindowInsets.Type.statusBars());
                android.graphics.Insets nav = insets.getInsets(WindowInsets.Type.navigationBars());
                top = status.top;
                bottom = nav.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(side, top + extraTop, side, bottom + extraBottom);
            return insets;
        });
        rootView.requestApplyInsets();
    }

    private void configureMap() {
        WebSettings settings = mapWeb.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        mapWeb.setVerticalScrollBarEnabled(false);
        mapWeb.setHorizontalScrollBarEnabled(false);
        mapWeb.setOverScrollMode(View.OVER_SCROLL_NEVER);

        mapWeb.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) navigateToCar();
            return true;
        });
    }

    private void configureActions() {
        findViewById(R.id.saveButton).setOnClickListener(v -> saveCurrentLocation());
        navButton.setOnClickListener(v -> navigateToCar());
        findViewById(R.id.shareButton).setOnClickListener(v -> shareCar());
        findViewById(R.id.deleteButton).setOnClickListener(v -> deleteCar());
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> showSettings());
    }

    private void refreshUi() {
        boolean has = CarStorage.hasCar(this);
        mapCar.setVisibility(has ? View.VISIBLE : View.GONE);
        mapTip.setText(has ? "Toca el mapa para ir al coche" : "Guarda aquí para fijar el coche");
        navButton.setEnabled(has);
        navButton.setAlpha(has ? 1f : 0.42f);

        if (!has) {
            statusTitle.setText("Coche no guardado");
            savedAgo.setText("Pulsa Guardar aquí");
            distanceText.setText("—");
            addressText.setText("Todavía no hay una ubicación guardada");
            coordsText.setText("");
            loadMap(42.0613, -1.6045, false);
            return;
        }

        double lat = CarStorage.lat(this);
        double lon = CarStorage.lon(this);
        statusTitle.setText("Coche guardado");
        savedAgo.setText(formatAgo(CarStorage.time(this)));
        String address = CarStorage.address(this);
        addressText.setText(address == null || address.trim().isEmpty() ? "Ubicación guardada" : address);
        coordsText.setText(String.format(Locale.US, "%.6f, %.6f", lat, lon));
        loadMap(lat, lon, true);
    }

    private void loadMap(double lat, double lon, boolean carSaved) {
        double dLon = 0.0065;
        double dLat = 0.0045;
        String url = String.format(Locale.US,
                "https://www.openstreetmap.org/export/embed.html?bbox=%.6f%%2C%.6f%%2C%.6f%%2C%.6f&layer=mapnik",
                lon - dLon, lat - dLat, lon + dLon, lat + dLat);
        mapWeb.loadUrl(url);
        mapCar.setVisibility(carSaved ? View.VISIBLE : View.GONE);
    }

    private void saveCurrentLocation() {
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            return;
        }

        if (!isAnyProviderEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Activa la ubicación")
                    .setMessage("Para guardar el coche necesito que la ubicación del teléfono esté activada.")
                    .setPositiveButton("Abrir ajustes", (d, w) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        Toast.makeText(this, "Obteniendo ubicación…", Toast.LENGTH_SHORT).show();
        requestFreshLocation(location -> {
            if (location == null) {
                Toast.makeText(this, "No he podido obtener la ubicación. Inténtalo de nuevo.", Toast.LENGTH_LONG).show();
                return;
            }
            final double lat = location.getLatitude();
            final double lon = location.getLongitude();
            CarStorage.save(this, lat, lon, "Obteniendo dirección…", System.currentTimeMillis());
            refreshUi();
            updateDistance(location);
            CarWidgetProvider.updateAll(this);
            reverseGeocodeAndSave(lat, lon);
            Toast.makeText(this, "Coche guardado aquí", Toast.LENGTH_SHORT).show();
        });
    }

    private void requestFreshLocation(LocationCallback callback) {
        String provider = bestProvider();
        if (provider == null) {
            callback.onLocation(null);
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                Executor executor = getMainExecutor();
                locationManager.getCurrentLocation(provider, null, executor, callback::onLocation);
            } else {
                LocationListener listener = new LocationListener() {
                    @Override public void onLocationChanged(Location location) {
                        callback.onLocation(location);
                        try { locationManager.removeUpdates(this); } catch (SecurityException ignored) {}
                    }
                    @Override public void onProviderEnabled(String provider) {}
                    @Override public void onProviderDisabled(String provider) {}
                    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                };
                locationManager.requestSingleUpdate(provider, listener, null);
            }
        } catch (SecurityException e) {
            callback.onLocation(null);
        }
    }

    private void reverseGeocodeAndSave(double lat, double lon) {
        new Thread(() -> {
            String result = "";
            try {
                Geocoder geocoder = new Geocoder(MainActivity.this, new Locale("es", "ES"));
                List<Address> list = geocoder.getFromLocation(lat, lon, 1);
                if (list != null && !list.isEmpty()) {
                    Address a = list.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= a.getMaxAddressLineIndex(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(a.getAddressLine(i));
                    }
                    result = sb.toString();
                }
            } catch (IOException | IllegalArgumentException ignored) {}

            final String finalResult = result;
            runOnUiThread(() -> {
                String address = finalResult == null || finalResult.trim().isEmpty()
                        ? String.format(Locale.US, "%.6f, %.6f", lat, lon)
                        : finalResult;
                CarStorage.save(MainActivity.this, lat, lon, address, CarStorage.time(MainActivity.this));
                refreshUi();
                CarWidgetProvider.updateAll(MainActivity.this);
            });
        }).start();
    }

    private void refreshDistanceFromLastKnown() {
        if (!CarStorage.hasCar(this) || !hasLocationPermission()) return;
        try {
            Location best = null;
            for (String p : locationManager.getProviders(true)) {
                Location l = locationManager.getLastKnownLocation(p);
                if (l != null && (best == null || l.getAccuracy() < best.getAccuracy())) best = l;
            }
            if (best != null) updateDistance(best);
        } catch (SecurityException ignored) {}
    }

    private void updateDistance(Location current) {
        if (!CarStorage.hasCar(this) || current == null) return;
        float[] results = new float[1];
        Location.distanceBetween(current.getLatitude(), current.getLongitude(),
                CarStorage.lat(this), CarStorage.lon(this), results);
        float meters = results[0];
        if (meters < 1000) distanceText.setText(Math.round(meters) + " m");
        else distanceText.setText(String.format(Locale.US, "%.1f km", meters / 1000f));
    }

    private void navigateToCar() {
        if (!CarStorage.hasCar(this)) {
            Toast.makeText(this, "Primero guarda la ubicación del coche", Toast.LENGTH_SHORT).show();
            return;
        }
        double lat = CarStorage.lat(this);
        double lon = CarStorage.lon(this);
        Intent google = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + lat + "," + lon + "&mode=w"));
        google.setPackage("com.google.android.apps.maps");
        try {
            startActivity(google);
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + lat + "," + lon + "(Mi%20Coche)")));
        }
    }

    private void shareCar() {
        if (!CarStorage.hasCar(this)) {
            Toast.makeText(this, "Primero guarda la ubicación del coche", Toast.LENGTH_SHORT).show();
            return;
        }
        double lat = CarStorage.lat(this);
        double lon = CarStorage.lon(this);
        String address = CarStorage.address(this);
        String text = "Mi coche está aquí:\n" + (address == null ? "" : address + "\n")
                + String.format(Locale.US, "https://www.google.com/maps?q=%.6f,%.6f", lat, lon);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, "Compartir ubicación del coche"));
    }

    private void deleteCar() {
        if (!CarStorage.hasCar(this)) return;
        new AlertDialog.Builder(this)
                .setTitle("Borrar ubicación")
                .setMessage("¿Quieres borrar la ubicación guardada del coche?")
                .setPositiveButton("Borrar", (d, w) -> {
                    CarStorage.clear(this);
                    refreshUi();
                    CarWidgetProvider.updateAll(this);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showSettings() {
        new AlertDialog.Builder(this)
                .setTitle("Mi Coche · v1.2")
                .setMessage("Interfaz compacta sin desplazamiento.\n\nEl mapa se adapta al alto disponible y la aplicación respeta la barra superior del teléfono.\n\nMapa: OpenStreetMap")
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isAnyProviderEnabled() {
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    private String bestProvider() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return LocationManager.GPS_PROVIDER;
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return LocationManager.NETWORK_PROVIDER;
        } catch (Exception ignored) {}
        return null;
    }

    private String formatAgo(long time) {
        if (time <= 0) return "Ubicación guardada";
        long seconds = Math.max(0, (System.currentTimeMillis() - time) / 1000L);
        if (seconds < 45) return "Guardado ahora";
        long minutes = seconds / 60L;
        if (minutes < 60) return "Guardado hace " + minutes + " min";
        long hours = minutes / 60L;
        if (hours < 24) return "Guardado hace " + hours + " h";
        long days = hours / 24L;
        return "Guardado hace " + days + (days == 1 ? " día" : " días");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (hasLocationPermission()) saveCurrentLocation();
            else Toast.makeText(this, "Necesito permiso de ubicación para guardar el coche", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationManager != null) refreshDistanceFromLastKnown();
    }

    @Override
    protected void onDestroy() {
        if (mapWeb != null) {
            mapWeb.stopLoading();
            mapWeb.destroy();
        }
        super.onDestroy();
    }

    private interface LocationCallback {
        void onLocation(Location location);
    }
}
