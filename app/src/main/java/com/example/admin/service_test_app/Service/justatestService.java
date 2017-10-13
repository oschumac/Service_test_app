package com.example.admin.service_test_app.Service;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.admin.service_test_app.driver.Application;
import com.example.admin.service_test_app.driver.BTConnection;
import com.example.admin.service_test_app.driver.PumpData;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static android.content.ContentValues.TAG;
import static com.example.admin.service_test_app.driver.Application.cmdDeliverBolus;

public class justatestService extends Service {
    final String Tag = "justatestService";
    private final IBinder mBinder = new LocalBinder();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 1);
    private static boolean run_workerthread = false;
    private static boolean connectOnFocus = true;
    private static int count = 0;
    private Activity activity;
    private Pump Combopump = new Pump();

    public void  connectOnFocus(Boolean val) {
        connectOnFocus=val;
    }

    @Override
    public void onCreate () {
        Log.v(TAG,"(public justatestService())  onCreate()");

    }
    public void justatestService() {

        Log.v(TAG,"(public justatestService())  Keine ahnung was ich hier tun soll");
    }

    public boolean malsehen(Activity act) {
        Log.v(TAG,"(public justatestService())  mal sehen");

        if (act!=null) {
            activity = act;
        } else {
            return false;
        }

        if (run_workerthread==false) {
            run_workerthread=true;
            scheduler.execute(workerThread);
        } else {
            run_workerthread=false;
        }
        return run_workerthread;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public justatestService getService() {
            // Return this instance of LocalService so clients can call public methods
            return justatestService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG,"(public justatestService())  onBind");
        return mBinder;
    }

    public boolean workerrunning() {
        return run_workerthread;
    }

    private Runnable workerThread = new Runnable()
    {
        @Override
        public void run() {


            if (checkandpair()) {
                // Todo Ja dann los
                Log.v(TAG,"(public justatestService())  kleiner roter Traktor los geht!!");
                Combopump.Start(activity);
            }


            /*
            try {
                Thread.sleep(120000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.v(TAG, "(public justatestService())  testworker running");
            }
            */
            run_workerthread=false;
            Log.v(TAG, "(public justatestService())  testworker stopped");
        }
    };


    private boolean checkandpair() {
        PumpData pumpData = new PumpData(activity);

        PumpData data = pumpData.loadPump(activity);
        Pair pair = new Pair();
        Log.v(TAG, "(public justatestService())  testworker starting");


        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        boolean found = false;

        if (pumpData.isPumpBound(activity)) {
            try {
                for(BluetoothDevice bt : pairedDevices) {
                    if (bt.getAddress().equals(data.getPumpMac())) {
                        Log.v(TAG, "(public justatestService())  Found  -> " + bt.getAddress() + " Name -> " + bt.getName());
                        found = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        if (found==false) {
            Log.v(TAG, "(public justatestService())  no bounded Pump found start Pairing");
            pumpData.setAndSavePumpMac(null);
            pair.Start(activity);
            Log.v(TAG, "(public justatestService())  Pairing started()");


        }  else {
            return true;
        }

        data = pumpData.loadPump(activity);
        found =false;
        if (pumpData.isPumpBound(activity)) {
            try {
                for(BluetoothDevice bt : pairedDevices) {
                    if (bt.getAddress().equals(data.getPumpMac())) {
                        Log.v(TAG, "(public justatestService())  Found  -> " + bt.getAddress() + " Name -> " + bt.getName());
                        found = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        return found;
    };

    public void sendPumpCommandMode() {
        Combopump.sendPumpCommandMode();
    }
    public void sendPumpRTMode() {
        Combopump.sendPumpRTMode();
    }
    public boolean isBTConnected(){
        return Combopump.isBTConnected();

    }
    public void cmddeliverBolus(double bolus) {
        Combopump.cmdDeliverBolus(bolus);
    }


}
