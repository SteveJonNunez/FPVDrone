package com.stevejonnunez.fpvdrone.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import com.parrot.freeflight.drone.DroneConfig;
import com.parrot.freeflight.service.DroneControlService;
import com.parrot.freeflight.ui.GLTextureView;
import com.parrot.freeflight.video.VideoStageRenderer;
import com.stevejonnunez.fpvdrone.R;
import com.stevejonnunez.fpvdrone.rxEvent.ListenerServiceEvent;
import com.stevejonnunez.fpvdrone.ui.base.FPVDroneBaseActivity;
import com.stevejonnunez.fpvdrone.util.rx.RxEventBus;
import com.stevejonnunez.sharedclasses.MessagePath;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import rx.android.schedulers.AndroidSchedulers;

/**
 * FPVDrone
 * Created by steven on 4/12/2016.
 */
public class DroneControlActivity extends FPVDroneBaseActivity
        implements ServiceConnection,
        SensorEventListener {
    GLTextureView glView1;
    GLTextureView glView2;
    VideoStageRenderer renderer;

    SensorManager sensorManager;
    Sensor sensorGyroscope;

    DroneControlService droneControlService;

    @Inject
    @Named("ListenerServiceEventBus")
    RxEventBus<ListenerServiceEvent> rxListenerServiceEventBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drone_control_activity);

        glView1 = (GLTextureView) findViewById(R.id.leftEye);
        glView2 = (GLTextureView) findViewById(R.id.rightEye);
        glView1.setEGLContextClientVersion(2);
        glView2.setEGLContextClientVersion(2);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        renderer = new VideoStageRenderer(this, null);

        bindService(new Intent(this, DroneControlService.class), this, Context.BIND_AUTO_CREATE);

        initGLTextureView();
    }

    @Override
    protected void onStart() {
        rxListenerServiceEventBus.toObserverable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (event.getPath().equals(ListenerServiceEvent.LAND_DRONE)) {
                        triggerTakeOff();
                    } else if (event.getPath().equals(ListenerServiceEvent.TAKEOFF_DRONE)) {
                        triggerTakeOff();
                    }
                });

        sensorManager.registerListener(this, sensorGyroscope, SensorManager.SENSOR_DELAY_GAME);
        super.onStart();
    }

    @Override
    protected void onStop() {
        sensorManager.unregisterListener(this, sensorGyroscope);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    protected List<Object> getModules() {
        return null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        droneControlService = ((DroneControlService.LocalBinder) service).getService();

        droneControlService.resume();
        droneControlService.requestDroneStatus();
        setDroneSettings();
        sendMessageInThread(MessagePath.DRONE_CONNECT_SUCCESS_MESSAGE_PATH);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;

        if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void initGLTextureView() {
        if (glView1 != null)
            glView1.setRenderer(renderer);
        if (glView2 != null)
            glView2.setRenderer(renderer);
    }

    private void setDroneSettings() {
        if (droneControlService != null) {
            DroneConfig droneConfig = droneControlService.getDroneConfig();
            droneConfig.setAltitudeLimit(DroneConfig.ALTITUDE_MIN);
            droneConfig.setVertSpeedMax(DroneConfig.VERT_SPEED_MIN);
            droneConfig.setYawSpeedMax(DroneConfig.YAW_MIN);
            droneConfig.setTilt(DroneConfig.TILT_MIN);
            droneConfig.setOutdoorFlight(false);
            droneConfig.setOutdoorHull(false);
        }
    }

    private void triggerTakeOff() {
        if (droneControlService != null) {
            droneControlService.triggerTakeOff();
        }
    }
}
