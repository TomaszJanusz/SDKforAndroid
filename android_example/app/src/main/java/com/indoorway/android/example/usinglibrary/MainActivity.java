package com.indoorway.android.example.usinglibrary;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.indoorway.android.common.sdk.exceptions.MissingPermissionException;
import com.indoorway.android.common.sdk.listeners.GenericListenerArg0;
import com.indoorway.android.common.sdk.listeners.GenericListenerArg1;
import com.indoorway.android.common.sdk.listeners.position.OnHeadingChangedListener;
import com.indoorway.android.common.sdk.listeners.position.OnPositionChangedListener;
import com.indoorway.android.common.sdk.model.Coordinates;
import com.indoorway.android.common.sdk.model.IndoorwayBuildingParameters;
import com.indoorway.android.common.sdk.model.IndoorwayIconParameters;
import com.indoorway.android.common.sdk.model.IndoorwayObjectId;
import com.indoorway.android.common.sdk.model.IndoorwayObjectParameters;
import com.indoorway.android.common.sdk.model.IndoorwayPosition;
import com.indoorway.android.common.sdk.task.IndoorwayTask;
import com.indoorway.android.location.sdk.IndoorwayLocationSdk;
import com.indoorway.android.location.sdk.exceptions.bluetooth.BLENotSupportedException;
import com.indoorway.android.location.sdk.exceptions.bluetooth.BluetoothDisabledException;
import com.indoorway.android.location.sdk.exceptions.location.LocationDisabledException;
import com.indoorway.android.location.sdk.model.visitor.Visitor;
import com.indoorway.android.location.sdk.service.PositioningServiceConnection;
import com.indoorway.android.map.sdk.IndoorwayMapSdk;
import com.indoorway.android.map.sdk.listeners.OnMapLoadedListener;
import com.indoorway.android.map.sdk.listeners.OnObjectSelectedListener;
import com.indoorway.android.map.sdk.model.IndoorwayMap;
import com.indoorway.android.map.sdk.view.IndoorwayMapView;
import com.indoorway.android.map.sdk.view.drawable.figures.DrawableCircle;
import com.indoorway.android.map.sdk.view.drawable.figures.DrawableText;
import com.indoorway.android.map.sdk.view.drawable.layers.MarkersLayer;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 1;

    // Text views for sample data: position, direction, selected object ids etc.
    TextView tvPosition;
    TextView tvDirection;
    TextView tvSelectedObject;
    AlertDialog lastDialog;

    /**
     * Id of current selected object.
     */
    @Nullable
    String selectedObjectId;

    /**
     * Current positon.
     */
    @Nullable
    IndoorwayPosition currentPosition;

    /**
     * Map view.
     */
    IndoorwayMapView indoorwayMapView;

    /**
     * Connection with positioning service.
     * Used for control: starting, stopping and receiving position updates.
     */
    PositioningServiceConnection serviceConnection;

    /**
     * Instance of Map SDK.
     */
    private IndoorwayMapSdk mapSdk;

    /**
     * Instance of Location SDK.
     */
    private IndoorwayLocationSdk locationSdk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSelectedObject = (TextView) findViewById(R.id.tvSelectedObject);

        tvPosition = (TextView) findViewById(R.id.tvPosition);
        tvDirection = (TextView) findViewById(R.id.tvDirection);

        mapSdk = IndoorwayMapSdk.getInstance();
        locationSdk = IndoorwayLocationSdk.getInstance();

        indoorwayMapView = (IndoorwayMapView) findViewById(R.id.mapView);
        setCustomColorsForMapView();
        loadMap();

        initInteractionButtons();
        getSampleData();
    }

    /**
     * Optional: Setting custom colors for map view before map loading.
     */
    void setCustomColorsForMapView() {
        mapSdk.getConfig()
                // eg. custom background color for different room types
                .setCustomBackgroundColor("inaccessible", Color.parseColor("#23252C"))
                .setCustomBackgroundColor("elevator", Color.parseColor("#23252C"))
                // custom map outdoor background
                .setMapOutdoorBackgroundColor(Color.parseColor("#282a30"));
    }

    /**
     * Loading default map and setting listener for object selection/deselection.
     */
    void loadMap() {
        indoorwayMapView
                // optional: assign callback for map loaded event
                .setOnMapLoadCompletedListener(new OnMapLoadedListener() {
                    @Override
                    public void onAction(IndoorwayMap indoorwayMap) {
                        // called on every new map load success
                        initCustomMarkers();
                    }
                })
                // optional: assign callback for map loading failure
                .setOnMapLoadFailedListener(new GenericListenerArg0() {
                    @Override
                    public void onAction() {
                        // called on every map load error
                    }
                })
                // TODO: insert building and map uuid
                .loadMap("<BUILDING-UUID>", "<MAP-UUID>");

        indoorwayMapView.getSelectionControl().setOnObjectSelectedListener(new OnObjectSelectedListener() {

            @Override
            public boolean canObjectBeSelected(IndoorwayObjectParameters objectParameters) {
                return !"inaccessible".equals(objectParameters.getType());
            }

            @Override
            public void onObjectSelected(IndoorwayObjectParameters objectParameters) {
                selectedObjectId = objectParameters.getId();
                tvSelectedObject.setText("Selected: " + selectedObjectId);
                updateNavigation();
            }

            @Override
            public void onSelectionCleared() {
                selectedObjectId = null;
                tvSelectedObject.setText("No object selected");
                updateNavigation();
            }

        });
    }

    void initCustomMarkers() {
        // create new layer
        MarkersLayer layer = indoorwayMapView.getMarkerControl().addLayer();
        // insert multiple objects
        // ... labels
        layer.add(
                new DrawableText(
                        "<label-id>",   // unique identifier
                        new Coordinates(52.10, 21.30), // lat, long
                        "Sample label",
                        2f              // height
                )
        );
        // ... figures
        layer.add(
                new DrawableCircle(
                        "<figure-id>", // unique identifier
                        10f,           // radius (in meters)
                        Color.RED,     // background color
                        Color.BLUE,    // outline color
                        0.2f,          // outline width (in meters)
                        new Coordinates(52.20, 21.40) // lat, long of center point
                )
        );
    }

    /**
     * Initialisation of sample interaction buttons.
     */
    void initInteractionButtons() {
        // selection of object with given id
        findViewById(R.id.btnSelect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                indoorwayMapView.getSelectionControl().selectObject("<OBJECT-ID>");
            }
        });

        // deselection of selected object
        findViewById(R.id.btnDeselect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                indoorwayMapView.getSelectionControl().deselect();
            }
        });
    }

    /**
     * Getting data: building, map, image adresses etc.
     */
    void getSampleData() {
        // getting buildings list
        mapSdk.getBuildingsApi()
                .getBuildings()
                .setOnCompletedListener(new GenericListenerArg1<List<IndoorwayBuildingParameters>>() {
                    @Override
                    public void onAction(List<IndoorwayBuildingParameters> indoorwayBuildingParameters) {
                        // handle buildings list
                    }
                })
                .setOnFailedListener(new GenericListenerArg1<IndoorwayTask.ProcessingException>() {
                    @Override
                    public void onAction(IndoorwayTask.ProcessingException e) {
                        // handle error, original exception is given on e.getCause()
                    }
                })
                .execute();

        // getting maps for building
        mapSdk.getBuildingsApi()
                // TODO: insert building uuid
                .getMaps("<BUILDING-UUID>")
                .setOnCompletedListener(new GenericListenerArg1<List<IndoorwayObjectId>>() {
                    @Override
                    public void onAction(List<IndoorwayObjectId> indoorwayObjectIds) {
                        // handle maps list
                    }
                })
                .setOnFailedListener(new GenericListenerArg1<IndoorwayTask.ProcessingException>() {
                    @Override
                    public void onAction(IndoorwayTask.ProcessingException e) {
                        // handle error, original exception is given on e.getCause()
                    }
                })
                .execute();

        // getting map objects
        mapSdk.getBuildingsApi()
                // TODO: insert building and map uuid
                .getMapObjects("<BUILDING-UUID>", "<MAP-UUID>")
                .setOnCompletedListener(new GenericListenerArg1<IndoorwayMap>() {
                    @Override
                    public void onAction(IndoorwayMap indoorwayMap) {
                        // handle map objects
                    }
                })
                .setOnFailedListener(new GenericListenerArg1<IndoorwayTask.ProcessingException>() {
                    @Override
                    public void onAction(IndoorwayTask.ProcessingException e) {
                        // handle error, original exception is given on e.getCause()
                    }
                })
                .execute();

        // getting available images
        mapSdk.getLogosApi()
                .getLogotypes()
                .setOnCompletedListener(new GenericListenerArg1<List<IndoorwayIconParameters>>() {
                    @Override
                    public void onAction(List<IndoorwayIconParameters> indoorwayIconParameterses) {
                        // handle available logo images
                    }
                })
                .setOnFailedListener(new GenericListenerArg1<IndoorwayTask.ProcessingException>() {
                    @Override
                    public void onAction(IndoorwayTask.ProcessingException e) {
                        // handle error, original exception is given on e.getCause()
                    }
                })
                .execute();

        // getting available poi types
        mapSdk.getPoisApi()
                .getPois()
                .setOnCompletedListener(new GenericListenerArg1<List<IndoorwayIconParameters>>() {
                    @Override
                    public void onAction(List<IndoorwayIconParameters> indoorwayIconParameterses) {
                        // handle poi types (eg. for legend)
                    }
                })
                .setOnFailedListener(new GenericListenerArg1<IndoorwayTask.ProcessingException>() {
                    @Override
                    public void onAction(IndoorwayTask.ProcessingException e) {
                        // handle error, original exception is given on e.getCause()
                    }
                })
                .execute();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // starting service
        serviceConnection = locationSdk.getPositioningServiceConnection();

        // setup of visitor tracking
        trackVisitor();

        // starting positioning service
        startPositioningService();
    }

    @Override
    protected void onPause() {
        // remember to stop service on pause
        // as it may cause battery draining
        serviceConnection.stop(this);
        super.onPause();
    }

    /**
     * Optional: tracking visitor with specified informations.
     * Visitor will be registered and his location will be sent to analytics dashboard.
     */
    protected void trackVisitor() {
        Visitor visitor = new Visitor()
                .setName("John Smith")
                .setAge(60)
                .setSex(Visitor.Sex.MALE);

        // you can call setupVisotor at any time
        // tracking will be set up as soon as position is found
        serviceConnection.setupVisitor(visitor);
    }

    /**
     * Starting positioning service.
     */
    protected void startPositioningService() {
        try {
            serviceConnection
                    .setOnPositionChangedListener(new OnPositionChangedListener() {
                        @Override
                        public void onPositionChanged(IndoorwayPosition position) {
                            // react for position changes
                            currentPosition = position;
                            // update navigation state (if necessary)
                            updateNavigation();
                            tvPosition.setText(String.format("Position: %s, %s", position.getCoordinates().getLatitude(), position.getCoordinates().getLongitude()));
                            // pass position changes to map view
                            // second argument indicates if you want to auto load map on position change
                            // for eg. after going to different building level
                            indoorwayMapView.getPositionControl().setPosition(position, false);
                        }
                    })
                    .setOnHeadingChangedListener(new OnHeadingChangedListener() {
                        @Override
                        public void onHeadingChanged(float angle) {
                            // react for heading changes
                            tvDirection.setText(String.format("Direction: %s", angle));
                            // pass heading changes to map view
                            indoorwayMapView.getPositionControl().setHeading(angle);
                        }
                    })
                    .start(this);
        } catch (MissingPermissionException e) {
            // some permission is missing, ask for it and try again
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String[] permissions = {e.getPermission()};
                requestPermissions(permissions, REQUEST_PERMISSION_CODE);
            }
        } catch (BLENotSupportedException e) {
            // ble is not supported, indoor location won't work
            closeDialog();
            lastDialog = new AlertDialog.Builder(this)
                    .setTitle("Sorry")
                    .setMessage("Bluetooth Low Energy is not supported on your device. We are unable to find your indoor location.")
                    .setCancelable(true)
                    .show();
        } catch (BluetoothDisabledException e) {
            // ask user to enable bluetooth
            closeDialog();
            lastDialog = new AlertDialog.Builder(this)
                    .setTitle("Please enable bluetooth")
                    .setMessage("In order to find your indoor position, you need to enable bluetooth on your device.")
                    .setCancelable(true)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent settingsIntent = new Intent();
                            settingsIntent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                            startActivity(settingsIntent);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            closeDialog();
                        }
                    })
                    .show();
        } catch (LocationDisabledException e) {
            // ask user to  enable location
            closeDialog();
            lastDialog = new AlertDialog.Builder(this)
                    .setTitle("Please enable location")
                    .setMessage("In order of finding your indoor position you must enable location in settings.")
                    .setCancelable(true)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent settingsIntent = new Intent();
                            settingsIntent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(settingsIntent);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            closeDialog();
                        }
                    })
                    .show();
        }
    }

    /**
     * Controlling navigation on map.
     */
    private void updateNavigation() {
        // stopping navigation
        if (selectedObjectId == null || currentPosition == null) {
            indoorwayMapView.getNavigationControl().stop();
            return;
        }

        // navigation from specified position to given object ...
        indoorwayMapView.getNavigationControl().start(currentPosition, selectedObjectId);

        // ... or to another position
        // indoorwayMapView.navigate(currentPosition.getCoordinates(), new Coordinates(53, 21));
    }

    /**
     * Closing dialog.
     */
    private void closeDialog() {
        if (lastDialog != null && lastDialog.isShowing())
            lastDialog.dismiss();
        lastDialog = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // if permission is satisfied, run positioning service once again
        if (requestCode == REQUEST_PERMISSION_CODE && grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startPositioningService();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}