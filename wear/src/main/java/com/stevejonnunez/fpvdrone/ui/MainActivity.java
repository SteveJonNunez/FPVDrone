package com.stevejonnunez.fpvdrone.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.stevejonnunez.fpvdrone.R;
import com.stevejonnunez.fpvdrone.rxEvent.ListenerServiceEvent;
import com.stevejonnunez.fpvdrone.util.dagger.DaggerActivity;
import com.stevejonnunez.fpvdrone.util.rx.RxEventBus;
import com.stevejonnunez.sharedclasses.Message;
import com.stevejonnunez.sharedclasses.MessagePath;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.schedulers.Schedulers;

public class MainActivity extends DaggerActivity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    Button button;

    boolean isConnected = false;
    boolean isFlying = false;

    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;

    int x = 0;
    int y = 0;
    int z = 0;

    GoogleApiClient googleApiClient;

    @Inject
    @Named("ListenerServiceEventBus")
    RxEventBus<ListenerServiceEvent> rxListenerServiceEventBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.button);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    protected void onStart() {
        rxListenerServiceEventBus.toObserverable()
                .subscribe(event -> {
                    if (event.getMessage().equals(ListenerServiceEvent.STOP_ACTIVITY))
                        finish();
                });
        super.onStart();

        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this, sensorAccelerometer);
    }

    @Override
    protected List<Object> getModules() {
        return null;
    }

    public void buttonPressed(View view) {
        if (!isConnected) {
            isConnected = true;
            button.setBackgroundResource(R.drawable.takeoff_button);
            button.setText("Takeoff");
        } else if (!isFlying) {
            isFlying = true;
            button.setBackgroundResource(R.drawable.land_button);
            button.setText("Land");
        } else {
            isFlying = false;
            button.setBackgroundResource(R.drawable.takeoff_button);
            button.setText("Takeoff");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sendAccelerometerDataMessageInThread(sensorEvent);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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

    private void sendAccelerometerDataMessageInThread(SensorEvent sensorEvent) {
        Observable.just(sensorEvent)
                .doOnNext(sensorEvent1 -> {
                    float valueX = sensorEvent.values[0];
                    float valueY = sensorEvent.values[1];
                    float valueZ = sensorEvent.values[2];

                    int absValueX = Math.round(valueX);
                    if (Math.abs(absValueX - x) >= 1) {
                        x = absValueX;
                        sendMessageInThread(new Message(MessagePath.ACCELEROMETER_X_MESSAGE_PATH, String.valueOf(valueX)));
                    }
                    int absValueY = Math.round(valueY);
                    if (Math.abs(absValueY - y) >= 1) {
                        y = absValueY;
                        sendMessageInThread(new Message(MessagePath.ACCELEROMETER_Y_MESSAGE_PATH, String.valueOf(valueY)));
                    }
                    int absValueZ = Math.round(valueZ);
                    if (Math.abs(absValueZ - z) >= 1) {
                        z = absValueZ;
                        sendMessageInThread(new Message(MessagePath.ACCELEROMETER_Z_MESSAGE_PATH, String.valueOf(valueZ)));
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe();
    }

    private void sendMessageInThread(Message message) {
        Observable.just(message)
                .doOnNext(this::sendMessageToConnectedNodes)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe();
    }

    private void sendMessageToConnectedNodes(Message message) {
        Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(nodes -> {
            for (Node node : nodes.getNodes()) {
                Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), message.getPath(), message.getMessage().getBytes());
            }
        });
    }
}
