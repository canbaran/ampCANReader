package com.github.pires.obd.reader.io;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

/**
 * Created by canbaran on 3/26/18.
 */

@DynamoDBTable(tableName = "gpsData")
public class gpsData {
    private String vin;
    private double latitude;
    private double longitude;
    private long timestamp;


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

    @DynamoDBAttribute(attributeName = "latitude")
    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;

    }
    @DynamoDBAttribute(attributeName = "longitude")
    public double getLongitude() {
        return longitude;
    }
    public void setLongitude (double longitude) {
        this.longitude = longitude;

    }


}
