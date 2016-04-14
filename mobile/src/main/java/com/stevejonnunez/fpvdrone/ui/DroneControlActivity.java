package com.stevejonnunez.fpvdrone.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import com.golshadi.orientationSensor.sensors.Orientation;
import com.golshadi.orientationSensor.utils.OrientationSensorInterface;
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
        OrientationSensorInterface {
    GLTextureView glView1;
    GLTextureView glView2;
    VideoStageRenderer renderer;

    double baseAzimuth = 0;
    boolean flying = false;
    boolean getBase = false;

    boolean isRolling = false;
    boolean isPitching = false;

    Orientation orientationSensor;

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

        renderer = new VideoStageRenderer(this, null);

        bindService(new Intent(this, DroneControlService.class), this, Context.BIND_AUTO_CREATE);
        orientationSensor = new Orientation(this.getApplicationContext(), this);
        orientationSensor.init(1.0, 1.0, 1.0);
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
                    } else if (event.getPath().equals(ListenerServiceEvent.ACCELEROMETER_X_WEAR_DATA)) {
                        pitch(Float.valueOf(event.getMessage()));
                    } else if (event.getPath().equals(ListenerServiceEvent.ACCELEROMETER_Y_WEAR_DATA)) {
                        roll(Float.valueOf(event.getMessage()));
                    }
                });
        orientationSensor.on(2);
        super.onStart();
    }

    @Override
    protected void onStop() {
        orientationSensor.off();
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
    public void orientation(Double AZIMUTH, Double PITCH, Double ROLL) {
        if (getBase) {
            baseAzimuth = AZIMUTH;
            getBase = false;
        }
        altitude(ROLL);
        yaw(AZIMUTH);
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
            droneConfig.setOutdoorHull(true);

        }
    }

    private void triggerTakeOff() {
        if (droneControlService != null) {
            flying = !flying;
            if (flying)
                getBase = true;
            droneControlService.triggerTakeOff();
        }

    }

    private void roll(float val) {
        if (droneControlService != null && flying) {
            if (val < -6) {
                isRolling = true;
                setProgressiveCommand();
                droneControlService.setRoll(-1);
            } else if (val > 6) {
                isRolling = true;
                setProgressiveCommand();
                droneControlService.setRoll(1);
            } else {
                isRolling = false;
                setProgressiveCommand();
                droneControlService.setRoll(0);
            }
        }

    }

    private void pitch(float val) {
        if (droneControlService != null && flying) {
            if (val < -6f) {
                isPitching = true;
                setProgressiveCommand();
                droneControlService.setPitch(-1);
            } else if (val > 6f) {
                isPitching = true;
                setProgressiveCommand();
                droneControlService.setPitch(1);
            } else {
                isPitching = false;
                setProgressiveCommand();
                droneControlService.setPitch(0);
            }
        }
    }

    private void setProgressiveCommand() {
        if(isRolling || isPitching)
            droneControlService.setProgressiveCommandEnabled(true);
        else
            droneControlService.setProgressiveCommandEnabled(false);
    }

    private void altitude(double val) {
        if (droneControlService != null && flying) {
            if (val < -130) {
                droneControlService.setGaz(1);
            } else if (val > -50)
                droneControlService.setGaz(-1);
            else
                droneControlService.setGaz(0);
        }
    }

    private void yaw(double val) {
        if (droneControlService != null && !getBase && flying) {
            double realVal = modulosPositive(((180-baseAzimuth)+val),360.0);
            if (realVal < 180 - 50) {
                droneControlService.setYaw(-1);
            } else if (realVal > 180 + 50)
                droneControlService.setYaw(1);
            else
                droneControlService.setYaw(0);
        }
    }

    private double modulosPositive(double a, double b) {
        return (((a % b) + b) % b);
    }
}
