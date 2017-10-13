package com.example.admin.service_test_app.Service;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import com.example.admin.service_test_app.driver.Application;
import com.example.admin.service_test_app.driver.BTConnection;
import com.example.admin.service_test_app.driver.BTHandler;
import com.example.admin.service_test_app.driver.Packet;
import com.example.admin.service_test_app.driver.PacketHandler;
import com.example.admin.service_test_app.driver.Protocol;
import com.example.admin.service_test_app.driver.PumpData;
import com.example.admin.service_test_app.driver.Ruffy;
import com.example.admin.service_test_app.driver.Utils;
import com.example.admin.service_test_app.ruffy.Frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static com.example.admin.service_test_app.driver.Application.*;

/**
 * Created by Admin on 30.09.2017.
 */

public class Pump {
    private Activity activity;
    private PumpData pumpData;
    private BTConnection BT;
    private int step = 0;
    public void Start(Activity act) {
        activity = act;

        if(pumpData==null)
        {
            pumpData = PumpData.loadPump(activity);
        }

        PumpHandler();
        Log.v(TAG, "(public justatestService())  testworker starting");
        BT.connect(pumpData,10);

    }


    private void PumpHandler(){
        BT = new BTConnection(new BTHandler() {
            BluetoothDevice device;

            @Override
            public void deviceConnected() {
                Log.v("Pump","connected to device ");
                //Application.sendAppCommand(Application.Command.RT_MODE, btConn);
                // Application.sendAppConnect(BT);
                synThread.start();
            }

            @Override
            public void log(String s) {
                Log.v("Pump",s);
            }

            @Override
            public void fail(String s) {
                Log.v("Pump",s);
                BT.connect(pumpData,10);
            }

            @Override
            public void deviceFound(BluetoothDevice device) {
            }

            @Override
            public void handleRawData(byte[] buffer, int bytes) {
                List<Byte> t = new ArrayList<>();
                for (int i = 0; i < bytes; i++)
                    t.add(buffer[i]);
                for (List<Byte> x : Frame.frameDeEscaping(t)) {
                    byte[] xx = new byte[x.size()];
                    for (int i = 0; i < x.size(); i++)
                        xx[i] = x.get(i);
                    boolean rel = false;
                    if (x.size()>1 && (x.get(1) & 0x20) == 0x20) {
                        rel = true;

                        byte seq = 0x00;
                        if ((x.get(1) & 0x80) == 0x80)
                            seq = (byte) 0x80;

                        // handler.sendImidiateAcknowledge(seq);
                        Protocol.sendAck(seq,BT);
                    } else {
                        rel = false;
                    }
                    handleRX(xx, x.size(), rel);
                }

            }

            @Override
            public void requestBlueTooth() {
            }
        });
    }

    final Thread synThread=new Thread(){
        @Override
        public void run() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }

            // Protocol.sendIDReq(BT);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }

