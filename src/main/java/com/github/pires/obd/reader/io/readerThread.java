package com.github.pires.obd.reader.io;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.github.pires.obd.reader.App;
import com.github.pires.obd.reader.activity.MainActivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.github.pires.obd.reader.database.entity.ampData;


import static android.content.ContentValues.TAG;
import static com.github.pires.obd.reader.io.uiNotificationIds.awsUploadStatus;
import static java.lang.Math.round;

/**
 * Created by canbaran on 12/21/17.
 */

public class readerThread extends Thread {


        private final BlockingQueue<ArrayList<can_data>> myQ;
        private final ArrayBlockingQueue<ArrayList<can_data>> myInternalQ = new ArrayBlockingQueue<ArrayList<can_data>>(1024/4);
        private Context appContext;
        private Context ctxUi;
        private ObdGatewayService myService;
        private long startTime;

//        private CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
//                getApplicationContext(),
//                "us-east-1:2ee7fe14-536e-4291-898a-e8408bce1040", // Identity pool ID
//                Regions.US_EAST_1 // Region
//        );
//        private AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);

        public readerThread(BlockingQueue<ArrayList<can_data>> incomingQ, Context context, Context ctx2,
                            ObdGatewayService myIncomingService,
                            long incomingStartTime)
        {
            myQ = incomingQ;
            appContext = context;
            ctxUi = ctx2;
            myService = myIncomingService;
            startTime = incomingStartTime;
        }

        public void run()
        {
            try
            {
//                ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ((MainActivity) ctxUi).canBUSUpdate( awsUploadStatus,  awsUploadStatus,  "Uploader Started");
//                    }
//                });
                CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                        appContext,
                        "us-east-1:2ee7fe14-536e-4291-898a-e8408bce1040", // Identity pool ID
                        Regions.US_EAST_1 // Region
                );
                AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
                DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

                String SubmissionStatus = "sent";
                List<DynamoDBMapper.FailedBatch> temp = null;
                while (myService.isRunning() || myQ.size()>0) {
//                    ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            ((MainActivity) ctxUi).canBUSUpdate( awsUploadStatus,  awsUploadStatus,  "AWS Objects Created");
//                        }
//                    });
                    myQ.drainTo(myInternalQ, myInternalQ.remainingCapacity());

                    while (myInternalQ.size()>0) {
//                        ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                ((MainActivity) ctxUi).canBUSUpdate( awsUploadStatus,  awsUploadStatus,  Integer.toString(myInternalQ.size()));
//                            }
//                        });
                        Log.d(TAG, this.getName()+ " Working " + Integer.toString( myInternalQ.size())
                                + "Elements to upload");
                        ArrayList<can_data> canDataLs = myInternalQ.take();
                        ArrayList<can_data> cleanArr = removeDuplicateTs(canDataLs);
                        Long a = System.currentTimeMillis();
                        try {
                            storeInternal(cleanArr);
                            mapper.batchSave(cleanArr);
                            myService.setBatchCount( myService.getBatchCount() + cleanArr.size());
                            //right at thos moment store cleanArr to our internal db as well

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Long b = System.currentTimeMillis();
                        double elapsedTime = (b-startTime)/1e3;
                        final String submissionStatus2 = Double.toString(round( myService.getBatchCount()  / elapsedTime ) );
                        ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((MainActivity) ctxUi).canBUSUpdate( awsUploadStatus,  awsUploadStatus,  submissionStatus2);
                            }
                        });

                        Log.d(TAG, this.getName() + " Time to upload to AWS: "+ Long.toString(b-a) + " [ms] " + Integer.toString(canDataLs.size()  ) + " elements" + " per element " + Double.toString( (b-a) / (canDataLs.size() )  ) + " [ms]");
                    }
                }

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }


    private ArrayList<can_data> removeDuplicateTs( ArrayList<can_data> canDataLs) {
//			ArrayList<Long> myTs = new ArrayList<Long>();
//			ArrayList<can_data> cleanArr = new ArrayList<can_data>();

        Set<can_data> canDataSet = new TreeSet<can_data>(new CanDataComparator());
        ArrayList<can_data> cleanArr = new ArrayList<can_data>();
        for(can_data a: canDataLs){
            if(canDataSet.add(a)) {
                cleanArr.add(a);
            } else {
                Log.d(TAG, "debugging entered duplicate array");
            }
        }
        return cleanArr;
    }
    public class CanDataComparator implements Comparator<can_data>
    {
        public int compare(can_data c1, can_data c2)
        {
            return (int) (c1.getTimeStamp()-c2.getTimeStamp());
        }
    }
    private void storeInternal(ArrayList<can_data> cleanArr) {
//        List<ampData> list = new ArrayList<>();
        ampData[] list = new ampData[cleanArr.size()];

        Log.d(TAG, "about to store internally an array of size: " + Integer.toString(cleanArr.size()));
        for (int i = 0; i < cleanArr.size(); i++) {
            ampData curAmpData = new ampData();

            can_data curCanData = cleanArr.get(i);

//            Log.d(TAG, "Reader Thread Timestamp: " + Long.toString(curCanData.getTimeStamp()));

            curAmpData.setCommandTorque(curCanData.getCommandTorque());
            curAmpData.setCurve(curCanData.getCurve());
            curAmpData.setXD(curCanData.getXD());
            curAmpData.setRLD(curCanData.getRLD());
            curAmpData.setLLD(curCanData.getLLD());
//            curAmpData.setTAngle(curCanData.getTAngle());
            curAmpData.setTError(curCanData.getTError());
            curAmpData.setUserTorque(curCanData.getUserTorque());
            curAmpData.setTotalTorque(curCanData.getTotalTorque());
            curAmpData.setTimestamp(curCanData.getTimeStamp());
            curAmpData.setIsPlotted(false);

            list[i] = curAmpData;
        }

        // insert product list into database
//        MyDatabase db = ((MainActivity) ctxUi).getDB();
        App.get().getDB().ampDataDAO().insertAll(list);
//        List<ampData> test = db.ampDataDAO().getAll();

        // disable flag for force update
//        App.get().setForceUpdate(false);
    }


}
