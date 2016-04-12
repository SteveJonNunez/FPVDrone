package com.stevejonnunez.sharedclasses;

/**
 * Created by kryonex on 4/10/2016.
 */
public class Message {
    String path;
    String message = null;

    public Message(String path) {
        this.path = path;
    }

    public Message(String path, String message) {
        this.path = path;
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }
}
