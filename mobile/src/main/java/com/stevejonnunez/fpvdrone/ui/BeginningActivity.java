package com.stevejonnunez.fpvdrone.ui;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;
import com.parrot.freeflight.tasks.CheckDroneNetworkAvailabilityTask;
import com.stevejonnunez.fpvdrone.R;
import com.stevejonnunez.fpvdrone.util.dagger.CardboardDaggerActivity;
import com.sveder.cardboardpassthrough.CardboardOverlayView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by kryonex on 4/11/2016.
 */
public class BeginningActivity
        extends CardboardDaggerActivity
        implements CardboardView.StereoRenderer,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    CardboardView cardboardView;
    CardboardOverlayView cardboardOverlayView;

    CheckDroneNetworkAvailabilityTask checkDroneConnectionTask;

    boolean droneOnNetwork = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.beginning_activity);
        cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardOverlayView = (CardboardOverlayView) findViewById(R.id.overlay);

        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        cardboardOverlayView.showText("Looking for drone...");

        Observable.interval(5, TimeUnit.SECONDS)
                .takeUntil(time -> droneOnNetwork)
                .doOnNext(time -> this.checkDroneConnectivity())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        time -> cardboardOverlayView.showText("Looking for drone..."),
                        throwable -> {},
                        () -> cardboardOverlayView.showText("Drone found\nPress \"Connect\" on your wear device.")
                );
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
    public void onNewFrame(HeadTransform headTransform) {

    }

    @Override
    public void onDrawEye(Eye eye) {

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {

    }

    @Override
    public void onRendererShutdown() {

    }

    @Override
    protected List<Object> getModules() {
        return null;
    }

    private void checkDroneConnectivity()
    {
        if (checkDroneConnectionTask != null && checkDroneConnectionTask.getStatus() != AsyncTask.Status.FINISHED) {
            checkDroneConnectionTask.cancel(true);
        }

        checkDroneConnectionTask = new CheckDroneNetworkAvailabilityTask() {

            @Override
            protected void onPostExecute(Boolean result) {
                droneOnNetwork = result;
            }

        };

        checkDroneConnectionTask.executeOnExecutor(CheckDroneNetworkAvailabilityTask.THREAD_POOL_EXECUTOR, this);
    }
}
