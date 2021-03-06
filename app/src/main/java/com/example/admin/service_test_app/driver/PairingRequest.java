package com.example.admin.service_test_app.driver;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.lang.reflect.Method;

class PairingRequest extends BroadcastReceiver {
    private final Activity activity;
    private final BTHandler handler;

    public PairingRequest(final Activity activity, final BTHandler handler)
        {
            super();
            this.activity = activity;
            this.handler = handler;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
                try {
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try
                            {
                                byte[] pinBytes;
                                pinBytes = ("}gZ='GD?gj2r|B}>").getBytes("UTF-8");

                                handler.log( "Try to set the PIN");
                                device.setPin(pinBytes);
                                // Method m = device.getClass().getMethod("setPin", byte[].class);
                                // m.invoke(device, pinBytes);
                                handler.log("Success to add the PIN.");

                                int loop=3;
                                boolean bonded=false;
                                while (loop>0 && !bonded) {
                                    try {
                                        //m = device.getClass().getMethod("createBond");
                                        //m.invoke(device);
                                        device.createBond();
                                        bonded=true;
                                    } catch (Exception e) {
                                        handler.log("No Success to start bond.");
                                        e.printStackTrace();
                                        Thread.sleep(100);
                                        loop--;
                                    }
                                    loop=0;
                                }

                                loop=3;
                                Boolean Pairconfirm=false;
                                while (loop>3 && !Pairconfirm) {
                                    try {
                                        device.setPairingConfirmation(true);
                                        Pairconfirm=true;
                                        // device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
                                        handler.log( "Success to setPairingConfirmation.");
                                    } catch (Exception e) {
                                        handler.log( "No Success to setPairingConfirmation.");
                                        e.printStackTrace();
                                        Thread.sleep(100);
                                        loop--;
                                    }
                                    loop=0;

                                }
                            }catch(Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }