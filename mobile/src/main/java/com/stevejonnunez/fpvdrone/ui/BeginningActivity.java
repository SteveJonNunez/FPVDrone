package com.stevejonnunez.fpvdrone.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.parrot.freeflight.tasks.CheckDroneNetworkAvailabilityTask;
import com.stevejonnunez.fpvdrone.R;
import com.stevejonnunez.fpvdrone.rxEvent.ListenerServiceEvent;
import com.stevejonnunez.fpvdrone.ui.base.FPVDroneBaseActivity;
import com.stevejonnunez.fpvdrone.util.rx.RxEventBus;
import com.stevejonnunez.sharedclasses.MessagePath;
import com.sveder.cardboardpassthrough.CardboardOverlayView;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by kryonex on 4/11/2016.
 */
public class BeginningActivity
        extends FPVDroneBaseActivity {

    CardboardView cardboardView;
    CardboardOverlayView cardboardOverlayView;

    CheckDroneNetworkAvailabilityTask checkDroneConnectionTask;

    boolean droneOnNetwork = false;

    @Inject
    @Named("ListenerServiceEventBus")
    RxEventBus<ListenerServiceEvent> rxListenerServiceEventBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupActivity();
        setupDroneConnectionChecker();
        sendMessageInThread(MessagePath.START_WEAR_ACTIVITY_MESSAGE_PATH);
    }

    @Override
    protected void onStart() {
        rxListenerServiceEventBus.toObserverable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (event.getPath().equals(ListenerServiceEvent.CONNECT_TO_DRONE)) {
                        connectToDrone();
                    }
                });
        super.onStart();
    }

    private void setupActivity() {
        setContentView(R.layout.blank_cardboard_activity);
        cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardOverlayView = (CardboardOverlayView) findViewById(R.id.overlay);

        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        cardboardOverlayView.showText("Looking for drone...");
    }

    private void setupDroneConnectionChecker() {
        Observable.interval(5, TimeUnit.SECONDS)
                .takeUntil(time -> droneOnNetwork)
                .doOnNext(time -> this.checkDroneConnectivity())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        time -> cardboardOverlayView.showText("Looking for drone..."),
                        throwable -> {
                        },
                        () -> {
                            cardboardOverlayView.showText("Drone found\nPress \"Connect\" on your wear device.");
                            sendMessageInThread(MessagePath.DRONE_FOUND_MESSAGE_PATH);
                        }
                );
    }

    private void checkDroneConnectivity() {
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

    private void connectToDrone() {
        Intent intent = new Intent(this, ConnectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
