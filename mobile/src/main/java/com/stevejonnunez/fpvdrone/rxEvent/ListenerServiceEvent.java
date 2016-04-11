package com.stevejonnunez.fpvdrone.rxEvent;

import com.google.android.gms.wearable.DataMap;

/**
 * Created by kryonex on 4/10/2016.
 */
public class ListenerServiceEvent {
    public static final String ACCELEROMETER_X_WEAR_DATA = "accelerometerXWearData";
    public static final String ACCELEROMETER_Y_WEAR_DATA = "accelerometerYWearData";
    public static final String ACCELEROMETER_Z_WEAR_DATA = "accelerometerZWearData";

    String path;
    String message;

    public ListenerServiceEvent(String path) {
        this.path = path;
    }

    public ListenerServiceEvent(String path, String message) {
        this.path = path;
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }
}
