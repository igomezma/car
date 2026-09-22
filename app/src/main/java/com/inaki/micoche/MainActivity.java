package com.inaki.micoche;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Intent;
import android.content.IntentSender;
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
import android.graphics.drawable.ColorDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;

public class MainActivity extends Activity {
    public static final String ACTION_SAVE_NOW = "com.inaki.micoche.SAVE_NOW";

    private static final int REQ_LOCATION = 10;
    private static final int REQ_BT_CONNECT = 20;
    private static final int REQ_ASSOCIATE_CAR = 21;
    private static final int REQ_NOTIFICATIONS = 22;

    private View rootView;
    private WebView mapWeb;
    private ImageView mapCar;
    private ImageView carCardIcon;
    private TextView mapTip;
    private TextView statusTitle;
    private TextView savedAgo;
    private TextView distanceText;
    private TextView addressText;
    private TextView coordsText;
    private Button navButton;
    private LocationManager locationManager;

    private boolean pendingChooseCarAfterBluetoothPermission = false;
    private boolean pendingManualSaveAfterLocationPermission = false;
    private boolean pendingAutoPermissionGuide = false;
    private View currentSettingsView;
    private AlertDialog currentSettingsDialog;
    private float settingsGestureStartX;

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
        AutoParkingManager.createNotificationChannel(this);
        AutoParkingManager.ensureObservation(this);

        refreshUi();
        refreshDistanceFromLastKnown();

