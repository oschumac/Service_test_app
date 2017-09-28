package com.example.admin.service_test_app.driver;

import java.nio.ByteBuffer;

/**
 * Callbacks for the Application-Layer
 */
public interface AppHandler {
    void log(String s);

    void connected();

    void rtModeActivated();

    void modeDeactivated();

    void addDisplayFrame(ByteBuffer b);

    void modeError();

    void sequenceError();

    void error(short error, String desc);

    void keySent(ByteBuffer b);
}
