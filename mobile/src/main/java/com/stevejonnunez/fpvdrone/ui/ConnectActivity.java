package com.stevejonnunez.fpvdrone.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.parrot.freeflight.receivers.DroneConnectionChangeReceiverDelegate;
import com.parrot.freeflight.receivers.DroneConnectionChangedReceiver;
import com.parrot.freeflight.receivers.DroneReadyReceiver;
import com.parrot.freeflight.receivers.DroneReadyReceiverDelegate;
import com.parrot.freeflight.service.DroneControlService;
import com.stevejonnunez.fpvdrone.R;
import com.stevejonnunez.fpvdrone.ui.base.FPVDroneBaseActivity;
import com.stevejonnunez.sharedclasses.MessagePath;
import com.sveder.cardboardpassthrough.CardboardOverlayView;

/**
 * Created by kryonex on 4/12/2016.
 */
public class ConnectActivity extends FPVDroneBaseActivity
        implements ServiceConnection,
        DroneReadyReceiverDelegate,
        DroneConnectionChangeReceiverDelegate {
    CardboardView cardboardView;
    CardboardOverlayView cardboardOverlayView;

    DroneControlService droneControlService;
    BroadcastReceiver droneReadyReceiver;
    BroadcastReceiver droneConnectionChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupActivity();
        setupReceivers();
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (droneControlService != null)
            droneControlService.resume();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
        manager.registerReceiver(droneReadyReceiver, new IntentFilter(DroneControlService.DRONE_STATE_READY_ACTION));
        manager.registerReceiver(droneConnectionChangeReceiver, new IntentFilter(
                DroneControlService.DRONE_CONNECTION_CHANGED_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (droneControlService != null) {
            droneControlService.pause();
        }

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
        manager.unregisterReceiver(droneReadyReceiver);
        manager.unregisterReceiver(droneConnectionChangeReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(this);
    }

    @Override
    public void onDroneConnected() {
        droneControlService.requestConfigUpdate();
    }

    @Override
    public void onDroneDisconnected() {

    }

    @Override
    public void onDroneReady() {
        openDroneControlActivity();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        droneControlService = ((DroneControlService.LocalBinder) service).getService();

        droneControlService.resume();
        droneControlService.requestDroneStatus();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    private void setupActivity() {
        setContentView(R.layout.blank_cardboard_activity);
        cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardOverlayView = (CardboardOverlayView) findViewById(R.id.overlay);

        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        cardboardOverlayView.showText("Connecting to Drone...");
    }

    private void setupReceivers() {
        droneReadyReceiver = new DroneReadyReceiver(this);
        droneConnectionChangeReceiver = new DroneConnectionChangedReceiver(this);

        bindService(new Intent(this, DroneControlService.class), this, Context.BIND_AUTO_CREATE);
    }

    private void openDroneControlActivity() {
        cardboardOverlayView.showText("Drone connection success.");
        sendMessageInThread(MessagePath.DRONE_CONNECT_SUCCESS_MESSAGE_PATH);

        Intent intent = new Intent(this, DroneControlActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
