package com.stevejonnunez.fpvdrone.util.dagger;

import android.app.Application;

import java.util.List;

import dagger.ObjectGraph;

/**
 * MicAndroidClient
 * Created by steven on 2/26/2015.
 */
public abstract class DaggerApplication extends Application {
    public abstract List<Object> getModules();

    public abstract ObjectGraph getObjectGraph();

    public abstract ObjectGraph createScopedGraph(Object... modules);
}
