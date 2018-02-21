package com.github.pires.obd.reader.activity;

import android.arch.persistence.room.Room;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.pires.obd.reader.App;
import com.github.pires.obd.reader.R;
import com.github.pires.obd.reader.database.MyDatabase;
import com.github.pires.obd.reader.database.entity.ampData;
import com.google.gson.Gson;

import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import roboguice.activity.RoboActivity;

import static com.github.pires.obd.reader.trips.TripLog.DATABASE_NAME;

/**
 * Created by canbaran on 2/14/18.
 */

public class VisualsActivity extends AppCompatActivity {
    private LineChart mChartXd;
    private LineChart mChartCenterOffset;
    private LineChart mChartCurvature;
    private ArrayList<LineChart> chartArr = new ArrayList<LineChart>();

    private Runnable mTimer1;
    private final Handler mHandler = new Handler();
    private long createSystemTime;
//    private Pusher pusher;
//
//    private static final String PUSHER_APP_KEY = "<INSERT_PUSHER_KEY>";
//    private static final String PUSHER_APP_CLUSTER = "<INSERT_PUSHER_CLUSTER>";
//    private static final String CHANNEL_NAME = "stats";
//    private static final String EVENT_NAME = "new_memory_stat";

//    private static final float TOTAL_MEMORY = 255f;
//    private static final float LIMIT_MAX_MEMORY = 20.0f;
    private static final float xdLim = 20;
    private static final float offsetLim = 18;
    private static final float curveLim = 250;

    private MyDatabase database;
    private long firstTimeStamp;
//    private ArrayList<Long> x = new ArrayList<Long>();
//    private ArrayList<Integer> y = new ArrayList<Integer>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plots_layout);
       //find the earliest time in db

        mChartXd = (LineChart) findViewById(R.id.chart_xD);
        mChartCurvature = (LineChart) findViewById(R.id.chart_curvature);
        mChartCenterOffset = (LineChart) findViewById(R.id.chart_offset);
        //configure Xd Chart
        setupChart(mChartXd);
        setupAxes(mChartXd, xdLim);
        setupData(mChartXd, Color.WHITE);
        setLegend(mChartXd);
        //configure curve Chart
        setupChart(mChartCurvature);
        setupAxes(mChartCurvature, curveLim);
        setupData(mChartCurvature, Color.RED);
        setLegend(mChartCurvature);
        //configure Offset Chart
        setupChart(mChartCenterOffset);
        setupAxes(mChartCenterOffset, offsetLim);
        setupData(mChartCenterOffset, Color.BLUE);
        setLegend(mChartCenterOffset);




//        chartArr.add(mChartXd);
//        chartArr.add(mChartCurvature);
//        chartArr.add(mChartCenterOffset);
//
//        for(LineChart i : chartArr) {
//            setupChart(i);
//            setupAxes(i);
//            setupData(i);
//            setLegend(i);
//        }


//        Intent intent = this.getIntent();
//        Bundle bundle = intent.getExtras();
//        database = ( MyDatabase) bundle.getSerializable("dbHandle");



