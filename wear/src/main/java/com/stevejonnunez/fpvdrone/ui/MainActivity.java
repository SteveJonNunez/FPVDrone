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
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends DaggerActivity
        implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks,
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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (event.getMessage().equals(ListenerServiceEvent.STOP_ACTIVITY))
                        finish();
                    else if (event.getMessage().equals(ListenerServiceEvent.DRONE_FOUND))
                        button.setEnabled(true);
                    else if (event.getMessage().equals(ListenerServiceEvent.DRONE_CONNECTED)) {
                        isConnected = true;
                        button.setEnabled(true);
                        setButtonToTakeOff();
                    }
                });
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        super.onStart();

    }

    @Override
    protected void onStop() {
        sensorManager.unregisterListener(this, sensorAccelerometer);
        super.onStop();
    }

    @Override
    protected List<Object> getModules() {
        return null;
    }

    public void buttonPressed(View view) {
        if (!isConnected) {
            setButtonToConnect();
        } else if (!isFlying) {
            setButtonToLand();
            sendTakeOffCommandToDrone();
        } else {
            setButtonToTakeOff();
            sendLandCommandToDrone();
        }
    }

    private void sendTakeOffCommandToDrone() {
        sendMessageInThread(MessagePath.TAKEOFF_DRONE_MESSAGE_PATH);
    }

    private void sendLandCommandToDrone() {
        sendMessageInThread(MessagePath.LAND_DRONE_MESSAGE_PATH);
    }

    private void setButtonToConnect() {
        button.setText("Connecting");
        button.setEnabled(false);
        sendMessageInThread(new Message(MessagePath.CONNECT_TO_DRONE_MESSAGE_PATH));
    }

    private void setButtonToLand() {
        isFlying = true;
        button.setBackgroundResource(R.drawable.land_button);
        button.setText("Land");
    }

    private void setButtonToTakeOff() {
        isFlying = false;
        button.setBackgroundResource(R.drawable.takeoff_button);
        button.setText("Takeoff");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER && isConnected) {
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
                }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe();
    }

    private void sendMessageInThread(String messagePath) {
        sendMessageInThread(new Message(messagePath));
    }

    private void sendMessageInThread(Message message) {
        Observable.just(message)
                .doOnNext(this::sendMessageToConnectedNodes)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe();
    }

    private void sendMessageToConnectedNodes(Message message) {
        String path = message.getPath();
        byte[] messageBytes = message.getMessage() == null ? null : message.getMessage().getBytes();
        Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(nodes -> {
            for (Node node : nodes.getNodes()) {
                Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, messageBytes);
            }
        });
    }
}
