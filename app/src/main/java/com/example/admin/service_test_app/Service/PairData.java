package com.example.admin.service_test_app.Service;

import android.util.Log;

/**
 * Created by Admin on 26.09.2017.
 */

public class PairData {
    private static byte[] pin;
    private static Boolean pinokay;

public byte[] getPin() {
    Log.v("PairData", "getPin->"+pin);
    return pin;
}

public void setPin(byte[] val) {
    Log.v("PairData", "setPin->"+val);
    pin = val;
}
public boolean getPinokay() {
    Log.v("PairData", "getPinokay->"+pinokay);
    return pinokay;
}

public void setPinokay(boolean val) {
    Log.v("PairData", "setPinokay->"+val);
    pinokay = val;
}


}