        //database init

//        database = Room.inMemoryDatabaseBuilder(getApplicationContext(), MyDatabase.class, DATABASE_NAME))

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("visuals", "entering the thread");
                List<ampData> temp =  App.get().getDB().ampDataDAO().findByTimeStampInterval( (long) 0, System.currentTimeMillis() );
                firstTimeStamp = temp.get(0).getTimestamp();
                Log.d("visuals", Long.toString(firstTimeStamp));
                long previousTimeStamp = firstTimeStamp;
//                long x = 0;

//
                int count=1;
                while(true) {
                    List<ampData> myAmpDataLs =  App.get().getDB().ampDataDAO().findByTimeStampInterval(System.currentTimeMillis()-15*1000, System.currentTimeMillis() );
//                    addEntry(x, 1);
//                    x++;
                    Log.d("visuals", "size of the array from internal db: " + Integer.toString(myAmpDataLs.size()));
//                    Log.d("visuals", "inside forever while loop");
//                    Log.d("visuals", "beginning timestamp: " + Long.toString(createSystemTime) + " upper timestamp:" + Long.toString(System.currentTimeMillis()));
                    for (int i=0; i<myAmpDataLs.size(); i++) {
                        Long curX = myAmpDataLs.get(i).getTimestamp(); //- firstTimeStamp ) / 1000;
                        if (curX - previousTimeStamp > 100 ) { //do it at 10hz not 2hz
                            long x = ( curX - firstTimeStamp)/1000;
                            final int curXd = myAmpDataLs.get(i).getXD();
                            final int curCurvature = myAmpDataLs.get(i).getCurve();
                            final int curCenterOffset = calculateOffset(myAmpDataLs.get(i) );
                            //                    double f = mRand.nextDouble()*0.15+0.3;
                            //                    float y = (float) ( 10*(Math.sin(i*f+2) + mRand.nextDouble()*0.3));
                            Log.d("visuals", "in for loop:x= " + Long.toString(x) + " y= " + Float.toString(curXd));
                            final long x2 =x;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                    System.out.println("Received event with data: " + data);
//                                    Gson gson = new Gson();
//                                    Stat stat = gson.fromJson(data, Stat.class);
                                    addEntry(x2, curXd, mChartXd, "Xd");
                                    addEntry(x2, curCenterOffset, mChartCenterOffset, "CenterOffset");
                                    addEntry(x2, curCurvature, mChartCurvature, "Curvature");

                                }
                            });
//                            addEntry(x, y);
                            previousTimeStamp = curX;
                        }
//                        Log.d("visuals", "in for loop:x= " + Long.toString(curX) + " y= " + Float.toString(y));
                    }
                }
//                List<Product> products = App.get().getDB().productDao().getAll();
//                boolean force = App.get().isForceUpdate();
//                if (force || products.isEmpty()) {
//                    retrieveProducts();
//                } else {
//                    populateProducts(products);
//                }
            }
        }).start();

//        PusherOptions options = new PusherOptions();
//        options.setCluster(PUSHER_APP_CLUSTER);
//        pusher = new Pusher(PUSHER_APP_KEY);
//        Channel channel = pusher.subscribe(CHANNEL_NAME);

//        SubscriptionEventListener eventListener = new SubscriptionEventListener() {
//            @Override
//            public void onEvent(String channel, final String event, final String data) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        System.out.println("Received event with data: " + data);
//                        Gson gson = new Gson();
//                        Stat stat = gson.fromJson(data, Stat.class);
//                        addEntry(stat);
//                    }
//                });
//            }
//        };

