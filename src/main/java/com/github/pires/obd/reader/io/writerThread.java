package com.github.pires.obd.reader.io;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.pires.obd.reader.App;
import com.github.pires.obd.reader.R;
import com.github.pires.obd.reader.activity.ConfigActivity;
import com.github.pires.obd.reader.activity.MainActivity;
import com.github.pires.obd.reader.database.entity.ampData;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import amp.internal.io.CanMessage;

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
    private int thresholdSpeed = 0;
//    private String hexID;
    private String vehicleID;
    private String userName;
    private int blockSize = 16;
    private  ObdGatewayService myService;
//    private Long timeStamp;
//    private Long threadStartTimeStamp;
    private ArrayList<Integer> IDArray;
    private int ourByteLength = 16;
    private int messageLengthWithID = 19;
    private int indexKey;
    private int numOfBytes;
    private long startTime;
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

                loopStartTimeStamp = System.currentTimeMillis();
                Long a = System.nanoTime();

                for( int i=0; i<blockSize; i++){
                    can_data curData = readDataFromElm();
                    if (curData == null && !myService.isRunning()) {
                        return;
//                        Log.d(TAG, "Exited the inner data collector for loop");
//                        break;
                    }
                    //if stationary dont log
                    if ( curData.getSpeed() >= thresholdSpeed ) {
                        canDataLs.add(curData);

                    }
                    final int iteratorI= i;
                    ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((MainActivity) ctxUi).canBUSUpdate(elmDeviceStatus, elmDeviceStatus, "Block For Loop at " + Integer.toString(iteratorI) );
                        }
                    });
                }
                if ( canDataLs.size() > 0 ) {
                    Long b = System.nanoTime();
                    long avgTimeMillis = (b - a) / (canDataLs.size() * 1000000);
                    for (int i = 0; i < canDataLs.size(); i++) {
                        can_data curData = canDataLs.get(i);
                        Long curTimeStamp = loopStartTimeStamp + i * avgTimeMillis;

                        curData.setTimeStamp(curTimeStamp);
                        curData.setVIN(vehicleID);
//                    storeSingleInternally( curData );
                    }
                    Log.d(TAG, Integer.toString(blockSize) + " points produced: " + Long.toString((b - a) / (blockSize * 1000000)) + " [ms] per point");
                    writeToReaderThread(canDataLs);
                }
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

