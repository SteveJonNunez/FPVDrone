package com.stevejonnunez.fpvdrone.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import com.golshadi.orientationSensor.sensors.Orientation;
import com.golshadi.orientationSensor.utils.OrientationSensorInterface;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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
        GoogleApiClient.OnConnectionFailedListener,
        SensorEventListener,
        OrientationSensorInterface {
    private final String START_WEAR_ACTIVITY_MESSAGE = "/startWearActivity";
    private final String STOP_WEAR_ACTIVITY_MESSAGE = "/stopWearActivity";

    TextView accelorometerX;
    TextView accelorometerY;
    TextView accelorometerZ;

    TextView gyroscopeX;
    TextView gyroscopeY;
    TextView gyroscopeZ;

    TextView buttonPressed;

    @Inject
    @Named("ListenerServiceEventBus")
    RxEventBus<ListenerServiceEvent> rxListenerServiceEventBus;

    GoogleApiClient googleApiClient;

    SensorManager sensorManager;
    Sensor sensorGyroscope;
    private Orientation orientationSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accelorometerX = (TextView) findViewById(R.id.accelerometerXData);
        accelorometerY = (TextView) findViewById(R.id.accelerometerYData);
        accelorometerZ = (TextView) findViewById(R.id.accelerometerZData);


        gyroscopeX = (TextView) findViewById(R.id.gyroscopeXData);
        gyroscopeY = (TextView) findViewById(R.id.gyroscopeYData);
        gyroscopeZ = (TextView) findViewById(R.id.gyroscopeZData);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        orientationSensor = new Orientation(this.getApplicationContext(), this);
        orientationSensor.init(1.0, 1.0, 1.0);

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
        sensorManager.registerListener(this, sensorGyroscope, SensorManager.SENSOR_DELAY_GAME);
        orientationSensor.on(2);
        super.onStart();
    }

    @Override
    protected void onStop() {
        sendMessageInThread(STOP_WEAR_ACTIVITY_MESSAGE);
        sensorManager.unregisterListener(this, sensorGyroscope);
        orientationSensor.off();
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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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

    @Override
    public void orientation(Double AZIMUTH, Double PITCH, Double ROLL) {
        gyroscopeX.setText(AZIMUTH+"");
        gyroscopeY.setText(PITCH+"");
        gyroscopeZ.setText(ROLL+"");
    }
}
