package com.github.pires.obd.reader.activity;

import android.arch.persistence.room.Room;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;


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
import static java.lang.Math.round;

/**
 * Created by canbaran on 2/14/18.
 */

public class VisualsActivity extends AppCompatActivity {
    private LineChart mChartXd;
    private LineChart mChartCenterOffset;
    private LineChart mChartCurvature;
    private ArrayList<LineChart> chartArr = new ArrayList<LineChart>();
//    private long upperTimeStamp = 0;
//    private long lowerTimeStamp = 0;

    private Runnable mTimer1;
    private final Handler mHandler = new Handler();
    private long createSystemTime;
    private ArrayList<Float> timestampArr = new ArrayList<>();
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

                queryDBPlotData();

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
                if (curRLD<42) {
                    LLDTarget = (curLLD + curRLD) / 2;
                } else {
                    LLDTarget = (curLLD + 42)/2;
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
        } else if( (curLLD==255)&&(curRLD==255) ) {
            centerOffset = -20;
        }
        else {
            centerOffset = 20;
        }
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
//        leftAxis.setDrawLimitLinesBehindData(true);
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

//        ColorTemplate.VORDIPLOM_COLORS

        int curColor = 0;
        switch (labelInfo) {
            case "Xd":
                curColor = Color.WHITE;
                break;
            case "CenterOffset":
                curColor = Color.RED;
                break;
            case "Curvature":
                curColor = Color.BLUE;
                break;
            default:
                curColor = Color.WHITE;
                break;
        }
        set.setColors(curColor); //ColorTemplate.VORDIPLOM_COLORS[0]
        set.setCircleColor(curColor);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
//        set.setValueTextColor(curColor);
//        set.setValueTextSize(10f);
        // To show values of each point
        set.setDrawValues(false);

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

            Entry myCurrentEntry = new Entry( x, y); //should be x on the x place

            if( !set.contains(  myCurrentEntry ) ) {
                Log.d("visuals", "Cur Timestamp: " + Float.toString(x));


                data.addEntry(myCurrentEntry, 0);
                Log.d("visuals", "just added Entry");
                Log.d("visuals", "Max X entry:" + Float.toString(data.getXMax()));

                // let the chart know it's data has changed
                data.notifyDataChanged();
                relevantChart.notifyDataSetChanged();

                // limit the number of visible entries
                relevantChart.setVisibleXRangeMaximum(15);

//                relevantChart.setVisibleXRange((float) (x-2.5*1000), (float) (x+2.5*1000));
                Log.d("visuals", "current x:" + Float.toString(x));
                Log.d("visuals", "Xaxis Range: " + Float.toString(relevantChart.getXAxis().mAxisRange));

                // move to the latest entry
                relevantChart.moveViewToX(data.getXMax()); //data.getEntryCount()
            } else {
                Log.d("visuals", "data already in the set");
            }

        }
    }

    private void queryDBPlotData() {
        Log.d("visuals", "entering the thread");
        List<ampData> temp = App.get().getDB().ampDataDAO().findByFirstTimestamp();
        if (temp.size() > 0) {
            firstTimeStamp = temp.get(0).getTimestamp();

            Log.d("visuals", "first timestamp: " + Long.toString(firstTimeStamp));
            //get the highest X point on the chart
//            float highestX = getHighestXPoint();
//            Log.d("visuals", "highest X Point: " + Float.toString(highestX));
            long previousTimeStamp = firstTimeStamp;
//            Log.d("visuals", "Lower TimeStamp:" + Long.toString(firstTimeStamp+ (long) highestX));
//            Log.d("visuals", "Upper TimeStamp:" + Long.toString(System.currentTimeMillis()));

            List<ampData> myAmpDataLs = App.get().getDB().ampDataDAO().findByTimeStampInterval(System.currentTimeMillis()-2*1000, System.currentTimeMillis()); //firstTimeStamp+ (long) highestX
//            long arrayFirstTS = myAmpDataLs.get(0).getTimestamp();
//            long arrayLastTS = myAmpDataLs.get(myAmpDataLs.size()-1).getTimestamp();

//            Log.d("Visuals", "Array First TimeStamp: " + Long.toString(arrayFirstTS));
//            Log.d("Visuals", "Array Last TimeStamp: " + Long.toString(arrayLastTS));


            Log.d("visuals", "Size of the Array received from DB: " + Integer.toString(myAmpDataLs.size()));
            for (int i = 0; i < myAmpDataLs.size();  i++) { //
                Long curX = myAmpDataLs.get(i).getTimestamp(); //- firstTimeStamp ) / 1000;

                if (curX - previousTimeStamp > 100) {
//                    long x = (curX - firstTimeStamp) / 1000;
                    float x =  (float) (myAmpDataLs.get(i).getTimestamp() - firstTimeStamp )/1000;
                    final int curXd = myAmpDataLs.get(i).getXD();
                    final int curCurvature = myAmpDataLs.get(i).getCurve();
                    final int curCenterOffset = calculateOffset(myAmpDataLs.get(i));


    //                Log.d("visuals", "Current TimeStamp: " + Long.toString(x));
    //
    //                    Log.d("visuals", "in for loop:x= " + Long.toString(x) + " y= " + Float.toString(curXd));
//                        if (!timestampArr.contains(x)) {

                    final float x2 = x;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addEntry(x2, curXd, mChartXd, "Xd");
                            addEntry(x2, curCenterOffset, mChartCenterOffset, "CenterOffset");
                            addEntry(x2, curCurvature, mChartCurvature, "Curvature");
                        }
                    });
//                            timestampArr.add(x);
//                        }
                    previousTimeStamp = curX;
                }
                ampData curAmpData = myAmpDataLs.get(i);
                curAmpData.setIsPlotted(true);
                App.get().getDB().ampDataDAO().update(curAmpData);
            }

        }

    }

    private float getHighestXPoint() {
        LineData data = mChartXd.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                return 0;
            } else {
                return set.getXMax()*1000;
            }
        } else return 0;


    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        mTimer1 = new Runnable() {
            @Override
            public void run() {

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        queryDBPlotData();

                    }
                }).start();

                mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.postDelayed(mTimer1, 1000);

    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(mTimer1);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("visuals", "destroy is entered");
        timestampArr.clear();

    }
}
