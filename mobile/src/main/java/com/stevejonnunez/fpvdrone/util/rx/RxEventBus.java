package com.stevejonnunez.fpvdrone.util.rx;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * MicAndroidClient
 * Created by steven on 5/4/2015.
 */
public class RxEventBus<T> {
    private final Subject<T, T> eventBus = new SerializedSubject<>(PublishSubject.create());

    public void send(T o) {
        eventBus.onNext(o);
    }

    public boolean hasObservers() {
        return eventBus.hasObservers();
    }

    public Observable<T> toObserverable() {
        return eventBus;
    }
}
