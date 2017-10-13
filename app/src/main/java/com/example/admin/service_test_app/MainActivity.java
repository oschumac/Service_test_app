package com.example.admin.service_test_app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.example.admin.service_test_app.Service.justatestService;
import com.example.admin.service_test_app.driver.Application;
import com.example.admin.service_test_app.driver.Utils;

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
                //testScreen(MainActivity.this);


                if (mjustatestService.isBTConnected()) {
                    // mjustatestService.sendPumpCommandMode();
                    mjustatestService.cmddeliverBolus(1.0);

                    Snackbar.make(view, "cmddeliverBolus(1.0)", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                } else {
                    if (mjustatestService.malsehen(MainActivity.this)) {
                        Snackbar.make(view, "Worker gestarted", Snackbar.LENGTH_LONG).setAction("Action", null).show();

                    } else {
                        Snackbar.make(view, "Worker gestoppt", Snackbar.LENGTH_LONG).setAction("Action", null).show();

                    }
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


    private void testScreen(Activity activity) {

        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        final EditText pinIn = new EditText(activity);
        pinIn.setGravity(Gravity.CENTER);
        pinIn.setInputType(InputType.TYPE_CLASS_NUMBER);
        // pinIn.setHint("XXX XXX XXXX");

        final AlertDialog AD = new AlertDialog.Builder(activity, R.style.CustomAlertDialog)

                            .setTitle("Enter Pin")
                            .setMessage("Read the Pin-Code from pump and enter it")
                            .setView(pinIn)
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Log.v("Enter Pin Screen", "cancel");
                                }
                            }
                            )
                            .setPositiveButton("Enter", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            String pin = pinIn.getText().toString();
                                            Log.v("testScreen", "Pin->" + pin + " ->" + pin.length() + " Zeichen");
                                        }
                                    }
                            ).show();

                        AD.getWindow().setLayout(400, 320);
                        pinIn.setFilters(new InputFilter[] { new InputFilter.LengthFilter(10) });
                        pinIn.setTextSize(30);
                        AD.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        pinIn.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {

                            }

                            @Override
                            public void afterTextChanged(Editable editable) {
                                String pin = pinIn.getText().toString();
                                if (pin.length()==10) {
                                    AD.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                } else {
                                    AD.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                }
                            }

                        });
    }
}



