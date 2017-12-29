package com.github.pires.obd.reader.io;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.github.pires.obd.reader.activity.MainActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

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


//    private Handler hdForUi;

    public writerThread(BlockingQueue<ArrayList<can_data>> incomingQ, InputStream elmInput, OutputStream elmOutput, Context ctx,
                        String incomingHexID,
                        String incomingVehicleID,
                        String incomingUserName,
                        ObdGatewayService incomingService)
    {

        elmInputStream = elmInput;
        elmOutputStream = elmOutput;
        ctxUi = ctx;
        myQ = incomingQ;

        hexID = incomingHexID;
        vehicleID = incomingVehicleID;
        userName = incomingUserName;
        myService = incomingService;


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

                    curCanData[i] =  readDataFromElm();
                    long t2 = System.nanoTime();
                    if (curCanData.equals("Exception Occured"))
                        break;

                }
                Long b = System.nanoTime();
                long avgTimeMillis = (b-a)/(blockSize*1000000);
                for (int i=0; i<blockSize; i++) {
                    can_data curData = new can_data(); // us-east-1:0d327241-156d-45e2-9560-4ce6c9192613
                    curData.setTimeStamp(loopStartTimeStamp+i*avgTimeMillis);
                    curData.setData(curCanData[i]);
                    curData.setCanID(hexID);
                    curData.setVIN(vehicleID);
                    curData.setCanIDMeaning("Still Hard Coded");
                    curData.setGPS("MY GPS");
                    canDataLs.add(curData);
                }
                Log.d(TAG, Integer.toString(blockSize) + " points produced: " + Long.toString( (b-a) / (blockSize*1000000) ) + " [ms] per point" );
                writeToReaderThread(canDataLs);
            }


        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private String readDataFromElm() {
        StringBuilder res = new StringBuilder();
        byte b = 0;
        char c;
        String temp;

        while( true ) {
            try {

                b = (byte) elmInputStream.read();
            } catch (Exception e) {
                e.printStackTrace();
                return "Exception Occured";
            }
            c = (char) b;
            if (c == '>' || c == '<' || c == '\r') // read until '>' arrives
            {
                if (!res.toString().equals("DATA ERROR")) {
                    final String byteData = res.toString().replaceAll("(\n" +
                            "|\r" +
                            "|\\bDATA ERROR\\b)", "");
                    ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((MainActivity) ctxUi).canBUSUpdate( "LISTEN_CAN",  "LISTEN_CAN", byteData );
                        }
                    });
//                    for (int i=0;i<1e4;i++)
//                        temp= "introduce delay";

                    //assign the timestamp right at this moment
//                    timeStamp = threadStartTimeStamp + SystemClock.currentThreadTimeMillis();//System.currentTimeMillis();
//                    Log.d(TAG, "Assigned timestamp: "  + Long.toString(timeStamp));
                    return byteData;
                }
            }
            res.append(c);
        }
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
