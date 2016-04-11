package com.stevejonnunez.fpvdrone;

import android.content.Context;

import com.stevejonnunez.fpvdrone.util.dagger.DaggerApplication;

import java.util.Collections;
import java.util.List;

import dagger.ObjectGraph;

/**
 * Created by kryonex on 4/10/2016.
 */
public class FRVDroneMobileApplication extends DaggerApplication {
    private ObjectGraph objectGraph;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        objectGraph = ObjectGraph.create(getModules().toArray());
//        objectGraph.inject(this);
    }

    @Override
    public List<Object> getModules() {
        return Collections.singletonList(new FRVDroneMobileApplicationModule(this));
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return objectGraph;
    }

    @Override
    public ObjectGraph createScopedGraph(Object... modules) {
        return objectGraph.plus(modules);
    }
}
