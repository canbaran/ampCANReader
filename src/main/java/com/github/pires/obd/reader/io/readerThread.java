package com.github.pires.obd.reader.io;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.github.pires.obd.reader.activity.MainActivity;

import java.io.PipedInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import amp.internal.io.CanMessage;

import static android.content.ContentValues.TAG;

/**
 * Created by canbaran on 12/21/17.
 */

public class readerThread extends Thread {


        private final BlockingQueue<ArrayList<can_data>> myQ;
        private final ArrayBlockingQueue<ArrayList<can_data>> myInternalQ = new ArrayBlockingQueue<ArrayList<can_data>>(1024/4);
        private Context appContext;
        private Context ctxUi;
        private ObdGatewayService myService;

//        private CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
//                getApplicationContext(),
//                "us-east-1:2ee7fe14-536e-4291-898a-e8408bce1040", // Identity pool ID
//                Regions.US_EAST_1 // Region
//        );
//        private AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);

        public readerThread(BlockingQueue<ArrayList<can_data>> incomingQ, Context context, Context ctx2,
                            ObdGatewayService myIncomingService)
        {
            myQ = incomingQ;
            appContext = context;
            ctxUi = ctx2;
            myService = myIncomingService;
        }

        public void run()
        {
            try
            {
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

                    myQ.drainTo(myInternalQ, myInternalQ.remainingCapacity());

                    while (myInternalQ.size()>0) {
                        ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((MainActivity) ctxUi).canBUSUpdate( "blocksRemaining",  "# of Blocks Remaining",  Integer.toString(myInternalQ.size()));
                            }
                        });
                        ArrayList<can_data> canDataLs = myInternalQ.take();
                        ArrayList<can_data> cleanArr = removeDuplicateTs(canDataLs);
                        Long a = System.currentTimeMillis();
                        try {
                            mapper.batchSave(cleanArr);
//                            if (temp.size() == canDataLs.size()) {
//                                SubmissionStatus = "Nothing is sent";
//                             } else if ( temp.size() == 0 ) {
//                                SubmissionStatus = "Batch Fully Sent";
//                            } else {
//                                SubmissionStatus = "Batch Partially sent";
//                            }
                            myService.setBatchCount( myService.getBatchCount() + cleanArr.size());
                        } catch (Exception e) {
                            e.printStackTrace();
//                            SubmissionStatus = "Failed to Send";
                        }
                        final String submissionStatus2 = "# of Blocks Sent:" + Integer.toString(myService.getBatchCount());
                        ((MainActivity) ctxUi).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((MainActivity) ctxUi).canBUSUpdate( "AWS_UPLOAD",  "AWS_UPLOAD",  submissionStatus2);
                            }
                        });

                        Long b = System.currentTimeMillis();
                        Log.d(TAG, this.getName() + " Time to upload to AWS: "+ Long.toString(b-a) + " [ms] " + Integer.toString(canDataLs.size()  ) + " elements" + " per element " + Double.toString( (b-a) / (canDataLs.size() )  ) + " [ms]");
//                        Log.d(TAG, this.getName()+ " The Size of the main queue is " + myQ.size());
//					}
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


}
