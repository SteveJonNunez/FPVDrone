package com.stevejonnunez.fpvdrone.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import com.parrot.freeflight.drone.DroneConfig;
import com.parrot.freeflight.service.DroneControlService;
import com.parrot.freeflight.ui.GLTextureView;
import com.parrot.freeflight.video.VideoStageRenderer;
import com.stevejonnunez.fpvdrone.R;

/**
 * FPVDrone
 * Created by steven on 4/12/2016.
 */
public class DroneControlActivity extends Activity
        implements ServiceConnection {
    GLTextureView glView1;
    GLTextureView glView2;
    VideoStageRenderer renderer;

    DroneControlService droneControlService;

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

        initGLTextureView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        droneControlService = ((DroneControlService.LocalBinder) service).getService();

        droneControlService.resume();
        droneControlService.requestDroneStatus();
        setDroneSettings();
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
