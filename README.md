# Indoorway Android SDK

[![Twitter](https://img.shields.io/badge/twitter-@Indoorway-blue.svg?style=flat)](http://twitter.com/indoorway)

Indoorway lets you find your way indoors. Check it out!

- [Features](#features)
- [Modules](#modules)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
    + [Initial configuration](#initial-configuration)
    + [Map displaying](#map-displaying)
    + [Custom map rendering](#custom-map-rendering)
    + [Custom markers](#custom-markers)
    + [Indoor objects selection](#indoor-objects-selection)
    + [Navigation](#navigation)
    + [Fetching buildings and maps objects](#fetching-buildings-and-maps-objects)
    + [Indoor positioning](#indoor-positioning)
    + [User tracking](#user-tracking)
    + [Ranges](#ranges)
    + [Proximity communication](#proximity-communication)
    + [Tracking](#tracking)
- [Documentation](#documentation)
- [Support](#support)
- [Licence](#licence)

## Features

- [x] Indoor location
- [x] Navigation
- [x] Map view
- [x] Documentation

## Modules

Indoorway SDK consists two main modules: 

- location SDK - for indoor positioning,
- map SDK - used for displaying map.

You can use one of them or both.

## Requirements

- Java 8 must be used for project compilation.

#### LocationSDK

- Android 4.3+ (SDK v18),
- BLE (Bluetooth Low Energy) support.

#### MapSDK

- Android 4.0+ (SDK v14),
- OpenGL ES 2.0 support.

## Installation

In order to use Indoorway SDK you need to set up your build script.

> Attention: error may occur during project build due to known third-party library (Jackson) bug. It is **neccessary** to include in application module script:
```gradle
android {
    packagingOptions {
        pickFirst 'META-INF/LICENSE'
    }
    // ...
}
```

#### Gradle / Maven

> __`{latest.version}`__ is `1.0.0`. You can check another versions [here](https://indoorway.com/sdk/android/repo/maven/com/indoorway/android/).

Module `com.indoorway.android:common` contains common set of required SDK classes. Librares `com.indoorway.android:map` and `com.indoorway.android:location` are independend to each other, they can be used separately or linked together.

[Gradle](#):

Setup repository:

```gradle
repositories {
    maven { url 'https://indoorway.com/sdk/android/repo/maven/' }
}
```

Setup dependencies:

```gradle
// required for map and location modules
compile('com.indoorway.android:common:{latest.version}')

// optional if you don't want to use map view
compile('com.indoorway.android:map:{latest.version}')

// optional if you don't want to use indoor positioning
compile('com.indoorway.android:location:{latest.version}')  
```

[Maven](#):

Setup repository:

```xml
<repositories>
    <repository>
      <id>indoorway</id>
      <name>Indoorway</name>
      <url>https://indoorway.com/sdk/android/repo/maven/</url>
    </repository>
</repositories>
```

Setup dependencies:

```xml
<!-- required for map and location modules -->
<dependency>
  <groupId>com.indoorway.android</groupId>
  <artifactId>common</artifactId>
  <version>{latest.version}</version>
</dependency>

<!-- optional if you don't want to use map view -->
<dependency>
  <groupId>com.indoorway.android</groupId>
  <artifactId>map</artifactId>
  <version>{latest.version}</version>
</dependency>

<!-- optional if you don't want to use indoor positioning -->
<dependency>
  <groupId>com.indoorway.android</groupId>
  <artifactId>location</artifactId>
  <version>{latest.version}</version>
</dependency>
```

## Usage

### Initial confguration

Before any use of the framework you must configure it using your API key. The best place to apply configuration is your Application class `onCreate` function.

> Tip: see [reference](https://developer.android.com/reference/android/app/Application.html) for creating custom implementation of Application class.

```java
/**
 * Application's class. Defined in AndroidManifest.xml
 * Initializes Indoorway sdk modules.
 */
public class CustomApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // ...

        // define your api key
        String trafficApiKey = "<Your API key>";

        // sdk for map view and fetching map objects
        IndoorwayMapSdk.init(this, trafficApiKey);

        // sdk for indoor positioning
        IndoorwayLocationSdk.init(this, trafficApiKey);
    }

}
```

### Map displaying

Firstly, define `com.indoorway.android.map.sdk.view.IndoorwayMapView` inside your layout:

```xml
<com.indoorway.android.map.sdk.view.IndoorwayMapView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_alignParentTop="true"
    android:id="@+id/mapView" />
```

Then refer it in your code:

```java

IndoorwayMapView indoorwayMapView = (IndoorwayMapView) findViewById(R.id.mapView);

indoorwayMapView
    // optional: assign callback for map loaded event
    .setOnMapLoadCompletedListener(new OnMapLoadedListener() {
        @Override
        public void onAction(IndoorwayMap indoorwayMap) {
            // called on every new map load success

            // access to paths graph
            paths = indoorwayMap.getPaths(); 

            // access to map objects
            mapObjects = indoorwayMap.getObjects(); 
        }
    })    
    // optional: assign callback for map loading failure
    .setOnMapLoadFailedListener(new GenericListenerArg0() {
        @Override
        public void onAction() {
            // called on every map load error
        }
    })
    // perform map loading using building UUID and map UUID
    .loadMap("<building UUID>", "<map UUID>");

```

### Custom map rendering

To customize map view's displaying attributes simply call setters on object returned by `mapSdk.getConfig()` before any map loading:

```java
IndoorwayMapSdk mapSdk = IndoorwayMapSdk.getInstance();

mapSdk.getConfig()
    // eg. custom background color for different room types
    .setCustomBackgroundColor("inaccessible", Color.parseColor("#23252C"))
    .setCustomBackgroundColor("elevator", Color.parseColor("#23252C"))
    // custom map outdoor background
    .setMapOutdoorBackgroundColor(Color.parseColor("#282a30"));
```

There are more properties which can be set. Refer to full documentation for complete attributes list and their default values.

### Custom markers

> Attention: custom markers can be added only **after** map loading. You may use `OnMapLoadedListener` in your map view.
> ```java
> indoorwayMapView
>     .setOnMapLoadCompletedListener(new OnMapLoadedListener() {
>         @Override
>         public void onAction(BuildingAndMapId buildingAndMapId) {
>             // load custom markers here!
>         }
>     });
> ```
>
> Please note that layers list is cleared after loading new map.

In order to add your own marker on a map, you have to create new `MarkersLayer`.

Firstly, define it as a field: 
```
MarkersLayer myLayer;
```

When map loads, obtain it's reference:
```
myLayer = indoorwayMapView.getMarkerControl().addLayer(priority);
```

`priority` is a float value which determines layers order. They are drawn from the lowest priority value to the highest. Standard map layers are registered with priorities from _0.0_ to _10.0_.
`addLayer` may be called multiple times to get multiple, independent layers.


#### Text

To add custom label on a map, call:

```java
myLayer.add(
        new DrawableText(
            "<marker-id>",
            new Coordinates(latitude, longitude),
            "Sample label",
            textHeight // eg. 2f
        )
    );
```

Identifiers of markers (like `<marker-id>`) added through `add` should be unique. They are used to replace on duplicate or removal.

At any time, marker can be removed by:

```java
myLayer.remove("<marker-id>");
```

#### Icons

Adding icons to map requires a little more work. Firstly, you need to register a "texture". Texture is a grid of images grouped into a single image (known also as a _sprite sheet_).

Secondly, you can add `DrawableIcon` using _registered texture_ identifier.

See an example below:

```java
// load images grid as a bitmap
Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sample_texture);

myLayer
    // register texture with "<texture-id>" identifier
    .registerTexture(new DrawableTexture("<texture-id>", bitmap))
    // first icon
    .add(
        new DrawableIcon(
            "<icon1-id>",   // icon identifier
            "<texture-id>", // texture identifier
            new Coordinates(latitude1, longitude1),
            textureCoordsForIcon, 
            iconXSize1,  // eg. 2f
            iconYSize1   // eg. 2f
        )
    )
    // second icon
    .add(
        new DrawableIcon(
            "<icon2-id>",    // icon identifier
            "<texture-id>", // texture identifier
            new Coordinates(latitude2, longitude2),
            textureCoordsForIcon, 
            iconXSize2,  // eg. 2f
            iconYSize2   // eg. 2f
        )
    );
    // etc...
```

#### Figures

Figures can be added using it's coordinates. See an example below:

```java
Coordinates coordinates = new Coordinates(52.0, 21.0);
myLayer.add(new DrawableCircle(
    "<unique-figure-id>",             
    radius,         // radius in meters, eg. 0.4f
    color,          // circle background color, eg. Color.RED
    outlineColor,   // color of outline, eg. Color.BLUE
    outlineWidth,   // width of outline in meters, eg. 0.1f
    coordinates     // coordinates of circle center point
));
```

> Tip: If you don't want to draw any outline, just use Color.TRANSPARENT and/or set `outlineWidth` to 0f.

See full documentation for another types of figures. 

### Indoor objects selection

If you want to control which objects can be selected and receive callbacks, set listener on `IndoorwayMapView`:

```java
indoorwayMapView.getSelectionControl().setOnObjectSelectedListener(new OnObjectSelectedListener() {

    @Override
    public boolean canObjectBeSelected(IndoorwayObjectParameters objectParameters) {
        // return true if object with given parameters can be selected
        return !"inaccessible".equals(objectParameters.getType());
    }

    @Override
    public void onObjectSelected(IndoorwayObjectParameters objectParameters) {
        // called on object selection, check objectParameters for details
    }

    @Override
    public void onSelectionCleared() {
        // called when no object is selected
    }

});
```

> Attention: selection won't work if `OnObjectSelectedListener` was not set.

Indoor objects can be also selected programmatically by:

- identifier:

    ```java
    indoorwayMapView.getSelectionControl().selectObject("<object identifier>");
    ```
- latitude and longitude:

    ```java
    // object containing given point will be selected
    indoorwayMapView.getSelectionControl().selectObject(latitude, longitude);
    ```

If you want to clear selection, call:

```java
indoorwayMapView.getSelectionControl().deselect();
```

### Handling touch events

To receive `Coordinates` of "touched" position use `OnTouchListener`:

```java
indoorwayMapView.getTouchControl().setOnTouchListener(new OnTouchListener() {
    @Override
    public void onTouch(Coordinates coordinates) {
        // handle on touch event
    }
});
```

### Navigation

There are several ways you can show navigation path in map view:

- navigate from current user's location to specific destination object (see [indoor positioning](#indoor-positioning) section for more information and example with `currentPosition` field):

    ```java
    indoorwayMapView.getNavigationControl().start(currentPosition, "<object identifier>");
    ```

- navigate from current user's location to specific position (latitude, longitude):

    ```java
    indoorwayMapView.getNavigationControl().start(currentPosition, destinationLatitude, destinationLongitude);
    ```

- navigate from given latitude, longitude to specific destination object:

    ```java
    indoorwayMapView.getNavigationControl().start(startLatitude, startLongitude, "<object identifier>");
    ```

- navigate between two positions defined by latitude and longitude:
    
    ```java
    indoorwayMapView.getNavigationControl().start(startLatitude, startLongitude, destinationLatitude, destinationLongitude);
    ```

To stop navigation call:

```java
indoorwayMapView.getNavigationControl().stop();
```

### Fetching buildings and maps objects

Map SDK can be used to fetch buildings and map objects. Each request is processed on background and it's result is delivered on UI thread.

Firstly, obtain map SDK instance:

```java
IndoorwayMapSdk mapSdk = IndoorwayMapSdk.getInstance();
```

#### Getting buildings list

```java
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
```

#### Getting maps list for building

```java
mapSdk.getBuildingsApi()
        .getMaps("<building UUID>")
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
```

#### Getting map objects

When objects only for current map are needed, `OnMapLoadCompletedListener` on `IndoorwayMapView` object can be used. If you want to fetch map elements _without_ loading map, call:

```java
mapSdk.getBuildingsApi()
        .getMapObjects("<building UUID>", "<map UUID>")
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
```

If you want to filter objects by their tags, you can use `getTags()` which returns `Set<String>`:

```java
ArrayList<IndoorwayObjectParameters> result = new ArrayList<>();
for(IndoorwayObjectParameters objectParameters : indoorwayObjectParametersList) {
    Set<String> tags = objectParameters.getTags();
    if(tags.contains("sample-tag")) {
        result.add(objectParameters);
    }
}

// process filtered objects
```

#### Getting list of available images

```java
mapSdk.getLogosApi()
        .getLogotypes()
        .setOnCompletedListener(new GenericListenerArg1<List<IndoorwayIconParameters>>() {
            @Override
            public void onAction(List<IndoorwayIconParameters> indoorwayIconParametersList) {
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
```

#### Getting list of available poi types

```java
mapSdk.getPoisApi()
        .getPois()
        .setOnCompletedListener(new GenericListenerArg1<List<IndoorwayIconParameters>>() {
            @Override
            public void onAction(List<IndoorwayIconParameters> indoorwayIconParametersList) {
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
```

#### Getting list of available tags

```java
mapSdk.getTagsApi()
        .getTags()
        .setOnCompletedListener(new GenericListenerArg1<List<String>>() {
            @Override
            public void onAction(List<String> strings) {
                // handle list of available tags
            }
        })
        .setOnFailedListener(new GenericListenerArg1<IndoorwayTask.ProcessingException>() {
            @Override
            public void onAction(IndoorwayTask.ProcessingException e) {
                // handle error, original exception is given on e.getCause()
            }
        })
        .execute();
```

### Indoor positioning

In order to find user location you need to do few steps:

#### 1. Obtain service connection
Call `getPositioningServiceConnection` on you activity's `onResume` function and store it's result as a field:

> Attention: remember to stop `serviceConnection` on `onPause` method.

```java
/**
 * Connection with positioning service.
 * Used for control: starting, stopping and receiving position updates.
 */
PositioningServiceConnection serviceConnection;

@Override
protected void onResume() {
    super.onResume();
    // ...
    serviceConnection = IndoorwayLocationSdk.getInstance().getPositioningServiceConnection();
}

@Override
protected void onPause() {
    serviceConnection.stop(this);
    // ...
    super.onPause();
}
```

#### 2. Set position and heading change listeners

```java
serviceConnection
    .setOnPositionChangedListener(new OnPositionChangedListener() {
        @Override
        public void onPositionChanged(IndoorwayPosition position) {
            // store last position as a field
            currentPosition = position;

            // react for position changes...

            // If you are using map view, you can pass position.
            // Second argument indicates if you want to auto reload map on position change
            // for eg. after going to different building level.
            indoorwayMapView.getPositionControl().setPosition(position, true);
        }
    })
    .setOnHeadingChangedListener(new OnHeadingChangedListener() {
        @Override
        public void onHeadingChanged(float angle) {
            // react for heading changes...
            
            // If you are using map view, you can pass heading.
            indoorwayMapView.getPositionControl().setHeading(angle);
        }
    });
```

#### 3. Start positioning service

Service connection wil throw an exception during `start` if some conditions are met:

- BLE is not supported on device,
- bluetooth is disabled,
- one of following permissions is missing:
    + `Manifest.permission.ACCESS_FINE_LOCATION`,
    + `Manifest.permission.BLUETOOTH`,
    + `Manifest.permission.BLUETOOTH_ADMIN`,
- location provider is disabled.

Application must ask user for specified permissions and settings change if necessary. To properly handle all of these wrap `serviceConnection.start(this)` in try-catch block. When permission is granted or user changes required setting, call `start` again.

> Tip: there is no need to register broadcast receiver for bluetooth and location state change, SDK will take care for eg. if user clicks on statusbar switch.

```java
try {
    serviceConnection.start(this);
} catch (MissingPermissionException e) {
    // some permission is missing, ask for it and try again
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        String[] permissions = {e.getPermission()};
        // handle asking for permissions
    }
} catch (BLENotSupportedException e) {
    // ble is not supported, indoor positioning won't work
} catch (BluetoothDisabledException e) {
    // ask user to enable bluetooth
} catch (LocationDisabledException e) {
    // ask user to enable location
}
```

### User tracking

If you want to track user position for analytic purposes, call:

```java
Visitor visitor = new Visitor()
        // optional: set more detailed informations if you have one
        .setGroupUuid("<users group identifier>")   // user group
        .setName("John Smith")                      // user name
        .setAge(60)                                 // user age
        .setSex(Visitor.Sex.MALE);                  // user gender

// You can call setupVisitor at any time if you have serviceConnection reference.
// Tracking will be set up automatically as soon as position is found.
serviceConnection.setupVisitor(visitor);
```

### Ranges

In case you want receive callbacks when user enters or leaves specific region, you can use `OnRangeEnterExitListener`.

```java
@Override
protected void onResume() {
    // ...

    Range sampleRange = new Range(
        "<range-id>",  // identifier
        new Coordinates(51.0, 20.0), // coordinates
        3f          // radius [meters]
    );

    serviceConnection
            // add multiple ranges
            .addRange(sampleRange)
            // receive callbacks
            .setOnRangeEnterExitListener(new OnRangeEnterExitListener() {
                @Override
                public void onEnter(Range range) {
                    // called when user entries range
                    if(range.getId().equals("<range-id>")) {
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("onEnter called")
                            .setMessage("Welcome!")
                            .show();
                    }
                }

                @Override
                public void onExit(Range range) {
                    // called when user exits range
                    if(range.getId().equals("<range-id>")) {
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("onExit called")
                            .setMessage("Goodbye!")
                            .show();
                    }
                }
            });

    // ...
}
```

> You can add as many ranges as you want. Please note their identifiers need to be unique.

### Proximity communication

Indoorway SDK supports proximity communication. Once defined in dashboard, events can be received on app to display notifications under some circumstances. 
Currently enter/exit triggers are supported.

```java
serviceConnection
    .setOnProximityEventListener(new OnProximityEventListener() {
        @Override
        public void onEvent(IndoorwayProximityEvent proximityEvent) {
            // show notification using event title, description, url etc.
            // track conversion using proximityEvent.getUuid()
        }
    });

```

> If you want to track open rate / interactions see [tracking section](#tracking).

### Tracking

In addition to location updates, Indoorway allows event tracking. It can be button, notification clicks or anything. Just use `EventTrackingControl`:

`label`, `category`, `interaction` are optional and can be set to any value.

```java

IndoorwaySdk.getInstance()
    .getEventTrackingControl()
    .track(proximityEvent.getUuid(), "<label>", "<category>", "<interaction>");
```

> See [proximity communication](#proximity-communication) section for `proximityEvent` sample.

## Documentation

Full documentation will be available soon.  

> If you need more informations contact us at contact@indoorway.com.

## Support

If you want to contact us please send email at contact@indoorway.com. Any suggestions or reports of technical issues are welcome!

## License

Indoorway Android SDK is available under the custom license. See the LICENSE file for more info.