//    private void storeSingleInternally( can_data curCanData ) {
//        ampData curAmpData = new ampData();
//        curAmpData.setCommandTorque(curCanData.getCommandTorque());
//        curAmpData.setCurve(curCanData.getCurve());
//        curAmpData.setXD(curCanData.getXD());
//        curAmpData.setRLD(curCanData.getRLD());
//        curAmpData.setLLD(curCanData.getLLD());
//        curAmpData.setTError(curCanData.getTError());
//        curAmpData.setUserTorque(curCanData.getUserTorque());
//        curAmpData.setTotalTorque(curCanData.getTotalTorque());
//        curAmpData.setTimestamp(curCanData.getTimeStamp());
//        App.get().getDB().ampDataDAO().insertAll(curAmpData);
//    }

    @NonNull
    private can_data readDataFromElm() {
        StringBuilder res = new StringBuilder();
        byte b = 0;
        char c;
        byte[] elmDataReady = new byte[ numOfBytes ];
//        String temp;
//        boolean bufferFullHit = false;

        HashMap<Integer, String> IdDataMap = new HashMap<Integer, String>();

        while( myService.isRunning() ) {
            try {

                b = (byte) elmInputStream.read();

            } catch (Exception e) {
                e.printStackTrace();
                if (!myService.isRunning()) {
                    return null;
                } else {
                    //try to re-establish bluetooth connection and set up the elm
                    reattemptAndSetUpElm();
                }


            }
            c = (char) b;
            if (c == '>' || c == '<' || c == '\r')
            {
//                if (!res.toString().equals("DATA ERROR")) {
                final String byteData = res.toString().replaceAll("(\n" +
                        "|\r" + "|<" + "|\\bAT\\s?MA\\b" + "|\\s+" + "|>"+
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


                            if (didGrabAllMsgs(byteData, IdDataMap)) {
                                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((MainActivity) ctxUi).incrementRowVal("properBlock", " # Proper Blocks Received from ELM ", "1");
                                    }
                                });
                                return decodeHexData(IdDataMap, elmDataReady);
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
                            Log.d(TAG, "Example incomplete: " + byteData);
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

//    private void displayEverything(final can_data curCanData) {
//        Log.d(TAG, "displaying Data on UI Thread");
//
//        ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                ((MainActivity) ctxUi).canBUSUpdateCanData(curCanData);
//            }
//        });
//
//
//
//
//    }

    private can_data decodeHexData(HashMap<Integer, String> IdDataMap, byte[] elmDataReady) {


        can_data decodedCanData = new can_data();
        ArrayList<Integer> idArr = new ArrayList<Integer>();
        Set<Integer> mySet = IdDataMap.keySet();
        idArr.addAll(mySet);
        Collections.sort(idArr);
        int speed =10;
//        HashMap<String, Object> decodedDataMap = new HashMap<String, Object>();
        for(Integer curKey: idArr) {
            Long curTimeStamp = System.currentTimeMillis();
            Date curDate = new Date(curTimeStamp);

            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String strDate = sdfDate.format(curDate);
            displayOnUi("Timestamp",strDate , speed);

            String curVal= IdDataMap.get(curKey);
            String  zerothByte = curVal.substring(0,2);
            String firstByte = curVal.substring(2,4);
            String secondByte = curVal.substring(4,6);
            String thirdByte = curVal.substring(6,8);
            String fourthByte = curVal.substring(8,10);
            String fifthByte = curVal.substring(10,12);
            String sixthByte = curVal.substring(12,14);
            String seventhByte = curVal.substring(14,16);

            switch (curKey) {
                case 0x500:
//                    500:
//                    0: LQ (upper 4 bits represents left lane, lower 4 bits is right lane.. not used in Toyota)
//                    1: Trim (Should never really go beyond +/- 20 or so) trim for dead center on steering
//                    2: LLD (Left lane distance 0-50ish 255 if not present. Low 30s is usually center. 15-16 is edge.)
//                    3: RLD (Right lane distance 0-50ish 255 if not present. Low 30s is usually center, 15-16 is edge.)
//                    4: XD (lateral motion, +-1, 2, 3 is moderate motion, +/-10, 11, 12 is aggressive, 18+ crazy)
//                    5: Curve (upper 8 bits)
//                    6: Curve (lower 4 bits at top) (0x08, 0x04 2 bits turn signal, 0x02 enabled, 0x01 lanes present)
//                    7: speed mph
//                    Python Code
//                    LLQ = int(zerothByte[0],16)
//                    RLQ = int(zerothByte[1],16)
//                    trim = sxtn( int(firstByte, 16), len(firstByte)*4)
//                    LLD = int(secondByte, 16)
//                    RLD = int(thirdByte, 16)
//                    xD = sxtn( int(fourthByte, 16), len(fourthByte)*4)
//                    curve = sxtn( int(fifthByte+sixthByte[0], 16), len(fifthByte+sixthByte[0])*4)
//                    speed = int(seventhByte, 16)
                    int LLQ = Integer.parseInt( zerothByte.substring(0,1), 16);
                    decodedCanData.setLLQ(LLQ);
                    int RLQ = Integer.parseInt( zerothByte.substring(1,2), 16);
                    decodedCanData.setRLQ(RLQ);
                    int trim = signConversion( Integer.parseInt( firstByte, 16), firstByte.length()*4);
                    decodedCanData.setTrim(trim);
                    int LLD = Integer.parseInt( secondByte, 16);
                    decodedCanData.setLLD(LLD);
                    int RLD = Integer.parseInt( thirdByte, 16);
                    decodedCanData.setRLD(RLD);
                    int XD = signConversion(Integer.parseInt( fourthByte, 16), fourthByte.length()*4);
                    decodedCanData.setXD(XD);
                    int curve = signConversion(Integer.parseInt( fifthByte+sixthByte.substring(0,1), 16), (fifthByte+sixthByte.substring(0,1)).length()*4);
//                    Log.d(TAG, "Message 500: " + curVal);
//                    Log.d(TAG,"curvature HEX data: " +  fifthByte+sixthByte.substring(0,1));
                    decodedCanData.setCurve(curve);
                    speed = Integer.parseInt( seventhByte, 16);
                    decodedCanData.setSpeed(speed);

                    displayOnUi("LLQ",  Integer.toString(decodedCanData.getLLQ()), speed);
                    displayOnUi("RLQ", Integer.toString( decodedCanData.getRLQ()), speed);
                    displayOnUi("trim", Integer.toString(decodedCanData.getTrim()), speed);
                    displayOnUi("LLD", Integer.toString(decodedCanData.getLLD()), speed);
                    displayOnUi("RLD", Integer.toString(decodedCanData.getRLD()), speed);
                    displayOnUi("XD", Integer.toString(decodedCanData.getXD()), speed);
                    displayOnUi("curve", Integer.toString(decodedCanData.getCurve()), speed);
                    displayOnUi("speed", Integer.toString(decodedCanData.getSpeed()), speed);


                    break;
                case 0x501:
//                    501:
//                    0: Tangle (Target angle upper 8)
//                    1: Tangle (lower 4 at top), steering angle (upper 4 at bottom)
//                    2: steering angle (lower 8)
//                    3: steering rate*kd (upper 8)
//                    4: steering rate*kd (lower 4 at top), angle_int/ki (upper 4 at bottom)
//                    5: angle_int/ki (lower 8)
//                    6: error (Tangle-steering angle)*kp (upper 8)
//                    7: error (lower 4 at top) + 0x08 backoff on.
//                    python code
//                    tAngle = sxtn( int(zerothByte+firstByte[0], 16), len(zerothByte+firstByte[0])*4)
//                    sAngle = sxtn( int(firstByte[1]+secondByte, 16), len(firstByte[1]+secondByte)*4)
//                    sRate = sxtn( int(thirdByte+fourthByte[0], 16), len(thirdByte+fourthByte[0])*4)
//                    tErrorIntegral = sxtn( int(fourthByte[1]+fifthByte, 16), len(fourthByte[1]+fifthByte)*4)
//                    tError = sxtn( int(sixthByte+seventhByte[0], 16), len(sixthByte+seventhByte[0])*4)
//                    backOff = int(seventhByte[1],16) & 0x0C
                    int tangle = signConversion( Integer.parseInt( zerothByte+firstByte.substring(0,1), 16), (zerothByte+firstByte.substring(0,1)).length()*4) ;
                    decodedCanData.setTAngle(tangle);

                    int sAngle = signConversion( Integer.parseInt( firstByte.substring(1,2)+secondByte, 16), (firstByte.substring(1,2)+secondByte).length()*4) ;
                    decodedCanData.setSAngle(sAngle);

                    int sRate = signConversion( Integer.parseInt( thirdByte + fourthByte.substring(0,1), 16), (thirdByte + fourthByte.substring(0,1)).length()*4);
                    decodedCanData.setSRate(sRate);


                    int tErrorIntegral = signConversion( Integer.parseInt( fourthByte.substring(1,2)+fifthByte, 16), (fourthByte.substring(1,2)+fifthByte).length()*4);
                    decodedCanData.setTErrorIntegral(tErrorIntegral);

                    int tError = signConversion( Integer.parseInt(sixthByte+seventhByte.substring(0,1), 16), (sixthByte+seventhByte.substring(0,1)).length()*4);
                    decodedCanData.setTError(tError);

                    displayOnUi("tAngle", Integer.toString(decodedCanData.getTAngle()), speed);
                    displayOnUi("sAngle", Integer.toString(decodedCanData.getSAngle()), speed);
                    displayOnUi("sRate", Integer.toString(decodedCanData.getSRate()), speed);
                    displayOnUi("tErrorIntegral", Integer.toString(decodedCanData.getTErrorIntegral()), speed);
                    displayOnUi("tError", Integer.toString(decodedCanData.getTError()), speed);


                    break;
                case 0x503:
//                    0: Commandtorque (upper 8)
//                    1: Commandtorque (lower 4 at top), user torque (upper 8 at bottom)
//                    2: user torque (lower 8)
//                    3: total torque (upper 8)
//                    4: total torque (lower 4 at top) + EPS status (TBD bottom)
//                    5: future radar distance
//                    6: future radar azimuth
//                    7: VERSION (upper 3 bits vehicle, lower 5 version)
//                    Python Code
//                    cmdTorque = sxtn( int(zerothByte+firstByte[0], 16), len(zerothByte+firstByte[0])*4)
//                    userTorque = sxtn( int(firstByte[1]+secondByte, 16), len(firstByte[1]+secondByte)*4)
//                    totalTorque = sxtn( int(thirdByte+fourthByte[0], 16), len(thirdByte+fourthByte[0])*4)
                    int commandTorque = signConversion( Integer.parseInt( zerothByte + firstByte.substring(0,1), 16), (zerothByte + firstByte.substring(0,1)).length()*4);
                    decodedCanData.setCommandTorque(commandTorque);

                    int userTorque =  signConversion( Integer.parseInt( firstByte.substring(1,2) + secondByte, 16), (firstByte.substring(1,2) + secondByte).length()*4);
                    decodedCanData.setUserTorque(userTorque);

                    int totalTorque = signConversion( Integer.parseInt(thirdByte+fourthByte.substring(0,1), 16),(thirdByte+fourthByte.substring(0,1)).length()*4 );
                    decodedCanData.setTotalTorque(totalTorque);

                    displayOnUi("commandTorque", Integer.toString(decodedCanData.getCommandTorque()), speed);
                    displayOnUi("userTorque", Integer.toString(decodedCanData.getUserTorque()), speed);
                    displayOnUi("totalTorque", Integer.toString(decodedCanData.getTotalTorque()), speed);
//

                    break;
                default:
                    break;
            }
//            String curHex = IdDataMap.get(curKey)
//            int pos = IDArray.indexOf(curKey);
//            if (pos>-1)
//                System.arraycopy(hexStringToByteArray(IdDataMap.get(curKey)), 0,elmDataReady , pos * ( IdDataMap.get(curKey).length() /2 ), IdDataMap.get(curKey).length()/2);

//            String curHexArr = IdDataMap.get(curKey);
//            elmDataReady[pos*8:pos*8+7] = hexStringToByteArray(IdDataMap.get(curKey));
//            byteBlock.append();
        }
        return decodedCanData; //byteBlock.toString();
    }

    private void writeToReaderThread(ArrayList<can_data> canDataLs) {
        try {
            myQ.put(canDataLs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayOnUi(final String variableName, final String variableValue, int speed) {
        if ( speed >= thresholdSpeed ) {
//            ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    ((MainActivity) ctxUi).canBUSUpdate(variableName, variableName, variableValue);
//                }
//            });
        }
    }
    private int signConversion(int x, int bits) {
        int h = 1 << (bits - 1);
        int m = (1 << bits) - 1;
        return ((x + h) & m) - h;
    }

    private void reattemptAndSetUpElm() {
        try {
            Log.d(TAG, "stopping the existing service gracefully");
            this.myService.stopService();
            Log.d(TAG, "start a new service with a new connection");
            this.myService.startService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
