package com.example.admin.service_test_app.Service;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static android.content.ContentValues.TAG;

public class justatestService extends Service {
    final String Tag = "justatestService";
    private final IBinder mBinder = new LocalBinder();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 1);
    private static boolean run_workerthread = false;
    private static boolean connectOnFocus = true;
    private static int count = 0;
    private Activity activity;

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
            Pair pair = new Pair();
            Log.v(TAG, "(public justatestService())  testworker starting");


            Log.v(TAG, "(public justatestService())  testworker running -> " +count);
            pair.Start(activity);

            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.v(TAG, "(public justatestService())  testworker running");
            }

            run_workerthread=false;

            Log.v(TAG, "(public justatestService())  testworker stopped");

        }
    };
}
