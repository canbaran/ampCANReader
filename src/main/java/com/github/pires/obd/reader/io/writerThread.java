package com.github.pires.obd.reader.io;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.pires.obd.reader.activity.MainActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import amp.internal.io.CanMessage;

import static android.content.ContentValues.TAG;

/**
 * Created by canbaran on 12/21/17.
 */

public class writerThread extends Thread {
    private InputStream elmInputStream;
    private OutputStream elmOutputStream;
    private Context ctxUi;
    private final BlockingQueue<ArrayList<can_data>> myQ;
    private String hexID;
    private String vehicleID;
    private String userName;
    private int blockSize = 256;
    private  ObdGatewayService myService;
    private Long timeStamp;
    private Long threadStartTimeStamp;
    private ArrayList<Integer> IDArray;
    private int ourByteLength = 16;
    private int messageLengthWithID = 19;
    private int indexKey;

    public writerThread(BlockingQueue<ArrayList<can_data>> incomingQ, InputStream elmInput, OutputStream elmOutput, Context ctx,
//                        String incomingHexID,
                        String incomingVehicleID,
                        String incomingUserName,
                        ObdGatewayService incomingService,
                        ArrayList<Integer> incomingIDArr,
                        int incomingIndexKey)
    {

        elmInputStream = elmInput;
        elmOutputStream = elmOutput;
        ctxUi = ctx;
        myQ = incomingQ;

//        hexID = incomingHexID;
        vehicleID = incomingVehicleID;
        userName = incomingUserName;
        myService = incomingService;

//        IDArray = incomingIDArr;
        indexKey = incomingIndexKey;

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
                elmOutputStream.write(("AT MA" + "\r").getBytes());
                elmOutputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

            while(myService.isRunning()) {


                //read from Elm
                ArrayList<can_data> canDataLs = new ArrayList<can_data>();

                String[] curCanData = new String[blockSize];
                loopStartTimeStamp = System.currentTimeMillis();
                Long a = System.nanoTime();
                for( int i=0; i<blockSize; i++){

                    curCanData[i] = readDataFromElm();
                    final int iteratorI= i;
                    ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((MainActivity) ctxUi).canBUSUpdate("SUpdate7", "Status Update7", "Block For Loop at " + Integer.toString(iteratorI) );
                        }
                    });
//                    long t2 = System.nanoTime();
                    if (curCanData[i].equals("Exception Occured") || curCanData[i].equals(""))
                        break;

                }

                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((MainActivity) ctxUi).canBUSUpdate("SUpdate8", "Status Update8", "Block For loop Finished ");
                    }
                });


                Long b = System.nanoTime();
                long avgTimeMillis = (b-a)/(blockSize*1000000);
