package com.stevejonnunez.fpvdrone.service;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.stevejonnunez.fpvdrone.rxEvent.ListenerServiceEvent;
import com.stevejonnunez.fpvdrone.ui.MainActivity;
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

        if (messageEvent.getPath().equals(MessagePath.START_WEAR_ACTIVITY_MESSAGE_PATH)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (messageEvent.getPath().equals(MessagePath.STOP_WEAR_ACTIVITY_MESSAGE_PATH)) {
            rxListenerServiceEventBus.send(new ListenerServiceEvent(ListenerServiceEvent.STOP_ACTIVITY));
        }
    }
}
