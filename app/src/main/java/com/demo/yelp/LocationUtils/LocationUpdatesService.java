package com.demo.yelp.LocationUtils;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * location update service continues to running and getting location information
 */
@SuppressLint("SpecifyJobSchedulerIdRange")
public class LocationUpdatesService extends JobService implements LocationUpdatesComponent.ILocationProvider{
    private static final String TAG = LocationUpdatesService.class.getSimpleName();
    public static final int LOCATION_MESSAGE = 9999;
    private Messenger mActivityMessenger;
    private LocationUpdatesComponent locationUpdatesComponent;
    public LocationUpdatesService() {}

    @Override
    public boolean onStartJob(JobParameters params) {
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        locationUpdatesComponent.onStop();
        return false;
    }

    /**
     * initialise location class
     */
    @Override
    public void onCreate() {
        super.onCreate();
//        Log.i(TAG, "onCreate");
        locationUpdatesComponent = new LocationUpdatesComponent(this);
        locationUpdatesComponent.onCreate(this);
    }

    /**
     * start getting location updates
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mActivityMessenger = intent.getParcelableExtra(ConstantsKt.MESSENGER_INTENT_KEY);
        }
        locationUpdatesComponent.onStart();
        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    /**
     * stop getting location updates and
     * destroy the service after usage
     */
    @Override
    public void onDestroy() {
//        Log.i(TAG, "Stopping Location Service");
        locationUpdatesComponent.onStop();
        stopSelf();
    }

    @Override
    public void onLocationUpdate(Location location) {
        sendMessage(location);
    }

    /**
     * Send location to the calling activity
     * @param location location object
     */
    private void sendMessage(Location location) {
        if (mActivityMessenger == null) {
            return;
        }
        Message m = Message.obtain();
        m.what = LocationUpdatesService.LOCATION_MESSAGE;
        m.obj = location;
        try {
            mActivityMessenger.send(m);
        } catch (RemoteException e) {
            Log.e(TAG, "Error passing service object back to activity.");
        }
    }
}