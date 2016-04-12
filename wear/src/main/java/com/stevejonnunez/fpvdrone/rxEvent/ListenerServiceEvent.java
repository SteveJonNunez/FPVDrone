package com.stevejonnunez.fpvdrone.rxEvent;

/**
 * Created by kryonex on 4/10/2016.
 */
public class ListenerServiceEvent {
    public static final String STOP_ACTIVITY = "stopActivity";
    public static final String DRONE_FOUND = "droneFound";
    public static final String DRONE_CONNECTED = "droneConnected";

    String message;

    public ListenerServiceEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
