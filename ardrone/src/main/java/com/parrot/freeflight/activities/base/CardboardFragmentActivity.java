package com.parrot.freeflight.activities.base;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.google.vrtoolkit.cardboard.CardboardDeviceParams;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.sensors.MagnetSensor;
import com.google.vrtoolkit.cardboard.sensors.NfcSensor;

/**
 * Created by kryonex on 4/8/2016.
 */
public class CardboardFragmentActivity extends FragmentActivity {

    private static final long NAVIGATION_BAR_TIMEOUT_MS = 2000L;

    private CardboardView mCardboardView;
    private MagnetSensor mMagnetSensor;
    private SensorListener sensorListener;
    private int mVolumeKeysMode;

    public CardboardFragmentActivity() {
        super();
        sensorListener = new SensorListener();
    }

    public void setCardboardView(final CardboardView cardboardView) {
        mCardboardView = cardboardView;
        if (cardboardView == null) {
            return;
        }
//        final NdefMessage tagContents = mNfcSensor.get();
//        if (tagContents != null) {
//            updateCardboardDeviceParams(CardboardDeviceParams.createFromNfcContents(tagContents));
//        }
    }

    public CardboardView getCardboardView() {
        return mCardboardView;
    }

    public NfcSensor getNfcSensor() {
        return null;
    }

    public void setVolumeKeysMode(final int mode) {
        mVolumeKeysMode = mode;
    }

    public int getVolumeKeysMode() {
        return mVolumeKeysMode;
    }

    public boolean areVolumeKeysDisabled() {
        switch (mVolumeKeysMode) {
            case VolumeKeys.NOT_DISABLED: {
                return false;
            }
            case VolumeKeys.DISABLED_WHILE_IN_CARDBOARD: {
                return false;
            }
            case VolumeKeys.DISABLED: {
                return true;
            }
            default: {
                throw new IllegalStateException(new StringBuilder()
                        .append("Invalid volume keys mode ")
                        .append(mVolumeKeysMode).toString()
                );
            }
        }
    }

    public void onInsertedIntoCardboard(final CardboardDeviceParams cardboardDeviceParams) {
        this.updateCardboardDeviceParams(cardboardDeviceParams);
    }

    public void onRemovedFromCardboard() {
    }

    public void onCardboardTrigger() {
    }

    protected void updateCardboardDeviceParams(final CardboardDeviceParams newParams) {
        if (mCardboardView != null) {
            mCardboardView.updateCardboardDeviceParams(newParams);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.ALPHA_CHANGED);

        mMagnetSensor = new MagnetSensor(this);
        mMagnetSensor.setOnCardboardTriggerListener(sensorListener);

        setVolumeKeysMode(VolumeKeys.DISABLED_WHILE_IN_CARDBOARD);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            final Handler handler = new Handler();
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                    new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(final int visibility) {
                            if ((visibility & 0x2) == 0x0) {
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CardboardFragmentActivity.this.setFullscreenMode();
                                    }
                                }, NAVIGATION_BAR_TIMEOUT_MS);
                            }
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCardboardView != null) {
            mCardboardView.onResume();
        }
        mMagnetSensor.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCardboardView != null) {
            mCardboardView.onPause();
        }
        mMagnetSensor.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void setContentView(final View view) {
        if (view instanceof CardboardView) {
            setCardboardView((CardboardView) view);
        }
        super.setContentView(view);
    }

    @Override
    public void setContentView(final View view, final ViewGroup.LayoutParams params) {
        if (view instanceof CardboardView) {
            setCardboardView((CardboardView) view);
        }
        super.setContentView(view, params);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        return ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                && areVolumeKeysDisabled()) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        return ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                && areVolumeKeysDisabled()) || super.onKeyUp(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setFullscreenMode();
        }
    }

    private void setFullscreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(5894);
    }

    public abstract static class VolumeKeys {
        public static final int NOT_DISABLED = 0;
        public static final int DISABLED = 1;
        public static final int DISABLED_WHILE_IN_CARDBOARD = 2;
    }

    private class SensorListener
            implements MagnetSensor.OnCardboardTriggerListener, NfcSensor.OnCardboardNfcListener{
        @Override
        public void onInsertedIntoCardboard(final CardboardDeviceParams deviceParams) {
            CardboardFragmentActivity.this.onInsertedIntoCardboard(deviceParams);
        }

        @Override
        public void onRemovedFromCardboard() {
            CardboardFragmentActivity.this.onRemovedFromCardboard();
        }

        @Override
        public void onCardboardTrigger() {
            CardboardFragmentActivity.this.onCardboardTrigger();
        }
    }
}