package com.stevejonnunez.fpvdrone.ui;

import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.stevejonnunez.fpvdrone.R;
import com.stevejonnunez.fpvdrone.rxEvent.ListenerServiceEvent;
import com.stevejonnunez.fpvdrone.util.dagger.DaggerActivity;
import com.stevejonnunez.fpvdrone.util.rx.RxEventBus;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends DaggerActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private final String START_WEAR_ACTIVITY_MESSAGE = "/startWearActivity";
    private final String STOP_WEAR_ACTIVITY_MESSAGE = "/stopWearActivity";

    TextView accelorometerX;
    TextView accelorometerY;
    TextView accelorometerZ;
    TextView buttonPressed;

    @Inject
    @Named("ListenerServiceEventBus")
    RxEventBus<ListenerServiceEvent> rxListenerServiceEventBus;

    GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accelorometerX = (TextView) findViewById(R.id.accelerometerXData);
        accelorometerY = (TextView) findViewById(R.id.accelerometerYData);
        accelorometerZ = (TextView) findViewById(R.id.accelerometerZData);
        buttonPressed = (TextView) findViewById(R.id.wearButtonPressedData);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();

        sendMessageInThread(START_WEAR_ACTIVITY_MESSAGE);
    }

    @Override
    protected List<Object> getModules() {
        return null;
    }

    @Override
    protected void onStart() {
        rxListenerServiceEventBus.toObserverable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (event.getPath().equals(ListenerServiceEvent.ACCELEROMETER_X_WEAR_DATA)) {
                        accelorometerX.setText(event.getMessage());
                    } else if (event.getPath().equals(ListenerServiceEvent.ACCELEROMETER_Y_WEAR_DATA)) {
                        accelorometerY.setText(event.getMessage());
                    } else if (event.getPath().equals(ListenerServiceEvent.ACCELEROMETER_Z_WEAR_DATA)) {
                        accelorometerZ.setText(event.getMessage());
                    }
                });
        super.onStart();
    }

    @Override
    protected void onStop() {
        sendMessageInThread(STOP_WEAR_ACTIVITY_MESSAGE);
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void sendMessageInThread(String message) {
        Observable.just(message)
                .doOnNext(this::sendMessageToConnectedNodes)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    private void sendMessageToConnectedNodes(String message) {
        Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(nodes -> {
            for (Node node : nodes.getNodes()) {
                Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), message, null);
            }
        });
    }
}
