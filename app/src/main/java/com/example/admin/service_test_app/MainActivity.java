package com.example.admin.service_test_app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.example.admin.service_test_app.Service.justatestService;

public class MainActivity extends AppCompatActivity {
    public justatestService mjustatestService;
    boolean mBound = false;
    String Tag = "MainActivity Service_test";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Log.v("MainActivity","oncreate");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {




            @Override
            public void onClick(View view) {
                if (mjustatestService.malsehen(MainActivity.this)) {
                    Snackbar.make(view, "Worker gestarted", Snackbar.LENGTH_LONG).setAction("Action", null).show();

                } else {
                    Snackbar.make(view, "Worker gestoppt", Snackbar.LENGTH_LONG).setAction("Action", null).show();

                }
            }
        });



        // connect_justatestService();
    }
    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        Log.v("MainActivity","onStop");
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v("MainActivity","onStart");
        // Bind to LocalService
        Intent intent = new Intent(this, justatestService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v("MainActivity","onDestroy");
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance

            Log.v("Main ","onServiceConnected ");
            justatestService.LocalBinder binder = (justatestService.LocalBinder) service;
            mjustatestService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.v("Main","onServiceDisconnected");
            mBound = false;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.v("Main","onBindingDied");

        }
    };


}
