package com.stevejonnunez.fpvdrone.service;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.stevejonnunez.fpvdrone.rxEvent.ListenerServiceEvent;
import com.stevejonnunez.fpvdrone.util.dagger.DaggerApplication;
import com.stevejonnunez.fpvdrone.util.rx.RxEventBus;
import com.stevejonnunez.sharedclasses.MessagePath;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.ObjectGraph;

/**
 * Created by kryonex on 4/9/2016.
 */
public class ListenerService extends WearableListenerService {

    private ObjectGraph activityGraph;

    @Inject
    @Named("ListenerServiceEventBus")
    RxEventBus<ListenerServiceEvent> rxListenerServiceEventBus;

    @Override
    public void onCreate() {
        super.onCreate();
        activityGraph = ((DaggerApplication) getApplicationContext()).getObjectGraph();
        activityGraph.inject(this);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        if (messageEvent.getPath().equals(MessagePath.ACCELEROMETER_X_MESSAGE_PATH)) {
            String message = new String(messageEvent.getData());
            rxListenerServiceEventBus.send(new ListenerServiceEvent(ListenerServiceEvent.ACCELEROMETER_X_WEAR_DATA, message));
        } else if (messageEvent.getPath().equals(MessagePath.ACCELEROMETER_Y_MESSAGE_PATH)) {
            String message = new String(messageEvent.getData());
            rxListenerServiceEventBus.send(new ListenerServiceEvent(ListenerServiceEvent.ACCELEROMETER_Y_WEAR_DATA, message));
        } else if (messageEvent.getPath().equals(MessagePath.ACCELEROMETER_Z_MESSAGE_PATH)) {
            String message = new String(messageEvent.getData());
            rxListenerServiceEventBus.send(new ListenerServiceEvent(ListenerServiceEvent.ACCELEROMETER_Z_WEAR_DATA, message));
        } else if (messageEvent.getPath().equals(MessagePath.CONNECT_TO_DRONE_MESSAGE_PATH)) {
            rxListenerServiceEventBus.send(new ListenerServiceEvent(ListenerServiceEvent.CONNECT_TO_DRONE));
        } else if (messageEvent.getPath().equals(MessagePath.LAND_DRONE_MESSAGE_PATH)) {
            rxListenerServiceEventBus.send(new ListenerServiceEvent(ListenerServiceEvent.LAND_DRONE));
        } else if (messageEvent.getPath().equals(MessagePath.TAKEOFF_DRONE_MESSAGE_PATH)) {
            rxListenerServiceEventBus.send(new ListenerServiceEvent(ListenerServiceEvent.TAKEOFF_DRONE));
        }
    }
}
