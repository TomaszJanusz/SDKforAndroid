package com.indoorway.android.example.usinglibrary;

import android.app.Application;

import com.indoorway.android.location.sdk.IndoorwayLocationSdk;
import com.indoorway.android.map.sdk.IndoorwayMapSdk;

/**
 * Application's class. Defined in AndroidManifest.xml
 * Initializes Indoorway sdk modules.
 */
public class CustomApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        String trafficApiKey = "<YOUR-API-KEY>";

        // sdk for using map view and getting map objects
        IndoorwayMapSdk.init(this, trafficApiKey);

        // sdk for indoor positioning
        IndoorwayLocationSdk.init(this, trafficApiKey);
    }

}