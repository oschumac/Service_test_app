// IRuffyService.aidl
package com.example.admin.service_test_app.aidl;

// Declare any non-default types here with import statements
import com.example.admin.service_test_app.aidl.IRTHandler;

interface IRuffyService {

    void addHandler(IRTHandler handler);
    void removeHandler(IRTHandler handler);

    /** Connect to the pump
    *
    * @return 0 if successful, -1 otherwise
    */
    int doRTConnect(IRTHandler handler);

    /** Disconnect from the pump */
    void doRTDisconnect(IRTHandler handler);

    /*What's the meaning of 'changed'?
     * changed means if a button state has been changed, like btton pressed is a change and button release another*/
    void rtSendKey(byte keyCode, boolean changed);
    void resetPairing();
    boolean isConnected();
    boolean isBoundToPump();
}