//        channel.bind(EVENT_NAME, eventListener);
//        pusher.connect();

    }
    private int calculateOffset(ampData curAmpData ) {
        int centerOffset = 0;
        int LLDTarget = 0;
        int RLDTarget = 0;
        int curLLD = curAmpData.getLLD();
        int curRLD = curAmpData.getRLD();
        if (curLLD != 255 && curLLD > 2){
            if (curRLD != 255 && curRLD > 2) {
                if (curRLD<40) {
                    LLDTarget = (curLLD + curRLD) / 2;
                } else {
                    LLDTarget = (curLLD + 40)/2;
                }
            } else {
                LLDTarget = 35;
            }
        } else if(curRLD != 255 && curRLD>2) {
            RLDTarget = 32;
        }
        if (LLDTarget > 0 ) {
            centerOffset = LLDTarget - curLLD;
        } else if (RLDTarget > 0) {
            centerOffset = curRLD - RLDTarget;
        } else {
            centerOffset = -20;
        }

//        LLDTarget = 0;
//        RLDTarget = 0;
//        if((LLD!=255)&&(LLD>2)){
//            if((RLD!=255)&&(RLD>2)){
//                if(RLD<40){
//                    LLDTarget= (unsigned char)(LLD+RLD)/2;
//                }
//                else {
//                    LLDTarget = (unsigned char)(LLD+40)/2;
//                }
//            }
//            else {
//                LLDTarget = 35;
//            }
//        }
//        else if((RLD!=255)&&(RLD>2)){
//            RLDTarget = 32;
//        }
//
//
//
//        if(LLDTarget)
//            center_offset = LLDTarget-LLD;  // this is our error in our position control loop
//        else if(RLDTarget)
//            center_offset = RLD-RLDTarget;
//        else
//... no lanes.
        return centerOffset;
    }
    private void setupChart(LineChart curChart) {
        // disable description text
        curChart.getDescription().setEnabled(false);
        // enable touch gestures
        curChart.setTouchEnabled(true);
        // if disabled, scaling can be done on x- and y-axis separately
        curChart.setPinchZoom(true);
        // enable scaling
        curChart.setScaleEnabled(true);
        curChart.setDrawGridBackground(false);
        // set an alternative background color
        curChart.setBackgroundColor(Color.DKGRAY);
    }


    private void setupAxes(LineChart curChart, float relevantLim) {
        XAxis xl = curChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = curChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(relevantLim);
        leftAxis.setAxisMinimum(relevantLim * (-1) );
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = curChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Add a limit line
//        LimitLine ll = new LimitLine(LIMIT_MAX_MEMORY, "Upper Limit");
//        ll.setLineWidth(2f);
//        ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
//        ll.setTextSize(10f);
//        ll.setTextColor(Color.WHITE);
        // reset all limit lines to avoid overlapping lines
        leftAxis.removeAllLimitLines();
//        leftAxis.addLimitLine(ll);
        // limit lines are drawn behind data (and not on top)
        leftAxis.setDrawLimitLinesBehindData(true);
    }

    private void setupData(LineChart curChart, int relevantColor ) {
        LineData data = new LineData();
        data.setValueTextColor(relevantColor);

        // add empty data
        curChart.setData(data);
    }

    private void setLegend(LineChart curChart) {
        // get the legend (only possible after setting data)
        Legend l = curChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.CIRCLE);
        l.setTextColor(Color.WHITE);
    }

    private LineDataSet createSet(String labelInfo) {
        LineDataSet set = new LineDataSet(null, labelInfo);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColors(ColorTemplate.VORDIPLOM_COLORS[0]);
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);
        // To show values of each point
        set.setDrawValues(true);

        return set;
    }

    private void addEntry(float x, float y, LineChart relevantChart, String labelInfo) {
        LineData data = relevantChart.getData();
        //todo dont label every point
        //todo how often do we get an error from the steering unit
        //todo how many times does the power steering have a fault
        //todo how often does the lane quality detoriate per toyota
        //todo distribution of lane quality, when it is bad, how bad does the curvature get?
        //todo center from the offset
        //todo fix the Y axis

//        Entery text = new Entry();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet(labelInfo);
                data.addDataSet(set);
            }

            data.addEntry(new Entry(x, y), 0);

            // let the chart know it's data has changed
            data.notifyDataChanged();
            relevantChart.notifyDataSetChanged();

            // limit the number of visible entries
            relevantChart.setVisibleXRangeMaximum(75); //todo this needs to be increased 75

            // move to the latest entry
            relevantChart.moveViewToX(data.getXMax()); //data.getEntryCount()
        }
    }

//    @Override
//    public void onResume() {
//        super.onResume();  // Always call the superclass method first
//        mTimer1 = new Runnable() {
//            @Override
//            public void run() {
//                List<ampData> myAmpDataLs =  App.get().getDB().ampDataDAO().findByTimeStampInterval(System.currentTimeMillis()-15*1000, System.currentTimeMillis() );
//
//                for( int i=0; i<10; i++) {
//                    addEntry(i,1);
//                }
//                mHandler.postDelayed(this, 300);
//            }
//        };
//        mHandler.postDelayed(mTimer1, 300);
//
//    }
//    double mLastRandom = 2;
//    Random mRand = new Random();
//    private double getRandom() {
//        return mLastRandom += mRand.nextDouble()*0.5 - 0.25;
//    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        new Thread(new Runnable() {
            @Override
            public void run() {
                App.get().getDB().ampDataDAO().deleteTable();
            }
        }).start();
    }
}
