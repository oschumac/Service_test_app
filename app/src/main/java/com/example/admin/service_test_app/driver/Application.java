package com.example.admin.service_test_app.driver;

import android.os.Debug;
import android.util.Log;

import com.example.admin.service_test_app.ruffy.Frame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;

/**
 * Some statics to do some things on application level
 */
public class Application {
    /**
     * how often we accept an mode error before accepting we are in the wrong mode
     */
    public static int MODE_ERROR_TRESHHOLD = 3;


    public static enum Command
    {
        COMMANDS_SERVICES_VERSION,
        REMOTE_TERMINAL_VERSION,
        BINDING,
        COMMAND_MODE,
        COMMAND_DEACTIVATE,
        RT_MODE,
        RT_DEACTIVATE,
        DEACTIVATE_ALL,
        APP_DISCONNECT,
        CMD_PING,
    }

    public static void sendAppConnect(BTConnection btConn) {
        ByteBuffer payload = null;
        byte[] connect_app_layer = {16, 0, 85, -112};

        payload = ByteBuffer.allocate(8);				//4 bytes for application header, 4 for payload
        payload.put(connect_app_layer);					//Add prefix array
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putInt(Integer.parseInt("12345"));

        Application.sendData(payload,true,btConn);
    }

    private static void sendData(ByteBuffer payload, boolean reliable, BTConnection btConn){
        btConn.getPumpData().incrementNonceTx();

        byte[] sendR = {16,3,0,0,0};

        List<Byte> packet  = Packet.buildPacket(sendR, payload, true,btConn);					//Add the payload, set the address if valid

        if(reliable) {
            int seq = btConn.seqNo;
            packet.set(1, setSeqRel(packet.get(1), true,btConn));                        //Set the sequence and reliable bits
        }
        Packet.adjustLength(packet, payload.capacity());							//Set the payload length

        packet = Utils.ccmAuthenticate(packet, btConn.getPumpData().getToPumpKey(), btConn.getPumpData().getNonceTx());		//Authenticate packet


        List<Byte> temp = Frame.frameEscape(packet);
        byte[] ro = new byte[temp.size()];
        int i = 0;
        for(byte b : temp)
            ro[i++]=b;

        try {
            btConn.write(ro);

        } catch (Exception e) {
            Log.e("Application ","sendData() failed -> "  + e);
        }

    }

    private static Byte setSeqRel(Byte b, boolean rel, BTConnection btConn)
    {
        b = (byte) (b | btConn.seqNo);			//Set the sequence bit

        if(rel)
            b = (byte) (b |0x20);			//Set the reliable bit

        btConn.seqNo ^= 0x80;

        return b;
    }
    private static byte[] service_activate = {16, 0, 0x66, (byte)0x90};
    private static byte[] service_deactivate = {16, 0, 0x69, (byte)0x90};

    public static void sendAppCommand(Command command, BTConnection btConn){
        ByteBuffer payload = null;

        String s = "";

        boolean reliable = true;

        switch(command)
        {
            case COMMAND_MODE:
                s = "COMMAND_ACTIVATE";
                payload = ByteBuffer.allocate(7);
                payload.put(service_activate);
                payload.put((byte)0xB7);
                payload.put((byte)0x01);
                payload.put((byte)0x00);
                reliable = true;
                break;

            case COMMAND_DEACTIVATE:
                s = "COMMAND DEACTIVATE";
                payload = ByteBuffer.allocate(5);
                payload.put(service_deactivate);
                payload.put((byte)0xB7);
                reliable = true;
                break;
            case RT_MODE:
                s = "RT_ACTIVATE";
                payload = ByteBuffer.allocate(7);
                payload.put(service_activate);
                payload.put((byte)0x48);
                payload.put((byte)0x01);
                payload.put((byte)0x00);
                reliable = true;
                break;
            case RT_DEACTIVATE:
                s = "RT DEACTIVATE";
                payload = ByteBuffer.allocate(5);
                payload.put(service_deactivate);
                payload.put((byte)0x48);
                reliable = true;
                break;
            case COMMANDS_SERVICES_VERSION:
                s = "COMMAND_SERVICES_VERSION";
                payload = ByteBuffer.allocate(5);
                payload.put((byte)16);
                payload.put((byte)0);
                payload.put((byte) (((short)0x9065) & 0xFF));
                payload.put((byte) ((((short)0x9065)>>8) & 0xFF));
                payload.put((byte)0xb7);
                reliable = true;
                break;
            case REMOTE_TERMINAL_VERSION:
                s = "REMOTE_TERMINAL_VERSION";
                payload = ByteBuffer.allocate(5);
                payload.put((byte)16);
                payload.put((byte)0);
                payload.put((byte) (((short)0x9065) & 0xFF));
                payload.put((byte) ((((short)0x9065)>>8) & 0xFF));
                payload.put((byte)0x48);
                reliable = true;
                break;
            case BINDING:
                s = "BINDING";
                payload = ByteBuffer.allocate(5);
                payload.put((byte)16);
                payload.put((byte)0);
                payload.put((byte) (((short)0x9095) & 0xFF));
                payload.put((byte) ((((short)0x9095)>>8) & 0xFF));
                payload.put((byte) 0x48);		//Binding OK
                reliable = true;
                break;

            case APP_DISCONNECT:
                byte[] connect_app_layer = {16 , 0, 0x5a, 0x00};
                payload = ByteBuffer.allocate(6);				//4 bytes for application header, 4 for payload
                payload.put(connect_app_layer);					//Add prefix array
                payload.order(ByteOrder.LITTLE_ENDIAN);
                payload.putShort((byte)0x6003);
                reliable = true;
                break;

            case CMD_PING:
                payload.put((byte)16);
                payload.put((byte)0xB7);
                payload.put((byte) (0x9AAA & 0xFF));
                payload.put((byte) ((0x9AAA>>8) & 0xFF));
                reliable = false;
                break;

            default:
                s = "uknown subcommand: "+command;
                break;
        }

        Log.v("Application"," sendAppCommand-" + s);

        if(payload != null)
        {
            sendData(payload,reliable,btConn);
        }
    }

