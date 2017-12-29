package com.github.pires.obd.reader.io;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

/**
 * Created by canbaran on 12/23/17.
 */
@DynamoDBTable(tableName = "canData")
public class can_data {
        private String vin;
        private long timestamp;
        private String gps;
        private String data;
        private String canID;
        private String canIDMeaning;


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

