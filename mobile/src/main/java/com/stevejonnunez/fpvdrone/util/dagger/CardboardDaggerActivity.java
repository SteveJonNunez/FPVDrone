package com.stevejonnunez.fpvdrone.util.dagger;

import android.os.Bundle;

import com.google.vrtoolkit.cardboard.CardboardActivity;

import java.util.List;

import dagger.ObjectGraph;

/**
 * Created by kryonex on 4/11/2016.
 */
public abstract class CardboardDaggerActivity extends CardboardActivity {
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