    public static void sendAppDisconnect(BTConnection btConn) {
        sendAppCommand(Command.APP_DISCONNECT,btConn);
    }

    public static void cmdPing(BTConnection btConn)
    {
        sendAppCommand(Command.CMD_PING,btConn);
    }

    public static short sendRTKeepAlive(short rtSeq, BTConnection btConn) {
        ByteBuffer payload = ByteBuffer.allocate(6);
        payload.put((byte)16);
        payload.put((byte)0x48);
        payload.put((byte) (0x0566 & 0xFF));
        payload.put((byte) ((0x0566>>8) & 0xFF));

        payload.put((byte) (rtSeq & 0xFF));
        payload.put((byte) ((rtSeq>>8) & 0xFF));

        sendData(payload,false,btConn);

        //btConn.log("/////////////////////////////////////////////////////////////////////");
        //btConn.log("send alive with seq: "+rtSeq);
        //btConn.log("/////////////////////////////////////////////////////////////////////");
        rtSeq++;
        return rtSeq;

    }

    public static void cmdErrStatus(BTConnection btConn)
    {
        ByteBuffer payload = ByteBuffer.allocate(4);
        payload.put((byte)16);

        payload.put((byte)0x48);

        payload.put((byte) (0x9AA5 & 0xFF));
        payload.put((byte) ((0x9AA5>>8) & 0xFF));

        sendData(payload, true, btConn);
    }

    public static short rtSendKey(byte key, boolean changed,short rtSeq, BTConnection btConn)
    {
        ByteBuffer payload = ByteBuffer.allocate(8);

        payload.put((byte)16);
        payload.put((byte)0x48);
        payload.put((byte) 0x65);
        payload.put((byte) 0x05);

        payload.put((byte) (rtSeq & 0xFF));
        payload.put((byte) ((rtSeq>>8) & 0xFF));

        payload.put(key);

        if(changed)
            payload.put((byte) 0xB7);
        else
            payload.put((byte) 0x48);

       // btConn.log("/////////////////////////////////////////////////////////////////////");
        String k = "";
        switch (key)
        {
            case 0x00:
                k="NOKEY";
                break;
            case 0x03:
                k="MENU";
                break;
            case 0x0C:
                k="CHECK";
                break;
            case 0x30:
                k="UP";
                break;
            case (byte)0xC0:
                k="DOWN";
                break;
            case (byte)0xF0:
                k="COPY";
                break;
            case (byte)0x33:
                k="BACK";
                break;
        }
        //btConn.log("send key "+k+" with seq: "+rtSeq);
        //btConn.log("/////////////////////////////////////////////////////////////////////");
        sendData(payload, false, btConn);

        rtSeq++;
        return rtSeq;
    }

