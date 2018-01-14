package com.github.pires.obd.reader.io;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.util.MutableBoolean;
import android.widget.Toast;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.control.ModuleVoltageCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.UnsupportedCommandException;
import com.github.pires.obd.reader.R;
import com.github.pires.obd.reader.activity.ConfigActivity;
import com.github.pires.obd.reader.activity.MainActivity;
import com.github.pires.obd.reader.io.BluetoothManager;
import com.github.pires.obd.reader.io.ObdCommandJob.ObdCommandJobState;
import com.google.inject.Inject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import amp.internal.io.CanMessage;

import static android.content.ContentValues.TAG;

/**
 * This service is primarily responsible for establishing and maintaining a
 * permanent connection between the device where the application runs and a more
 * OBD Bluetooth interface.
 * <p/>
 * Secondarily, it will serve as a repository of ObdCommandJobs and at the same
 * time the application state-machine.
 */


public class ObdGatewayService extends AbstractGatewayService {

    private int queueSize = 512;
    private int threadCount = 2;
//    private BooleanHolder isMyRunning = new BooleanHolder();
    private int transmittedBatchCount = 0;
//    private HashMap<String, Boolean> myMap = new HashMap<String, Boolean>();

    private static final String TAG = ObdGatewayService.class.getName();
    private final IBinder binder = new ObdGatewayServiceBinder();

    @Inject
    SharedPreferences prefs;

    private BluetoothDevice dev = null;
    private BluetoothSocket sock = null;
    private BluetoothSocket sockFallback = null;
    private AmazonDynamoDBClient ddbClient;
    private ArrayList<Integer> IDArr = new ArrayList<Integer>();
    private int indexKey;
    private String CM = "";
    private String CF = "";
    //
    DynamoDBMapper mapper;





    public void startService() throws IOException {
        Log.d(TAG, "Starting service..");
        CF = prefs.getString(ConfigActivity.CF_hex, "7ff");
        ((MainActivity) ctx).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((MainActivity) ctx).canBUSUpdate("BaseCanID", "Base Can ID:",
                        CF + " (CF" + CF +")");
            }
        });
        String CMEnum = prefs.getString(ConfigActivity.CM_LIST_KEY, null);
        if (CMEnum!=null) {
            switch (CMEnum) {
                case "1":
                    CM = "7FF";
                    break;
                case "2":
                    CM = "7FE";
                    break;
                case "4":
                    CM = "7FC";
                    break;
                case "8":
                    CM = "7F8";
                    break;
                case "16":
                    CM = "7F0";
                    break;
                default:
                    CM = "7FF";
            }
        } else {
            CM ="7FF";
        }

        // get the remote Bluetooth device
        final String remoteDevice = prefs.getString(ConfigActivity.BLUETOOTH_LIST_KEY, null);
        if (remoteDevice == null || "".equals(remoteDevice)) {
            Toast.makeText(ctx, getString(R.string.text_bluetooth_nodevice), Toast.LENGTH_LONG).show();

            // log error
            Log.e(TAG, "No Bluetooth device has been selected.");

            // TODO kill this service gracefully
            stopService();
            throw new IOException();
        } else {

            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(),
                    "us-east-1:2ee7fe14-536e-4291-898a-e8408bce1040", // Identity pool ID
                    Regions.US_EAST_1 // Region
            );
            ddbClient = new AmazonDynamoDBClient(credentialsProvider);
            mapper = new DynamoDBMapper(ddbClient);

            final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            dev = btAdapter.getRemoteDevice(remoteDevice);


    /*
     * Establish Bluetooth connection
     *
     * Because discovery is a heavyweight procedure for the Bluetooth adapter,
     * this method should always be called before attempting to connect to a
     * remote device with connect(). Discovery is not managed by the Activity,
     * but is run as a system service, so an application should always call
     * cancel discovery even if it did not directly request a discovery, just to
     * be sure. If Bluetooth state is not STATE_ON, this API will return false.
     *
     * see
     * http://developer.android.com/reference/android/bluetooth/BluetoothAdapter
     * .html#cancelDiscovery()
     */
            Log.d(TAG, "Stopping Bluetooth discovery.");
            btAdapter.cancelDiscovery();

            showNotification(getString(R.string.notification_action), getString(R.string.service_starting), R.drawable.ic_btcar, true, true, false);

            try {
                startObdConnection();
            } catch (Exception e) {
                Log.e(
                        TAG,
                        "There was an error while establishing connection. -> "
                                + e.getMessage()
                );

                // in case of failure, stop this service.
                stopService();
                throw new IOException();
            }
            showNotification(getString(R.string.notification_action), getString(R.string.service_started), R.drawable.ic_btcar, true, true, false);
        }

     /*
     * TODO clean
     *
     * Get more preferences
     */
        ArrayList<ObdCommand> cmds = ConfigActivity.getObdCommands(prefs);

    }

    /**
     * Start and configure the connection to the OBD interface.
     * <p/>
     * See http://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3/18786701#18786701
     *
     * @throws IOException
     */
    private void startObdConnection() throws IOException {
        Log.d(TAG, "Starting OBD connection..");
        isRunning = true;
        try {
        	sock = BluetoothManager.connect(dev);
        } catch (Exception e2) {
        	Log.e(TAG, "There was an error while establishing Bluetooth connection. Stopping app..", e2);
        	stopService();
        	throw new IOException();
        }

        // Let's configure the connection.
        Log.d(TAG, "Queueing jobs for connection configuration..");


//        queueJob(new ObdCommandJob(new MonitorAllCommand()));

    /*
     * Will send second-time based on tests.
     *
     * TODO this can be done w/o having to queue jobs by just issuing
     * command.run(), command.getResult() and validate the result.
     */


        queueJob(new ObdCommandJob(new ObdResetCommand()));
        queueJob(new ObdCommandJob(new ObdFormatCommand()));
        queueJob(new ObdCommandJob(new SpaceOffCommand()));
        queueJob(new ObdCommandJob(new EchoOffCommand()));
        queueJob(new ObdCommandJob(new HeaderOnCommand()));
        queueJob(new ObdCommandJob(new FilterCan("CF", CF)));


        queueJob(new ObdCommandJob(new FilterCan("CM", CM)));


        queueJob(new ObdCommandJob(new MonitorAllCommand()));

        queueCounter = 0L;
        Log.d(TAG, "Initialization jobs queued.");


    }