            while (BT.isConnected()) {
                // Log.v("Pump","sendSyn");
                Protocol.sendSyn(BT);
                //rtSequence = Application.sendRTKeepAlive(rtSequence, btConn);

                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Log.e("synThreaad", " Thread Stopped ->" + e);
                }
            }
            BT.connect(pumpData,10);
            Log.v("Pump","synThread stopped");
        }
    };

    private void handleRX(byte[] inBuf, int length, boolean reliableFlagged) {


        ByteBuffer buffer = ByteBuffer.wrap(inBuf, 0, length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] nonce, payload, umac, packetNoUmac;

        Byte command;
        buffer.get(); //ignore
        command = buffer.get();

        short payloadlength = buffer.getShort();

        buffer.get(); //ignore

        nonce = new byte[13];
        buffer.get(nonce, 0, nonce.length);

        payload = new byte[payloadlength];
        buffer.get(payload, 0, payload.length);

        umac = new byte[8];
        buffer.get(umac, 0, umac.length);

        packetNoUmac = new byte[buffer.capacity() - umac.length];
        buffer.rewind();
        for (int i = 0; i < packetNoUmac.length; i++)
            packetNoUmac[i] = buffer.get();

        buffer.rewind();

        byte c = (byte)(command & 0x1F);
        switch (c) {
            case 20:
                Log.v("Pump Handle RX","got an id response");
                if (Utils.ccmVerify(packetNoUmac, getToDeviceKey(), umac, nonce)) {
                    handleResponse(Packet.Response.ID,reliableFlagged,payload);
                }
                break;
            case 24:
                Log.v("Pump Handle RX","got an Sync response");
                if (Utils.ccmVerify(packetNoUmac, getToDeviceKey(), umac, nonce)) {
                    handleResponse(Packet.Response.SYNC,reliableFlagged,payload);
                }
                break;

            case 0x23:
                Log.v("Pump Handle RX","got RELIABLE_DATA");
                if (Utils.ccmVerify(packetNoUmac, getToDeviceKey(), umac, nonce)) {
                    handleResponse(Packet.Response.RELIABLE_DATA,reliableFlagged,payload);
                }
                break;
            case 0x03:
                Log.v("Pump Handle RX","got UNRELIABLE_DATA");
                if (Utils.ccmVerify(packetNoUmac, getToDeviceKey(), umac, nonce)) {

                    handleResponse(Packet.Response.UNRELIABLE_DATA,reliableFlagged,payload);
                }
                break;
            case 0x05:
                Log.v("Pump Handle RX","got ack response");

                Log.v("Pump Handle RX","read()"+ inBuf.length +" bytes: "+Utils.byteArrayToHexString(inBuf,inBuf.length));

                //ignore ack response
                break;

            case 0x06:
                if(Utils.ccmVerify(packetNoUmac, getToDeviceKey(), umac, nonce))
                {
                    byte error = 0;
                    String err = "";

                    if(payload.length > 0)
                        error = payload[0];

                    switch(error)
                    {
                        case 0x00:
                            err = "Undefined";
                            break;
                        case 0x0F:
                            err = "Wrong state";
                            break;
                        case 0x33:
                            err = "Invalid service primitive";
                            break;
                        case 0x3C:
                            err = "Invalid payload length";
                            break;
                        case 0x55:
                            err = "Invalid source address";
                            break;
                        case 0x66:
                            err = "Invalid destination address";
                            break;
                    }
                    Log.v("handleRX", "Error in Transport Layer! ("+err+")");
                }
                break;
            default:
                Log.v("Pump.handleRX()","not yet implemented rx command: " + command + " ( " + String.format("%02X", command));
                break;
        }
        // Log.v("Pump.handleRX()"," rx command: " + command + " ( " + String.format("%02X", command));
    }

    private void handleResponse(Packet.Response response,boolean reliableFlagged, byte[] payload) {
                switch (response)
                {
                    case ID:
                        Log.v("Pump handleResponce()"," SendSyn");
                        Protocol.sendSyn(BT);
                        break;
                    case SYNC:
                        //Log.v("Pump handleResponce()"," got SYNC -> reset seqNo");
                        BT.seqNo = 0x00;

                if(step<201)
                    sendAppConnect(BT);
                else
                {
                    sendAppDisconnect(BT);
                    step = 300;
                }
                break;
            case UNRELIABLE_DATA:
            case RELIABLE_DATA:
                //Log.v("Pump handleResponce()","RELIABLE_DATA");
                Application.processAppResponse(payload, reliableFlagged);
                break;
        }
    }

    private Object getToDeviceKey() {
        return pumpData.getToDeviceKey();
    }

    public void sendPumpCommandMode() {
        sendAppCommand(Command.COMMAND_MODE,BT);
    }
    public void sendPumpRTMode() {
        sendAppCommand(Command.RT_MODE,BT);
    }

    public boolean isBTConnected() {
        if (BT!=null) {
            return BT.isConnected();
        } else {
            return false;
        }
    }

    public void cmdDeliverBolus(double bolus) {
        if (BT != null ) {
            if (BT.isConnected()) {

                Log.v("Pump cmdDeliverBolus()"," BT ist Connected try to Bolus");
                Application.cmdDeliverBolus(bolus, BT);
            }
        }
    }

}
