package com.stevejonnunez.fpvdrone.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.parrot.freeflight.drone.DroneConfig;
import com.parrot.freeflight.service.DroneControlService;
import com.parrot.freeflight.ui.GLSurfaceToTextureView;
import com.parrot.freeflight.video.VideoStageRenderer;
import com.stevejonnunez.fpvdrone.R;

/**
 * FPVDrone
 * Created by steven on 4/12/2016.
 */
public class DroneControlActivity extends Activity
        implements ServiceConnection {
    GLSurfaceToTextureView glView1;
    GLSurfaceToTextureView glView2;
    VideoStageRenderer renderer;

    DroneControlService droneControlService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drone_control_activity);

        glView1 = (GLSurfaceToTextureView) findViewById(R.id.leftEye);
        glView2 = (GLSurfaceToTextureView) findViewById(R.id.rightEye);
        glView1.setEGLContextClientVersion(2);
        glView2.setEGLContextClientVersion(2);

        renderer = new VideoStageRenderer(this, null);

        initGLTextureView();
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