        if (ACTION_SAVE_NOW.equals(getIntent().getAction())) {
            rootView.postDelayed(this::saveCurrentLocation, 250);
        }
    }

    private void bindViews() {
        rootView = findViewById(R.id.rootView);
        mapWeb = findViewById(R.id.mapWeb);
        mapCar = findViewById(R.id.mapCar);
        carCardIcon = findViewById(R.id.carCardIcon);
        mapTip = findViewById(R.id.mapTip);
        statusTitle = findViewById(R.id.statusTitle);
        savedAgo = findViewById(R.id.savedAgo);
        distanceText = findViewById(R.id.distanceText);
        addressText = findViewById(R.id.addressText);
        coordsText = findViewById(R.id.coordsText);
        navButton = findViewById(R.id.navButton);
        applyCarAppearance();
    }

    private void configureSafeArea() {
        final int side = dp(16);
        final int extraTop = dp(6);
        final int extraBottom = dp(6);

        rootView.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;

            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets status =
                        insets.getInsets(WindowInsets.Type.statusBars());
                android.graphics.Insets nav =
                        insets.getInsets(WindowInsets.Type.navigationBars());
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
        mapTip.setText(has
                ? "Toca el mapa para ir al coche"
                : "Guarda aquí para fijar el coche");

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

        String ago = formatAgo(CarStorage.time(this));
        if (CarStorage.SOURCE_AUTO.equals(CarStorage.source(this))) {
            savedAgo.setText("Automático · " + ago);
        } else {
            savedAgo.setText(ago);
        }

        String address = CarStorage.address(this);
        addressText.setText(
                address == null || address.trim().isEmpty()
                        ? "Ubicación guardada"
                        : address);

        coordsText.setText(
                String.format(Locale.US, "%.6f, %.6f", lat, lon));

        loadMap(lat, lon, true);
    }

    private void loadMap(double lat, double lon, boolean carSaved) {
        double dLon = 0.0065;
        double dLat = 0.0045;

        String url = String.format(
                Locale.US,
                "https://www.openstreetmap.org/export/embed.html?bbox=%.6f%%2C%.6f%%2C%.6f%%2C%.6f&layer=mapnik",
                lon - dLon,
                lat - dLat,
                lon + dLon,
                lat + dLat);

        mapWeb.loadUrl(url);
        mapCar.setVisibility(carSaved ? View.VISIBLE : View.GONE);
    }

    // -------------------------------------------------------------------------
    // APARCAMIENTO AUTOMÁTICO
    // -------------------------------------------------------------------------

    private void showSettings() {
        View view = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        currentSettingsView = view;

        updateSettingsView(view);
        configureCarAppearance(view);

        Button carBluetooth = view.findViewById(R.id.configureCarBluetoothButton);
        Button permissions = view.findViewById(R.id.autoPermissionsButton);
        Button test = view.findViewById(R.id.testAutoParkingButton);
        Button disable = view.findViewById(R.id.disableAutoParkingButton);

        carBluetooth.setOnClickListener(v -> configureCarBluetooth());

        permissions.setOnClickListener(v -> showAutomaticPermissionGuide());

        test.setOnClickListener(v -> testAutomaticParking());

        disable.setOnClickListener(v -> {
            AutoParkingManager.stopObservation(this);
            AutoParkingPrefs.disable(this);
            updateSettingsView(view);
            Toast.makeText(
                    this,
                    "Aparcamiento automático desactivado",
                    Toast.LENGTH_SHORT).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Material_NoActionBar)
                .setView(view)
                .create();
        currentSettingsDialog = dialog;

        view.findViewById(R.id.settingsBackButton).setOnClickListener(v -> dialog.dismiss());
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                settingsGestureStartX = event.getX();
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    && settingsGestureStartX < dp(36)
                    && event.getX() - settingsGestureStartX > dp(90)) {
                dialog.dismiss();
            }
            return false;
        });

        dialog.setOnDismissListener(d -> {
            currentSettingsView = null;
            currentSettingsDialog = null;
        });
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getColor(R.color.bg)));
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private void configureCarAppearance(View view) {
        ImageView preview = view.findViewById(R.id.carPreview);
        Spinner model = view.findViewById(R.id.carModelSpinner);
        Spinner color = view.findViewById(R.id.carColorSpinner);
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, getResources().getStringArray(R.array.car_models));
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, getResources().getStringArray(R.array.car_colors));
        modelAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        colorAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        model.setAdapter(modelAdapter);
        color.setAdapter(colorAdapter);
        model.setSelection(CarAppearance.model(this));
        color.setSelection(CarAppearance.color(this));
        preview.setImageBitmap(CarAppearance.render(this, dp(105), dp(132)));

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View item, int position, long id) {
                CarAppearance.save(MainActivity.this, model.getSelectedItemPosition(), color.getSelectedItemPosition());
                preview.setImageBitmap(CarAppearance.render(MainActivity.this, dp(105), dp(132)));
                applyCarAppearance();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        model.setOnItemSelectedListener(listener);
        color.setOnItemSelectedListener(listener);
    }

    private void applyCarAppearance() {
        if (mapCar != null) mapCar.setImageBitmap(CarAppearance.render(this, dp(84), dp(136)));
        if (carCardIcon != null) carCardIcon.setImageBitmap(CarAppearance.render(this, dp(56), dp(92)));
    }

    private void updateSettingsView(View view) {
        if (view == null) return;

        TextView status = view.findViewById(R.id.autoStatus);
        TextView device = view.findViewById(R.id.autoDevice);
        TextView permission = view.findViewById(R.id.autoPermission);
        TextView lastEvent = view.findViewById(R.id.autoLastEvent);
        Button test = view.findViewById(R.id.testAutoParkingButton);
        Button disable = view.findViewById(R.id.disableAutoParkingButton);

        boolean configured =
                AutoParkingPrefs.address(this) != null
                        && !AutoParkingPrefs.address(this).trim().isEmpty();

        boolean enabled = AutoParkingPrefs.enabled(this) && configured;

        if (!AutoParkingManager.supported(this)) {
            status.setText("No compatible con este dispositivo");
        } else if (enabled) {
            status.setText("Activo · Bluetooth del coche");
        } else if (configured) {
            status.setText("Configurado, pero desactivado");
        } else {
            status.setText("Desactivado");
        }

        String name = AutoParkingPrefs.deviceName(this);
        device.setText(
                configured
                        ? "Dispositivo: " + (
                                name == null || name.trim().isEmpty()
                                        ? AutoParkingPrefs.address(this)
                                        : name)
                        : "Bluetooth del coche: no configurado");

        if (AutoParkingManager.hasForegroundLocation(this)
                && AutoParkingManager.hasBackgroundLocation(this)) {
            permission.setText("Ubicación automática: lista");
        } else if (AutoParkingManager.hasForegroundLocation(this)) {
            permission.setText("Ubicación automática: falta «Permitir siempre»");
        } else {
            permission.setText("Ubicación automática: falta permiso de ubicación precisa");
        }

        lastEvent.setText(AutoParkingPrefs.lastEvent(this));

        test.setEnabled(enabled);
        test.setAlpha(enabled ? 1f : 0.45f);
        disable.setEnabled(configured || enabled);
        disable.setAlpha((configured || enabled) ? 1f : 0.45f);
    }

    private void configureCarBluetooth() {
        if (!AutoParkingManager.supported(this)) {
            Toast.makeText(
                    this,
                    "Este Android no admite el modo automático por dispositivo compañero.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= 31
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {

            pendingChooseCarAfterBluetoothPermission = true;

            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQ_BT_CONNECT);
            return;
        }

        chooseBondedCarDevice();
    }

    private void chooseBondedCarDevice() {
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        BluetoothAdapter adapter =
                bluetoothManager == null ? null : bluetoothManager.getAdapter();

        if (adapter == null) {
            Toast.makeText(
                    this,
                    "Este teléfono no tiene Bluetooth disponible.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!adapter.isEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Activa Bluetooth")
                    .setMessage("Activa Bluetooth y vuelve a seleccionar «Bluetooth de mi coche».")
                    .setPositiveButton("Abrir Bluetooth",
                            (d, w) -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        try {
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();

            if (bonded == null || bonded.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("No hay dispositivos emparejados")
                        .setMessage(
                                "Primero empareja el Bluetooth del coche desde los ajustes del teléfono. "
                                + "Después vuelve aquí y aparecerá en la lista.")
                        .setPositiveButton("Abrir Bluetooth",
                                (d, w) -> startActivity(
                                        new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
                        .setNegativeButton("Cancelar", null)
                        .show();
                return;
            }

            List<BluetoothDevice> devices = new ArrayList<>(bonded);
            devices.sort(Comparator.comparing(
                    d -> safeDeviceName(d).toLowerCase(Locale.ROOT)));

            String[] labels = new String[devices.size()];

            for (int i = 0; i < devices.size(); i++) {
                BluetoothDevice device = devices.get(i);
                labels[i] = safeDeviceName(device) + "\n" + device.getAddress();
            }

            new AlertDialog.Builder(this)
                    .setTitle("Elige el Bluetooth del coche")
                    .setItems(labels, (dialog, which) -> {
                        BluetoothDevice selected = devices.get(which);
                        requestCompanionAssociation(
                                selected.getAddress(),
                                safeDeviceName(selected));
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();

        } catch (SecurityException e) {
            Toast.makeText(
                    this,
                    "Falta permiso para leer tus dispositivos Bluetooth emparejados.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String safeDeviceName(BluetoothDevice device) {
        try {
            String name = device.getName();
            return name == null || name.trim().isEmpty()
                    ? "Dispositivo Bluetooth"
                    : name;
        } catch (SecurityException e) {
            return "Dispositivo Bluetooth";
        }
    }

    @SuppressWarnings("deprecation")
    private void requestCompanionAssociation(String address, String name) {
        try {
            CompanionDeviceManager manager =
                    (CompanionDeviceManager) getSystemService(
                            COMPANION_DEVICE_SERVICE);

            BluetoothDeviceFilter filter =
                    new BluetoothDeviceFilter.Builder()
                            .setAddress(address)
                            .build();

            AssociationRequest request =
                    new AssociationRequest.Builder()
                            .addDeviceFilter(filter)
                            .setSingleDevice(true)
                            .build();

            CompanionDeviceManager.Callback callback =
                    new CompanionDeviceManager.Callback() {

                        @Override
                        public void onDeviceFound(IntentSender chooserLauncher) {
                            launchAssociationUi(chooserLauncher);
                        }

                        @Override
                        public void onAssociationPending(IntentSender chooserLauncher) {
                            launchAssociationUi(chooserLauncher);
                        }

                        @Override
                        public void onAssociationCreated(AssociationInfo associationInfo) {
                            // En Android 13+ también llegará onActivityResult.
                            // Guardamos aquí por si el fabricante no devuelve el extra.
                            if (Build.VERSION.SDK_INT >= 33
                                    && associationInfo.getDeviceMacAddress() != null) {
                                finishCarAssociation(
                                        associationInfo.getDeviceMacAddress().toString(),
                                        name);
                            }
                        }

                        @Override
                        public void onFailure(CharSequence error) {
                            runOnUiThread(() -> Toast.makeText(
                                    MainActivity.this,
                                    error == null
                                            ? "No se pudo asociar el Bluetooth del coche."
                                            : error,
                                    Toast.LENGTH_LONG).show());
                        }
                    };

            if (Build.VERSION.SDK_INT >= 33) {
                manager.associate(request, getMainExecutor(), callback);
            } else {
                manager.associate(request, callback, null);
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "No se pudo iniciar la asociación Bluetooth: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void launchAssociationUi(IntentSender sender) {
        try {
            startIntentSenderForResult(
                    sender,
                    REQ_ASSOCIATE_CAR,
                    null,
                    0,
                    0,
                    0);
        } catch (IntentSender.SendIntentException e) {
            Toast.makeText(
                    this,
                    "No se pudo abrir la selección de Bluetooth.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void finishCarAssociation(String address, String name) {
        if (address == null || address.trim().isEmpty()) return;

        String currentAddress = AutoParkingPrefs.address(this);
        if (AutoParkingPrefs.enabled(this)
                && currentAddress != null
                && currentAddress.equalsIgnoreCase(address)) {
            AutoParkingManager.ensureObservation(this);
            if (currentSettingsView != null) {
                updateSettingsView(currentSettingsView);
            }
            return;
        }

        String previous = currentAddress;

        if (previous != null
                && !previous.trim().isEmpty()
                && !previous.equalsIgnoreCase(address)) {
            AutoParkingManager.stopObservation(this);
        }

        AutoParkingPrefs.configure(this, address, name);
        AutoParkingManager.ensureObservation(this);

        if (currentSettingsView != null) {
            updateSettingsView(currentSettingsView);
        }

        requestNotificationPermissionIfNeeded();

        new AlertDialog.Builder(this)
                .setTitle("Bluetooth del coche configurado")
                .setMessage(
                        "Ya puedo detectar cuándo este Bluetooth se conecta y se desconecta.\n\n"
                        + "Para guardar el coche con la app cerrada, Android también necesita "
                        + "que la ubicación de Mi Coche esté en «Permitir siempre».")
                .setPositiveButton("Configurar ubicación",
                        (d, w) -> showAutomaticPermissionGuide())
                .setNegativeButton("Luego", null)
                .show();
    }

    private void showAutomaticPermissionGuide() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            new AlertDialog.Builder(this)
                    .setTitle("Ubicación precisa")
                    .setMessage(
                            "Primero permite la ubicación precisa. Después configuraremos "
                            + "«Permitir siempre» para que funcione al apagar el coche.")
                    .setPositiveButton("Continuar",
                            (d, w) -> {
                                pendingAutoPermissionGuide = true;
                                pendingManualSaveAfterLocationPermission = false;
                                requestPermissions(
                                        new String[]{
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                        },
                                        REQ_LOCATION);
                            })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        if (Build.VERSION.SDK_INT >= 29
                && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            new AlertDialog.Builder(this)
                    .setTitle("Permitir siempre")
                    .setMessage(
                            "Para guardar la posición cuando el Bluetooth se desconecta con "
                            + "Mi Coche cerrada:\n\n"
                            + "1. Abre los permisos de Mi Coche.\n"
                            + "2. Entra en Ubicación.\n"
                            + "3. Selecciona «Permitir siempre».\n\n"
                            + "Luego vuelve a la aplicación.")
                    .setPositiveButton("Abrir permisos",
                            (d, w) -> {
                                Intent intent =
                                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        Toast.makeText(
                this,
                "Los permisos de ubicación para el modo automático están listos.",
                Toast.LENGTH_SHORT).show();

        if (currentSettingsView != null) {
            updateSettingsView(currentSettingsView);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_NOTIFICATIONS);
        }
    }

    private void testAutomaticParking() {
        if (!AutoParkingPrefs.enabled(this)) {
            Toast.makeText(
                    this,
                    "Primero configura el Bluetooth del coche.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!AutoParkingManager.hasForegroundLocation(this)
                || !AutoParkingManager.hasBackgroundLocation(this)) {
            showAutomaticPermissionGuide();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Probar guardado automático")
                .setMessage(
                        "Esta prueba sustituirá la ubicación guardada del coche por tu "
                        + "posición actual. Sirve para comprobar GPS, permisos, widget y aviso.")
                .setPositiveButton("Probar", (d, w) -> {
                    AutoParkingManager.saveAutomaticParking(this, true);
                    rootView.postDelayed(() -> {
                        refreshUi();
                        if (currentSettingsView != null) {
                            updateSettingsView(currentSettingsView);
                        }
                    }, 1200);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // -------------------------------------------------------------------------
    // GUARDADO MANUAL (se conserva)
    // -------------------------------------------------------------------------

    private void saveCurrentLocation() {
        if (!hasLocationPermission()) {
            pendingManualSaveAfterLocationPermission = true;
            pendingAutoPermissionGuide = false;
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQ_LOCATION);
            return;
        }

        if (!isAnyProviderEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Activa la ubicación")
                    .setMessage(
                            "Para guardar el coche necesito que la ubicación "
                            + "del teléfono esté activada.")
                    .setPositiveButton(
                            "Abrir ajustes",
                            (d, w) -> startActivity(
                                    new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        Toast.makeText(
                this,
                "Obteniendo ubicación…",
                Toast.LENGTH_SHORT).show();

        requestFreshLocation(location -> {
            if (location == null) {
                Toast.makeText(
                        this,
                        "No he podido obtener la ubicación. Inténtalo de nuevo.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            final double lat = location.getLatitude();
            final double lon = location.getLongitude();

            CarStorage.save(
                    this,
                    lat,
                    lon,
                    "Obteniendo dirección…",
                    System.currentTimeMillis(),
                    CarStorage.SOURCE_MANUAL);

            refreshUi();
            updateDistance(location);
            CarWidgetProvider.updateAll(this);
            reverseGeocodeAndSave(lat, lon);

            Toast.makeText(
                    this,
                    "Coche guardado aquí",
                    Toast.LENGTH_SHORT).show();
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
                locationManager.getCurrentLocation(
                        provider,
                        null,
                        executor,
                        callback::onLocation);
            } else {
                LocationListener listener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        callback.onLocation(location);
                        try {
                            locationManager.removeUpdates(this);
                        } catch (SecurityException ignored) {}
                    }

                    @Override public void onProviderEnabled(String provider) {}
                    @Override public void onProviderDisabled(String provider) {}
                    @Override public void onStatusChanged(
                            String provider,
                            int status,
                            Bundle extras) {}
                };

                locationManager.requestSingleUpdate(provider, listener, null);
            }
        } catch (SecurityException e) {
            callback.onLocation(null);
        }
    }

    private void reverseGeocodeAndSave(double lat, double lon) {
        final long savedTime = CarStorage.time(this);
        final String source = CarStorage.source(this);

        new Thread(() -> {
            String result = "";

            try {
                Geocoder geocoder =
                        new Geocoder(MainActivity.this, new Locale("es", "ES"));

                List<Address> list =
                        geocoder.getFromLocation(lat, lon, 1);

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
                String address =
                        finalResult == null || finalResult.trim().isEmpty()
                                ? String.format(
                                        Locale.US,
                                        "%.6f, %.6f",
                                        lat,
                                        lon)
                                : finalResult;

                CarStorage.save(
                        MainActivity.this,
                        lat,
                        lon,
                        address,
                        savedTime,
                        source);

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

                if (l != null
                        && (best == null
                        || (!best.hasAccuracy() && l.hasAccuracy())
                        || (best.hasAccuracy() && l.hasAccuracy()
                        && l.getAccuracy() < best.getAccuracy()))) {
                    best = l;
                }
            }

            if (best != null) updateDistance(best);

        } catch (SecurityException ignored) {}
    }

    private void updateDistance(Location current) {
        if (!CarStorage.hasCar(this) || current == null) return;

        float[] results = new float[1];

        Location.distanceBetween(
                current.getLatitude(),
                current.getLongitude(),
                CarStorage.lat(this),
                CarStorage.lon(this),
                results);

        float meters = results[0];

        if (meters < 1000) {
            distanceText.setText(Math.round(meters) + " m");
        } else {
            distanceText.setText(
                    String.format(Locale.US, "%.1f km", meters / 1000f));
        }
    }

    private void navigateToCar() {
        if (!CarStorage.hasCar(this)) {
            Toast.makeText(
                    this,
                    "Primero guarda la ubicación del coche",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        double lat = CarStorage.lat(this);
        double lon = CarStorage.lon(this);

        Intent google = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                        "google.navigation:q="
                                + lat + "," + lon + "&mode=w"));

        google.setPackage("com.google.android.apps.maps");

        try {
            startActivity(google);
        } catch (Exception e) {
            startActivity(
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    "geo:0,0?q="
                                            + lat + "," + lon
                                            + "(Mi%20Coche)")));
        }
    }

    private void shareCar() {
        if (!CarStorage.hasCar(this)) {
            Toast.makeText(
                    this,
                    "Primero guarda la ubicación del coche",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        double lat = CarStorage.lat(this);
        double lon = CarStorage.lon(this);
        String address = CarStorage.address(this);

        String text =
                "Mi coche está aquí:\n"
                        + (address == null ? "" : address + "\n")
                        + String.format(
                                Locale.US,
                                "https://www.google.com/maps?q=%.6f,%.6f",
                                lat,
                                lon);

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);

        startActivity(
                Intent.createChooser(
                        send,
                        "Compartir ubicación del coche"));
    }

    private void deleteCar() {
        if (!CarStorage.hasCar(this)) return;

        new AlertDialog.Builder(this)
                .setTitle("Borrar ubicación")
                .setMessage(
                        "¿Quieres borrar la ubicación guardada del coche?")
                .setPositiveButton("Borrar", (d, w) -> {
                    CarStorage.clear(this);
                    refreshUi();
                    CarWidgetProvider.updateAll(this);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isAnyProviderEnabled() {
        try {
            return locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    private String bestProvider() {
        try {
            if (locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)) {
                return LocationManager.GPS_PROVIDER;
            }

            if (locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER)) {
                return LocationManager.NETWORK_PROVIDER;
            }
        } catch (Exception ignored) {}

        return null;
    }

    private String formatAgo(long time) {
        if (time <= 0) return "Ubicación guardada";

        long seconds =
                Math.max(
                        0,
                        (System.currentTimeMillis() - time) / 1000L);

        if (seconds < 45) return "Guardado ahora";

        long minutes = seconds / 60L;
        if (minutes < 60) return "Guardado hace " + minutes + " min";

        long hours = minutes / 60L;
        if (hours < 24) return "Guardado hace " + hours + " h";

        long days = hours / 24L;
        return "Guardado hace "
                + days
                + (days == 1 ? " día" : " días");
    }

    private int dp(int value) {
        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data);

        if (requestCode != REQ_ASSOCIATE_CAR
                || resultCode != RESULT_OK
                || data == null) {
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                AssociationInfo association =
                        data.getParcelableExtra(
                                CompanionDeviceManager.EXTRA_ASSOCIATION,
                                AssociationInfo.class);

                if (association != null
                        && association.getDeviceMacAddress() != null) {

                    String address =
                            association
                                    .getDeviceMacAddress()
                                    .toString();

                    String name =
                            resolveBondedDeviceName(address);

                    finishCarAssociation(address, name);
                    return;
                }
            }

            @SuppressWarnings("deprecation")
            BluetoothDevice device =
                    data.getParcelableExtra(
                            CompanionDeviceManager.EXTRA_DEVICE);

            if (device != null) {
                finishCarAssociation(
                        device.getAddress(),
                        safeDeviceName(device));
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Bluetooth asociado, pero no pude leer sus datos.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String resolveBondedDeviceName(String address) {
        if (address == null) return "Bluetooth del coche";

        try {
            BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(
                            BLUETOOTH_SERVICE);

            BluetoothAdapter adapter =
                    bluetoothManager == null
                            ? null
                            : bluetoothManager.getAdapter();

            if (adapter == null) return "Bluetooth del coche";

            for (BluetoothDevice device : adapter.getBondedDevices()) {
                if (address.equalsIgnoreCase(device.getAddress())) {
                    return safeDeviceName(device);
                }
            }
        } catch (SecurityException ignored) {}

        return "Bluetooth del coche";
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode == REQ_BT_CONNECT) {
            if (Build.VERSION.SDK_INT < 31
                    || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {

                if (pendingChooseCarAfterBluetoothPermission) {
                    pendingChooseCarAfterBluetoothPermission = false;
                    chooseBondedCarDevice();
                }
            } else {
                pendingChooseCarAfterBluetoothPermission = false;

                Toast.makeText(
                        this,
                        "Necesito acceso a dispositivos cercanos para elegir el Bluetooth del coche.",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (requestCode == REQ_LOCATION) {
            if (hasLocationPermission()) {
                if (pendingManualSaveAfterLocationPermission) {
                    pendingManualSaveAfterLocationPermission = false;
                    pendingAutoPermissionGuide = false;
                    saveCurrentLocation();
                } else if (pendingAutoPermissionGuide) {
                    pendingAutoPermissionGuide = false;
                    showAutomaticPermissionGuide();
                }
            } else {
                pendingManualSaveAfterLocationPermission = false;
                pendingAutoPermissionGuide = false;
                Toast.makeText(
                        this,
                        "Necesito permiso de ubicación para guardar el coche.",
                        Toast.LENGTH_LONG).show();
            }
        }

        if (currentSettingsView != null) {
            updateSettingsView(currentSettingsView);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (ACTION_SAVE_NOW.equals(intent.getAction())) {
            rootView.postDelayed(
                    this::saveCurrentLocation,
                    150);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        AutoParkingManager.ensureObservation(this);

        if (locationManager != null) {
            refreshDistanceFromLastKnown();
        }

        refreshUi();

        if (currentSettingsView != null) {
            updateSettingsView(currentSettingsView);
        }
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