//                getCurrentLocation();
                for (int i=0; i<blockSize; i++) {
                    can_data curData = new can_data(); // us-east-1:0d327241-156d-45e2-9560-4ce6c9192613
                    curData.setTimeStamp(loopStartTimeStamp+i*avgTimeMillis);
                    curData.setData(hexStringToByteArray(curCanData[i]));
//                    curData.setCanID(hexID);
                    curData.setVIN(vehicleID);
//                    curData.setCanIDMeaning("Still Hard Coded");
//                    curData.setGPS("MY GPS");
//                    curData.setLat(curLat);
//                    curData.setLong(curLong);
                    canDataLs.add(curData);

                }
                Log.d(TAG, Integer.toString(blockSize) + " points produced: " + Long.toString( (b-a) / (blockSize*1000000) ) + " [ms] per point" );
                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((MainActivity) ctxUi).canBUSUpdate("SUpdate", "Status Update", "block ready to be sent to the Reader" );
                    }
                });
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
    private String readDataFromElm() {
        StringBuilder res = new StringBuilder();
        byte b = 0;
        char c;
        String temp;
        boolean bufferFullHit = false;

        HashMap<Integer, String> IdDataMap = new HashMap<Integer, String>();

        while( myService.isRunning() ) {
            try {

                b = (byte) elmInputStream.read();
            } catch (Exception e) {
                e.printStackTrace();
                return "Exception Occured";
            }
            c = (char) b;
            if (c == '>' || c == '<' || c == '\r') // read until '>' arrives
            {
//                if (!res.toString().equals("DATA ERROR")) {
                final String byteData = res.toString().replaceAll("(\n" +
                        "|\r" + "|<" + "|\\bAT\\s?MA\\b" + "|\\s+" +
                        "|\\bDATA\\s?ERROR\\b)", "");
                try {
                    if ( !bufferFullHit ) {
                        if (byteData.equals("BUFFERFULL")) {
                            ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((MainActivity) ctxUi).canBUSUpdate("BUFFER", "BUFFER", byteData);
                                }
                            });
                            ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((MainActivity) ctxUi).canBUSUpdate("SUpdate1", "Status Update1", "BUFFER FULL is Seen" );
                                }
                            });
                            bufferFullHit = true;
                            elmOutputStream.write(("AT MA" + "\r").getBytes());
                            elmOutputStream.flush();
                            ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((MainActivity) ctxUi).canBUSUpdate("SUpdate2", "Status Update2", "the Second AT MA is issued" );
                                }
                            });
                            Log.d(TAG, "Buffer Full Hit. Re-issuing AT MA");
                        }
                    } else {
                        ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((MainActivity) ctxUi).canBUSUpdate("SUpdate3", "Status Update3", "After 2nd AT MA, Regular flow has started" );
                            }
                        });
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
                                        ((MainActivity) ctxUi).canBUSUpdate("SUpdate4", "Status Update4", Integer.toString(indexKey) + " is seen Let's fill up the Upload buffer");
                                    }
                                });
                                return concatByteData(IdDataMap);
                            } else {
                                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((MainActivity) ctxUi).canBUSUpdate("SUpdate5", "Status Update5", Integer.toString(indexKey) + " is NOT seen");
                                    }
                                });
                                for (int curKey : IdDataMap.keySet()) {
                                    final int curkey2 = curKey;
                                    ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((MainActivity) ctxUi).canBUSUpdate("SUpdate6", "Status Update6", "map has key: " + Integer.toString(curkey2) );
                                        }
                                    });
                                }

                            }

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
        return "";

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

    private ArrayList<Integer> extractIDs(String byteData, HashMap<Integer, String> IdDataMap) {

        ArrayList<Integer> idArr = new ArrayList<Integer>();

        int curMsgID = Integer.parseInt(byteData.substring(0,3),16);
        String byteDataNoId = byteData.replace(byteData.substring(0,3), "");

        if(byteDataNoId.length() == ourByteLength) {
            IdDataMap.put(curMsgID, byteDataNoId);
        } else {
            Log.d(TAG, "wrong byte length");
        }

        Set<Integer> mySet = IdDataMap.keySet();
        idArr.addAll(mySet);
        Collections.sort(idArr);
        return idArr;

    }

    private String concatByteData(HashMap<Integer, String> IdDataMap) {

        //TODO: this needs to be done the way Greg describes in his email. 32element array each 8 byte goes to a specific position
//        for(i=0;i<8;i++)
//        {
//            data[i+(CANID&Index)*8] = MSG[i];
//        }

        StringBuilder byteBlock = new StringBuilder();
        ArrayList<Integer> idArr = new ArrayList<Integer>();
        Set<Integer> mySet = IdDataMap.keySet();
        idArr.addAll(mySet);
        Collections.sort(idArr);
        for(Integer curKey: idArr) {
            byteBlock.append(IdDataMap.get(curKey));
        }
        return byteBlock.toString();
    }

    private void writeToReaderThread(ArrayList<can_data> canDataLs) {
        //write to the other thread
//        CanMessage.canMessage.Builder  curCanMsg = CanMessage.canMessage.newBuilder();
//        curCanMsg.setCanData(curCanData);
//        curCanMsg.setTimestamp(System.currentTimeMillis());
        try {
            Long a = System.currentTimeMillis();
            myQ.put(canDataLs);
//            curCanMsg.build().writeDelimitedTo(_os);
            Long b = System.currentTimeMillis();
//            Log.d(TAG, "Time it took to write into the Reader Pipe: " + Long.toString(b-a) + " [ms]");
//            System.out.println("[COMPLETED] WriterThread submitted: " + curCanData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
