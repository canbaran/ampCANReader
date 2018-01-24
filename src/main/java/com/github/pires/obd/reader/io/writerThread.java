package com.github.pires.obd.reader.io;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.pires.obd.reader.activity.ConfigActivity;
import com.github.pires.obd.reader.activity.MainActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import amp.internal.io.CanMessage;

import static android.content.ContentValues.TAG;
import static com.github.pires.obd.reader.io.uiNotificationIds.elmDeviceStatus;

/**
 * Created by canbaran on 12/21/17.
 */

public class writerThread extends Thread {
    private InputStream elmInputStream;
    private OutputStream elmOutputStream;
    private Context ctxUi;
    private final BlockingQueue<ArrayList<can_data>> myQ;
//    private String hexID;
    private String vehicleID;
    private String userName;
    private int blockSize = 256;
    private  ObdGatewayService myService;
//    private Long timeStamp;
//    private Long threadStartTimeStamp;
    private ArrayList<Integer> IDArray;
    private int ourByteLength = 16;
    private int messageLengthWithID = 19;
    private int indexKey;
    private int numOfBytes;
//    private int numberOfMessages = 0;
//    private int numberOfBufferFullErrors = 0;

    public writerThread(BlockingQueue<ArrayList<can_data>> incomingQ, InputStream elmInput, OutputStream elmOutput, Context ctx,
//                        String incomingHexID,
                        String incomingVehicleID,
                        String incomingUserName,
                        ObdGatewayService incomingService,
                        ArrayList<Integer> incomingIDArr,
                        int incomingIndexKey,
                        int incomingNumOfBytes)
    {

        elmInputStream = elmInput;
        elmOutputStream = elmOutput;
        ctxUi = ctx;
        myQ = incomingQ;

//        hexID = incomingHexID;
        vehicleID = incomingVehicleID;
        userName = incomingUserName;
        myService = incomingService;

        IDArray = incomingIDArr;
        indexKey = incomingIndexKey;
        numOfBytes = incomingNumOfBytes;

//        hdForUi = hd;
    }

    public void run()
    {
        try
        {
//            threadStartTimeStamp = System.currentTimeMillis();
            Long loopStartTimeStamp;
            long t1 = System.nanoTime();
//            int counter = 0;
//            String submissionStatus;


            //issue the atma commands
            try {
//                elmOutputStream.write(("AT CAF0" + "\r").getBytes());
//                elmOutputStream.flush();
                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((MainActivity) ctxUi).canBUSUpdate( elmDeviceStatus,  elmDeviceStatus,  "Initial AT MA Sent");
                    }
                });
                elmOutputStream.write(("AT MA" + "\r").getBytes());
                elmOutputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

            while(myService.isRunning()) {


                //read from Elm
                ArrayList<can_data> canDataLs = new ArrayList<can_data>();

//                byte[][] curCanData = new byte[blockSize][];
                List<byte[]> curCanData = new ArrayList<>();

                loopStartTimeStamp = System.currentTimeMillis();
                Long a = System.nanoTime();
                for( int i=0; i<blockSize; i++){

                    curCanData.add( readDataFromElm() );
                    final int iteratorI= i;
                    ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((MainActivity) ctxUi).canBUSUpdate(elmDeviceStatus, elmDeviceStatus, "Block For Loop at " + Integer.toString(iteratorI) );
                        }
                    });
                    if (curCanData.get(i) == null) {
                        Log.d(TAG, "Exited the inner data collector for loop");
                        break;
                    }

                }

//                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ((MainActivity) ctxUi).canBUSUpdate("SUpdate8", "Status Update8", "Block For loop Finished ");
//                    }
//                });


                Long b = System.nanoTime();
                long avgTimeMillis = (b-a)/(curCanData.size()*1000000);
//                getCurrentLocation();
                for (int i=0; i<curCanData.size(); i++) {
                    can_data curData = new can_data(); // us-east-1:0d327241-156d-45e2-9560-4ce6c9192613
                    curData.setTimeStamp(loopStartTimeStamp+i*avgTimeMillis);
                    curData.setData(curCanData.get(i));
//                    curData.setCanID(hexID);
                    curData.setVIN(vehicleID);
//                    curData.setCanIDMeaning("Still Hard Coded");
//                    curData.setGPS("MY GPS");
//                    curData.setLat(curLat);
//                    curData.setLong(curLong);
                    canDataLs.add(curData);

                }
                Log.d(TAG, Integer.toString(blockSize) + " points produced: " + Long.toString( (b-a) / (blockSize*1000000) ) + " [ms] per point" );
//                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ((MainActivity) ctxUi).canBUSUpdate("SUpdate", "Status Update", "block ready to be sent to the Reader" );
//                    }
//                });
                writeToReaderThread(canDataLs);
            }


        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
