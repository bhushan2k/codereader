package com.demo.yelp.locationUtils;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


/**
 * stand alone component for location updates
 */
public class LocationUpdatesComponent {
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final String TAG = LocationUpdatesComponent.class.getSimpleName();
    Context mContext;
    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2 * 1000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;

    /**
     * The current location.
     */
    private Location mLocation = null;

    public ILocationProvider iLocationProvider;

    public LocationUpdatesComponent(ILocationProvider iLocationProvider) {
        this.iLocationProvider = iLocationProvider;
    }

    /**
     * create first time to initialize the location components
     *
     * @param context
     */
    public void onCreate(Context context) {
        this.mContext = context;
        Log.i(TAG, "created...............");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.i(TAG, "onCreate...onLocationResult");
                if (locationResult.getLastLocation() != null) {
                    Log.i(TAG, "onCreate...onLocationResult...............loc " + locationResult.getLastLocation());
                    mLocation = locationResult.getLastLocation();
                    onNewLocation(locationResult.getLastLocation());
                }
//                else {
//                    Log.i(TAG, "onCreate...onLocationResult null");
////                    getLastLocation();
//                    onNewLocation(null);
//                }
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
            }
        };
        // create location request
        createLocationRequest();
        // get last known location
//        getLastLocation();
    }

    /**
     * start location updates
     */
    public void onStart() {
//        Log.i(TAG, "onStart ");
        //hey request for location updates
        requestLocationUpdates();
    }

    /**
     * remove location updates
     */
    public void onStop() {
//        Log.i(TAG, "onStop....");
        removeLocationUpdates();
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
//        Log.i(TAG, "Requesting location updates");
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.getMainLooper());
//            checkLocationStatus();
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    private void checkLocationStatus() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mLocation != null) {
                    onNewLocation(mLocation);
                } else {
                    getLastLocation();
                }
            }
        }, 10000);
    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
//        Log.i(TAG, "Removing location updates");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
//            Utils.setRequestingLocationUpdates(this, false);
//            stopSelf();
        } catch (SecurityException unlikely) {
//            Utils.setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    /**
     * get last location
     */
    private void getLastLocation() {
        try {
//            Log.i(TAG, "getting lastknown");
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.getResult() != null) {
//                                Log.i(TAG, "last known: " + task.getResult());
                                onNewLocation(task.getResult());
                            } else {
//                                Log.i(TAG, "last known null");
                                onNewLocation(null);
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
            onNewLocation(null);
        }
    }

    /**
     * set location for interface method
     */
    private void onNewLocation(Location location) {
//        Log.i(TAG, "New location: " + location);
        if (this.iLocationProvider != null) {
            this.iLocationProvider.onLocationUpdate(location);
        }
    }

    /**
     * Sets the location request parameters.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * implements this interface to get call back of location changes
     */
    public interface ILocationProvider {
        void onLocationUpdate(Location location);
    }
}