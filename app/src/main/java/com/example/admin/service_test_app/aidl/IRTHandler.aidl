// IRTHandler.aidl
package com.example.admin.service_test_app.aidl;

// Declare any non-default types here with import statements

import com.example.admin.service_test_app.aidl.Menu;

interface IRTHandler {
    void log(String message);
    void fail(String message);

    void requestBluetooth();
    boolean canDisconnect();
    void rtStopped();
    void rtStarted();

    void rtClearDisplay();
    void rtUpdateDisplay(in byte[] quarter, int which);

    void rtDisplayHandleMenu(in Menu menu, in int sequence);
    void rtDisplayHandleNoMenu(in int sequence);

    void keySent(in int sequence);

    String getServiceIdentifier();
}