//        Log.d(TAG, s);
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }



    @NonNull
    private byte[] readDataFromElm() {
        StringBuilder res = new StringBuilder();
        byte b = 0;
        char c;
        byte[] elmDataReady = new byte[ numOfBytes ];
//        String temp;
//        boolean bufferFullHit = false;

        HashMap<Integer, String> IdDataMap = new HashMap<Integer, String>();

        while( myService.isRunning() ) {
            try {
//                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ((MainActivity) ctxUi).canBUSUpdate(elmDeviceStatus, elmDeviceStatus, "About to Read from ELM" );
//                    }
//                });
                b = (byte) elmInputStream.read();
//                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ((MainActivity) ctxUi).canBUSUpdate(elmDeviceStatus, elmDeviceStatus, "Read Success" );
//                    }
//                });
            } catch (Exception e) {
                e.printStackTrace();
                final String eMessage =e.getMessage();
//                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ((MainActivity) ctxUi).canBUSUpdate(elmDeviceStatus, elmDeviceStatus, "Read Success" );
//                    }
//                });
                return null;
            }
            c = (char) b;
            if (c == '>' || c == '<' || c == '\r') // read until '>' arrives
            {
//                if (!res.toString().equals("DATA ERROR")) {
                final String byteData = res.toString().replaceAll("(\n" +
                        "|\r" + "|<" + "|\\bAT\\s?MA\\b" + "|\\s+" +
                        "|\\bDATA\\s?ERROR\\b)", "");
                try {
                    if ( byteData.equals("BUFFERFULL") ) {
                        ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((MainActivity) ctxUi).incrementRowVal("BufferFullError", "Buffer Full Error", "1");
                            }
                        });
                        elmOutputStream.write(("AT MA" + "\r").getBytes());
                        elmOutputStream.flush();
                        Log.d(TAG, "Buffer Full Hit. Re-issuing AT MA");
                    } else {
                        Pattern p = Pattern.compile("^[0-9A-F]+$");
                        Matcher m = p.matcher(byteData);
                        if (byteData.length() == messageLengthWithID && m.find()) {

                            ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((MainActivity) ctxUi).canBUSUpdate(byteData.substring(0,3), byteData.substring(0,3), byteData.substring(3));
                                }
                            });
                            if (didGrabAllMsgs(byteData, IdDataMap)) {
                                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((MainActivity) ctxUi).incrementRowVal("properBlock", " # Proper Blocks Received from ELM ", "1");
                                    }
                                });
                                return concatByteData(IdDataMap, elmDataReady);
                            } //else {
//                                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        ((MainActivity) ctxUi).canBUSUpdate("SUpdate5", "Status Update5", Integer.toString(indexKey) + " is NOT seen");
//                                    }
//                                });
//                                for (int curKey : IdDataMap.keySet()) {
//                                    final int curkey2 = curKey;
//                                    ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            ((MainActivity) ctxUi).canBUSUpdate("SUpdate6", "Status Update6", "map has key: " + Integer.toString(curkey2) );
//                                        }
//                                    });
//                                }
                            //}
                        } else {
                            ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((MainActivity) ctxUi).incrementRowVal("MessageIncompleteErrors", "Message Incomplete Errors", "1");
                                }
                            });
                        }
                    }
                    res.delete(0, res.length());

                } catch (Exception e ) {
                    res.delete(0, res.length());
                    e.printStackTrace();
                }

            }
            res.append(c);
        }
        return null;

    }

    private boolean didGrabAllMsgs(String byteData, HashMap<Integer, String> IdDataMap) {
        //is index in the hashmap
//        ArrayList<Integer> curSessionIDArr = extractIDs(byteData, IdDataMap);
        int curMsgID = Integer.parseInt(byteData.substring(0,3),16);
        String byteDataNoId = byteData.replace(byteData.substring(0,3), "");

        if (byteDataNoId.length() == ourByteLength) {
            IdDataMap.put(curMsgID, byteDataNoId);
        } else {
            Log.d(TAG, "wrong byte length");
        }
        return IdDataMap.containsKey(indexKey);
//        return IDArray.equals(curSessionIDArr);
    }

//    private ArrayList<Integer> extractIDs(String byteData, HashMap<Integer, String> IdDataMap) {
//
//        ArrayList<Integer> idArr = new ArrayList<Integer>();
//
//        int curMsgID = Integer.parseInt(byteData.substring(0,3),16);
//        String byteDataNoId = byteData.replace(byteData.substring(0,3), "");
//
//        if(byteDataNoId.length() == ourByteLength) {
//            IdDataMap.put(curMsgID, byteDataNoId);
//        } else {
//            Log.d(TAG, "wrong byte length");
//        }
//
//        Set<Integer> mySet = IdDataMap.keySet();
//        idArr.addAll(mySet);
//        Collections.sort(idArr);
//        return idArr;
//
//    }

    private byte[] concatByteData(HashMap<Integer, String> IdDataMap, byte[] elmDataReady) {

        //TODO: this needs to be done the way Greg describes in his email. 32element array each 8 byte goes to a specific position
//        for(i=0;i<8;i++)
//        {
//            data[i+(CANID&Index)*8] = MSG[i];
//        }

//        StringBuilder byteBlock = new StringBuilder();
        ArrayList<Integer> idArr = new ArrayList<Integer>();
        Set<Integer> mySet = IdDataMap.keySet();
        idArr.addAll(mySet);
        Collections.sort(idArr);
        for(Integer curKey: idArr) {
            int pos = IDArray.indexOf(curKey);
            if (pos>-1)
                System.arraycopy(hexStringToByteArray(IdDataMap.get(curKey)), 0,elmDataReady , pos * ( IdDataMap.get(curKey).length() /2 ), IdDataMap.get(curKey).length()/2);

//            String curHexArr = IdDataMap.get(curKey);
//            elmDataReady[pos*8:pos*8+7] = hexStringToByteArray(IdDataMap.get(curKey));
//            byteBlock.append();
        }
        return elmDataReady; //byteBlock.toString();
    }

    private void writeToReaderThread(ArrayList<can_data> canDataLs) {
        try {
            myQ.put(canDataLs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
