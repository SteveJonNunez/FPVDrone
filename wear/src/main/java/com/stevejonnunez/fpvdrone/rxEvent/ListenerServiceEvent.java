package com.stevejonnunez.fpvdrone.rxEvent;

/**
 * Created by kryonex on 4/10/2016.
 */
public class ListenerServiceEvent {
    public static final String STOP_ACTIVITY = "stopActivity";

    String message;

    public ListenerServiceEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
