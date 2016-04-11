package com.stevejonnunez.fpvdrone.util.dagger;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.wearable.activity.WearableActivity;

import java.util.List;

import dagger.ObjectGraph;

/**
 * MicAndroidClient
 * Created by steven on 2/26/2015.
 */
public abstract class DaggerActivity extends WearableActivity {
    private ObjectGraph activityGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<Object> moduleList = getModules();
        if (moduleList != null) {
            activityGraph = ((DaggerApplication) getApplication()).createScopedGraph(getModules().toArray());
            activityGraph.inject(this);
        } else {
            activityGraph = ((DaggerApplication) getApplication()).getObjectGraph();
            activityGraph.inject(this);
        }
    }

    @Override
    protected void onDestroy() {
        activityGraph = null;
        super.onDestroy();
    }

    protected abstract List<Object> getModules();
}