    //public static void processAppResponse(byte[] payload, boolean reliable, AppHandler handler) {
    public static void processAppResponse(byte[] payload, boolean reliable) {
        //handler.log("processing app response");
        ByteBuffer b = ByteBuffer.wrap(payload);
        b.order(ByteOrder.LITTLE_ENDIAN);

        b.get();//ignore
        byte servId = b.get();
        short commId = b.getShort();

        //handler.log("Service ID: " + String.format("%X", servId) + " Comm ID: " + String.format("%X", commId) + " reliable: " + reliable);

        String descrip = null;
        if(reliable)
        {
            short error = b.getShort();
            if (!cmdProcessError(error)) {
                return;
            }

            switch (commId) {
                case (short) 0xA055://connect answer:
                    descrip = "connect answer";
                    //handler.connected();
                    break;
                case (short) 0xA065://something
                    descrip = "something";
                case (short) 0xA095://bind
                    descrip = "bind";
                    //handler.log("not should happen here!");
                    break;
                case (short) 0xA066://activate rt:
                    descrip = "activate rt";
                    //handler.rtModeActivated();
                    break;
                case (short) 0x005A://AL_DISCONNECT_RES:
                    descrip = "AL_DISCONNECT_RES";
                    break;
                case (short) 0xA069://service deactivated
                    descrip = "AL_DEACTIVATE_RES";
                    //handler.modeDeactivated();
                    break;
                case (short) 0xA06A://service all deactivate
                    descrip = "AL_DEACTIVATE_ALL_RES";
                    //handler.modeDeactivated();
                    break;
                default:
                    descrip = "UNKNOWN";
                    break;
            }
        } else {
            switch (commId) {
                case (short) 0x0555://Display frame
                    descrip = "Display Frame";
                    //handler.addDisplayFrame(b);
                    break;
                case (short) 0x0556://key answer
                    descrip = "key answer";
                    //handler.keySent(b);
                    break;
                case (short) 0x0566://alive answer, often missed
                    descrip = "alive answer";
                    break;
                default:
                    descrip = "UNKNOWN";
                    break;
            }
        }
        Log.v("Application() ","processAppresponce(): "+descrip);
    }

    //private static boolean cmdProcessError(short error, AppHandler handler) {
    private static boolean cmdProcessError(short error) {
        String desc = "Error > " + String.format("%X", error) + " ";

        if (error == 0x0000) {
            desc = "No error found!";
            return true;
        } else {
            switch (error) {
                //Application Layer **********************************************//
                case (short) 0xF003:
                    desc = "Unknown Service ID, AL, RT, or CMD";
                    break;
                case (short) 0xF006:
                    desc = "Invalid payload length";
                    break;
                case (short) 0xF05F:
                    desc = "wrong mode";
                    // handler.modeError();
                    break;

                case (short) 0xF50C:
                    desc = "wrong sequence";
                    //handler.sequenceError();
                    break;
                case (short) 0xF533:
                    desc = "died - no alive";
                    break;
                case (short) 0xF056:
                    desc = "not complete connected";
                    break;
            }

            //handler.error(error,desc);
            return false;
        }
    }


    public static void cmdDeliverBolus(double bolus,BTConnection locBT) {
        String FUNC_TAG = "deliverBolus";
        final byte AL_VERSION = (byte) 16;
        final short CBOL_BOLUS_DELIVER = (short) -22935;
        final byte COMMAND_MODE_SERVICE_ID = (byte) -73;
        final byte AL_SERVICE_DEACTIVATE_REQ_LSB = (byte) 105;
        final byte AL_SERVICE_DEACTIVATE_REQ_MSB = (byte) -112;
        final byte AL_CONNECT_REQ_LSB = (byte) 85;

        // CC 10 A3 1A 00 10 BA 57 00 00 00 00 00 00 00 00 00 00 00 10 B7 69 96 55 59 01 00 00 00 00 00 00 00 80 3F 00 00 00 00 00 00 00 00 D4 35 CE 76 53 D2 67 F7 E8 B6 CC


        ByteBuffer payload = ByteBuffer.allocate(26);
        payload.put(AL_VERSION);
        payload.put(COMMAND_MODE_SERVICE_ID);
        payload.put(AL_SERVICE_DEACTIVATE_REQ_LSB);
        payload.put((byte) -106);
        payload.put(AL_CONNECT_REQ_LSB);
        payload.put((byte) 89);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putShort((short) bolus);
        payload.putShort((short) 0);
        payload.putShort((short) 0);
        payload.putFloat((float) bolus);
        payload.putFloat(0.0f);
        payload.putFloat(0.0f);
        short crc = (short) -1;

        for (int i = 4; i < 24; i += 1) {
            crc = Utils.updateCrc(crc, payload.get(i));
        }

        payload.putShort(crc);

        boolean reliable = true;
        sendData(payload,reliable,locBT);

        Log.v(TAG,"cmdDeliverBolus Bolus-> " + bolus);
        // sendData(payload, "SEND_CBOL_DELIVER", CBOL_BOLUS_DELIVER, retries, timeout, bolus);
        /*
        setBolusState(BOLUS_CMD_RESP);
        if (commandTimer != null) {
            commandTimer.cancel(truce);
        }
        commandTimer = scheduler.schedule(this.command, 15000, TimeUnit.MILLISECONDS);
        */
    }
}
