package com.example.admin.service_test_app.Service;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.admin.service_test_app.R;
import com.example.admin.service_test_app.driver.Application;
import com.example.admin.service_test_app.driver.BTConnection;
import com.example.admin.service_test_app.driver.BTHandler;
import com.example.admin.service_test_app.driver.Packet;
import com.example.admin.service_test_app.driver.Protocol;
import com.example.admin.service_test_app.driver.Twofish_Algorithm;
import com.example.admin.service_test_app.driver.Utils;
import com.example.admin.service_test_app.ruffy.Frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.button;
import static java.security.AccessController.getContext;

/**
 * Created by Admin on 26.09.2017.
 */

public class Pair {
    private TextView connectLog;
    private BTConnection btConn;
    private int step = 0;
    private BluetoothDevice pairingDevice;
    private byte[] pin_nöh;
    private Activity activity;
    private PairData Pdata = new PairData();


    public void Start(Activity act) {
        Log.v("Pair.Start()","Starting rfcomm to wait for Pump connection…");

        activity=act;

        btConn = new BTConnection(new BTHandler() {
            BluetoothDevice device;

            @Override
            public void deviceConnected() {
                appendLog("connected to device ");
                pairingDevice = device;
                appendLog("initiate pairing…");
                //FIXME move
                byte[] key = {16,9,2,0,-16};
                step = 1;
                btConn.writeCommand(key);
            }

            @Override
            public void log(String s) {
                if(step == 200 && s.equals("got error in read"))
                {
                    return;
                }
                appendLog(s);
            }

            @Override
            public void fail(String s) {
                appendLog(s);
                if(step == 1)//trying to connect
                {
                    appendLog("retrying to connect!");
                    btConn.connect(pairingDevice);
                }
            }

            @Override
            public void deviceFound(BluetoothDevice device) {
                if (this.device == null) {
                    this.device = device;
                    appendLog("found device first time1 " + device + " waiting for next");

                } else if (this.device.getAddress().equals(device.getAddress())) {
                    pairingDevice = device;
                    btConn.connect(device);
                } else {
                    this.device = device;
                    appendLog("found device first time2 " + device + " waiting for next");
                }
            }

            @Override
            public void handleRawData(byte[] buffer, int bytes) {
                handleData(buffer,bytes);
            }

            @Override
            public void requestBlueTooth() {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, 1);
            }
        });
        btConn.makeDiscoverable(activity);
    }

    private void appendLog(final String message) {
        Log.v("Pair", message);
    }

    public void handleRX(byte[] inBuf, int length, boolean rel) {

        ByteBuffer buffer = ByteBuffer.wrap(inBuf, 0, length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);


        ByteBuffer pBuf;

        byte[] nonce, payload, umac, packetNoUmac;

        Byte command, addresses;
        buffer.get();
        command = (byte)(buffer.get() & 0x1F);

        short payloadlength = buffer.getShort();

        addresses = buffer.get();

        nonce = new byte[13];                            //Copy buffers for nonce
        buffer.get(nonce, 0, nonce.length);

        payload = new byte[payloadlength];                        //Payload
        buffer.get(payload, 0, payload.length);
        pBuf = ByteBuffer.wrap(payload);

        umac = new byte[8];                                //U-MAC
        buffer.get(umac, 0, umac.length);

        packetNoUmac = new byte[buffer.capacity() - umac.length];
        buffer.rewind();
        for (int i = 0; i < packetNoUmac.length; i++)
            packetNoUmac[i] = buffer.get();

        byte c = (byte)(command & 0x1F);
        switch (c) {
            case 0x11://key response?
                appendLog("got an key response");
                try {
                    Object tf = Twofish_Algorithm.makeKey(Pdata.getPin());
                    btConn.getPumpData().setAndSaveAddress((byte) ((addresses << 4) & 0xF0));        //Get the address and reverse it since source and destination are reversed from the RX packet

                    byte[] key_pd = new byte[16];                            //Get the bytes for the keys
                    byte[] key_dp = new byte[16];

                    pBuf.rewind();
                    pBuf.get(key_pd, 0, key_pd.length);
                    pBuf.get(key_dp, 0, key_dp.length);

                    String d = "";
                    for (byte b : key_pd)
                        d += String.format("%02X ", b);
                    appendLog("parseRx >>> Key_PD: " + d);

                    d = "";
                    for (byte b : key_dp)
                        d += String.format("%02X ", b);
                    appendLog("parseRx >>> Key_DP: " + d);



                    btConn.getPumpData().setAndSaveToDeviceKey(key_pd,tf);
                    btConn.getPumpData().setAndSaveToPumpKey(key_dp,tf);
                    btConn.getPumpData().setAndSavePumpMac(pairingDevice.getAddress());
                    Protocol.sendIDReq(btConn);
                } catch (Exception e) {
                    e.printStackTrace();
                    appendLog("failed inRX: " + e.getMessage());
                }
                break;
            case 20:
                appendLog("got an id response");
                byte[] device = new byte[13];

                pBuf.order(ByteOrder.LITTLE_ENDIAN);
                int serverId = pBuf.getInt();
                pBuf.get(device);
                String deviceId = new String(device);

                appendLog("Server ID: " + String.format("%X", serverId) + " Device ID: " + deviceId);

                try {
                    Protocol.sendSyn(btConn);
                    appendLog("send Syn!");
                }catch(Exception e) {
                    e.printStackTrace();
                    appendLog("failed to send Syn!");
                }

                break;
            case 24:
                appendLog("got a sync response ");
                btConn.seqNo = 0x00;

                appendLog("Sequence Number reset!");
                appendLog("parseRx >>> Sending APP_SEND_CONNECT!");

                if(step<100)
                    Application.sendAppConnect(btConn);
                else
                {
                    Application.sendAppDisconnect(btConn);
                    step=200;
                }
                break;

            case 0x23: //recieved reliable data/
                appendLog("recieved reliable data");
            case 0x03: //recieve unreliable data
                appendLog("recieved unreliable data");
                if (Utils.ccmVerify(packetNoUmac, btConn.getPumpData().getToDeviceKey(), umac, nonce)) {
                    appendLog("recieved ccmVerify");
                    processAppResponse(payload, rel);
                    // TODO !!!
                }
                break;

            case 0x05://ack response
                appendLog("recieved ack responce");
                break;

            default:
                appendLog("not yet implemented rx command: " + command + " ( " + String.format("%02X", command));

        }
    }

    private void processAppResponse(byte[] payload, boolean reliable) {
        appendLog("processing app response");
        ByteBuffer b = ByteBuffer.wrap(payload);
        b.order(ByteOrder.LITTLE_ENDIAN);

        b.get();
        byte servId = b.get();
        short commId = b.getShort();

        appendLog("Service ID: " + String.format("%X", servId) + " Comm ID: " + String.format("%X", commId) + " reliable: " + reliable);

        short error = b.getShort();
        if (error != 0) {
            appendLog("got error :(");
            return;
        }

        switch (commId) {
            case (short) 0xA055:
                Application.sendAppCommand(Application.Command.COMMANDS_SERVICES_VERSION,btConn);
                break;
            case (short) 0xA065:
                Application.sendAppCommand(Application.Command.BINDING,btConn);
                break;
            case (short) 0xA095:
                step+=100;
                Protocol.sendSyn(btConn);
                break;
        }
    }

    void handleData(byte buffer[], int bytes) {

        switch (step) {
            case 1: //we requested con, now we try to request auth
            {
                appendLog("(Handle Data) doing A_KEY_REQ");
                byte[] key = {16, 12, 2, 0, -16};

                btConn.writeCommand(key);

                if(activity!=null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Pdata.setPinokay(false);

                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                            final EditText pinIn = new EditText(activity);
                            pinIn.setGravity(Gravity.CENTER);
                            pinIn.setInputType(InputType.TYPE_CLASS_NUMBER);
                            // pinIn.setHint("XXX XXX XXXX");
                            final AlertDialog AD = new AlertDialog.Builder(activity,R.style.CustomAlertDialog)
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
                                                    appendLog("got the pin: " + pin);
                                                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

                                                    Pdata.setPinokay(true);

                                                    Pdata.setPin(Utils.generateKey(pin));
                                                    step = 2;
                                                    //sending key available:
                                                    appendLog(" doing A_KEY_AVA");
                                                    byte[] key = {16, 15, 2, 0, -16};
                                                    btConn.writeCommand(key);
                                                }
                                            }
                                    ).show();


                                    AD.getWindow().setLayout(400, 320);
                                    pinIn.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
                                    pinIn.setTextSize(30);
                                    AD.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                    pinIn.addTextChangedListener(new TextWatcher() {
                                        @Override
                                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                                        @Override
                                        public void onTextChanged(CharSequence s, int start, int before, int count) {}

                                        @Override
                                        public void afterTextChanged(Editable editable) {
                                            String pin = pinIn.getText().toString();
                                            if (pin.length() == 10) {
                                                AD.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                            } else {
                                                AD.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                            }
                                        }

                                    });
                        }
                    });
                }

            }
            break;
            default: //we indicated that we have a key, now lets handle the handle to the handle with an handler
            {
                List<Byte> t = new ArrayList<>();
                for (int i = 0; i < bytes; i++)
                    t.add(buffer[i]);
                for (List<Byte> x : Frame.frameDeEscaping(t)) {
                    byte[] xx = new byte[x.size()];
                    for (int i = 0; i < x.size(); i++)
                        xx[i] = x.get(i);
                    boolean rel = false;
                    if ((x.get(1) & 0x20) == 0x20) {
                        rel = true;

                        byte seq = 0x00;
                        if ((x.get(1) & 0x80) == 0x80)
                            seq = (byte) 0x80;

                        btConn.getPumpData().incrementNonceTx();

                        List<Byte> packet = Packet.buildPacket(new byte[]{16, 5, 0, 0, 0}, null, true,btConn);

                        packet.set(1, (byte) (packet.get(1) | seq));                //OR the received sequence number

                        packet = Utils.ccmAuthenticate(packet, btConn.getPumpData().getToPumpKey(), btConn.getPumpData().getNonceTx());

                        List<Byte> temp = Frame.frameEscape(packet);
                        byte[] ro = new byte[temp.size()];
                        int i = 0;
                        for (byte b : temp)
                            ro[i++] = b;
                        try {
                            btConn.write(ro);
                            appendLog("(handleData()) : succesful wrote " + temp.size() + " bytes!");
                        } catch (Exception e) {
                            e.printStackTrace();
                            appendLog("(handleData()) : error in tx: " + e.getMessage());
                        }

                    } else {
                        rel = false;
                    }
                    handleRX(xx, x.size(), rel);
                }
            }
            break;
        }
    }


}