//    private void writeToReaderThread(String curCanData, PipedOutputStream os) {
//        //write to the other thread
//        CanMessage.canMessage.Builder  curCanMsg = CanMessage.canMessage.newBuilder();
//        curCanMsg.setCanData(curCanData);
//        curCanMsg.setTimestamp(System.currentTimeMillis());
//        try {
//            curCanMsg.build();
//            curCanMsg.build().writeDelimitedTo(os);
//            System.out.println("[COMPLETED] WriterThread submitted: " + curCanData + " at " + Long.toString(System.currentTimeMillis()));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void setBatchCount(int number) {
        this.transmittedBatchCount = number;
    }

    public int getBatchCount() {
        return this.transmittedBatchCount;
    }





    /**
     * This method will add a job to the queue while setting its ID to the
     * internal queue counter.
     *
     * @param job the job to queue.
     */
    @Override
    public void queueJob(ObdCommandJob job) {
        // This is a good place to enforce the imperial units option
        job.getCommand().useImperialUnits(prefs.getBoolean(ConfigActivity.IMPERIAL_UNITS_KEY, false));

        // Now we can pass it along
        super.queueJob(job);
    }

    /**
     * Runs the queue until the service is stopped
     */
    protected void executeQueue() throws InterruptedException {
        Log.d(TAG, "Executing queue..");

        while (!Thread.currentThread().isInterrupted()) {
            ObdCommandJob job = null;
            try {
                job = jobsQueue.take();

                // log job
                Log.d(TAG, "Taking job[" + job.getId() + "] from queue..");

                if (job.getState().equals(ObdCommandJobState.NEW)) {
                    Log.d(TAG, "Job state is NEW. Run it..");
                    job.setState(ObdCommandJobState.RUNNING);
                    if ( job.getCommand().getName().equals("Monitoring everything") ) {

                        BlockingQueue<ArrayList<com.github.pires.obd.reader.io.can_data>> queue = new ArrayBlockingQueue<ArrayList<com.github.pires.obd.reader.io.can_data>>(queueSize);


                        ArrayList<Thread> tLS = new ArrayList<Thread>();


                        for (int i=0; i<threadCount; i++ ) {
                            readerThread readerThread = new readerThread(queue, getApplicationContext(),
                                    ctx,
                                    this);
                            readerThread.setName("Uploader Thread " + Integer.toString(i));
                            tLS.add(readerThread);
                        }

                        createIDArr();

                        writerThread writerThread = new writerThread(queue, sock.getInputStream(),
                                sock.getOutputStream(), ctx,
//                                prefs.getString(ConfigActivity.CRA_hex, ""),
                                prefs.getString(ConfigActivity.VEHICLE_ID_KEY, ""),
                                prefs.getString(ConfigActivity.userName, ""),
                                this,
                                IDArr,
                                indexKey);
                        writerThread.setName("Writer Thread");




                        for (int i=0; i<tLS.size(); i++) {
                            tLS.get(i).start();
                        }

                        writerThread.start();

                        for (int i=0; i<tLS.size(); i++) {
                            tLS.get(i).join();
                        }

                        writerThread.join();


                    } else {
                        job.getCommand().run(sock.getInputStream(), sock.getOutputStream());
                    }
                } else
                    // log not new job
                    Log.e(TAG,
                            "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
            } catch (InterruptedException i) {
                Thread.currentThread().interrupt();
            } catch (UnsupportedCommandException u) {
                if (job != null) {
                    job.setState(ObdCommandJobState.NOT_SUPPORTED);
                }
                Log.d(TAG, "Command not supported. -> " + u.getMessage());
            } catch (Exception e) {
                if (job != null) {
                    job.setState(ObdCommandJobState.EXECUTION_ERROR);
                }
                Log.e(TAG, "Failed to run command. -> " + e.getMessage());
            }

            if (job != null) {
                final ObdCommandJob job2 = job;
                ((MainActivity) ctx).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((MainActivity) ctx).stateUpdate(job2);
                    }
                });
            }
        }
    }

    private void createIDArr() {
        String CF = prefs.getString(ConfigActivity.CF_hex, "7ff");
//        String CM = prefs.getString(ConfigActivity.CM_hex, "7ff");
        int CFHex = Integer.parseInt(CF, 16);
//        int CMHex = Integer.parseInt(CM, 16);
        int index = 0; //0x7ff-CMHex;
        for(int i = 0x00; i<= index; i++) {
            IDArr.add(CFHex+i);
        }
        indexKey = Collections.max(IDArr);
        Log.d(TAG,  "index KEY: " + Integer.toString(indexKey));
//        IDArr.add(0x159);
//        IDArr.add(0x179);
//        IDArr.add(0x199);
    }

    /**
     * Stop OBD connection and queue processing.
     */
    public void stopService() {
        Log.d(TAG, "Stopping service..");

        notificationManager.cancel(NOTIFICATION_ID);
        jobsQueue.removeAll(jobsQueue); // TODO is this safe?
        isRunning = false;

        if (sock != null)
            // close socket
            try {
                sock.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

        // kill service
        stopSelf();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public class ObdGatewayServiceBinder extends Binder {
        public ObdGatewayService getService() {
            return ObdGatewayService.this;
        }
    }
    public static void saveLogcatToFile(Context context, String devemail) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{devemail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "OBD2 Reader Debug Logs");
        
        StringBuilder sb = new StringBuilder();
        sb.append("\nManufacturer: ").append(Build.MANUFACTURER);
        sb.append("\nModel: ").append(Build.MODEL);
        sb.append("\nRelease: ").append(Build.VERSION.RELEASE);

        emailIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());

        String fileName = "OBDReader_logcat_"+System.currentTimeMillis()+".txt";
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + File.separator + "OBD2Logs");
        dir.mkdirs();
        File outputFile = new File(dir,fileName);
        Uri uri = Uri.fromFile(outputFile);
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);

        Log.d("savingFile", "Going to save logcat to " + outputFile);
        //emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(emailIntent, "Pick an Email provider").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

        try {
            @SuppressWarnings("unused")
            Process process = Runtime.getRuntime().exec("logcat -f "+outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @DynamoDBTable(tableName = "canData")
    public class can_data {
        private String vin;
        private long timestamp;
        private String gps;
        private String data;
        private String canID;
        private String canIDMeaning;
        private String userName;


        @DynamoDBRangeKey(attributeName = "timestamp") //DynamoDBIndexRangeKey
        public long getTimeStamp() {
            return timestamp;
        }

        public void setTimeStamp(long timestamp) {
            this.timestamp = timestamp;
        }

        @DynamoDBHashKey(attributeName = "vin") //DynamoDBIndexHashKey
        public String getVIN() {
            return vin;
        }

        public void setVIN(String vin) {
            this.vin = vin;
        }

        @DynamoDBAttribute(attributeName = "gps")
        public String getGPS() {
            return gps;
        }

        public void setGPS(String gps) {
            this.gps = gps;
        }

        @DynamoDBAttribute(attributeName = "data")
        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        @DynamoDBAttribute(attributeName = "UserName")
        public String getUserName() {
            return userName;
        }

        public void setUserName(String UserName) {
            this.userName = UserName;
        }

        @DynamoDBAttribute(attributeName = "canID") //DynamoDBHashKey
        public String getID() {
            return canID;
        }

        public void setCanID(String canID) {
            this.canID = canID;
        }

        @DynamoDBAttribute(attributeName = "canIDMeaning")
        public String getCanIDMeaning() {
            return canIDMeaning;
        }

        public void setCanIDMeaning(String canIDMeaning) {
            this.canIDMeaning = canIDMeaning;
        }


    }
    
}
