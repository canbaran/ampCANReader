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
        //500
        private int LLQ;
        private int RLQ;
        private int trim;
        private int LLD;
        private int RLD;
        private int XD;
        private int curve;
        private int speed;
        //501
        private int tangle;
        private int sAngle;
        private int sRate;
        private int tErrorIntegral;
        private int tError;
        private int backOff;

        //503
        private int commandTorque;
        private int userTorque;
        private int totalTorque;

//        private String gps;
//        private byte[] data;
//        private String canID;
//        private String canIDMeaning;





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

        @DynamoDBAttribute(attributeName = "LLQ")
        public int getLLQ() {
            return LLQ;
        }
        public void setLLQ( int LLQ ) {
            this.LLQ = LLQ;
        }
        @DynamoDBAttribute(attributeName = "RLQ")
        public int getRLQ() {
            return RLQ;
        }
        public void setRLQ( int RLQ ) {
            this.RLQ = RLQ;
        }
        @DynamoDBAttribute(attributeName = "trim")
        public int getTrim() {
            return trim;
        }
        public void setTrim( int trim ) {
            this.trim = trim;
        }
        @DynamoDBAttribute(attributeName = "LLD")
        public int getLLD() {
            return LLD;
        }
        public void setLLD( int LLD ) {
            this.LLD = LLD;
        }
        @DynamoDBAttribute(attributeName = "RLD")
        public int getRLD() {
            return RLD;
        }
        public void setRLD( int RLD ) {
            this.RLD = RLD;
        }
        @DynamoDBAttribute(attributeName = "XD")
        public int getXD() {
            return XD;
        }
        public void setXD( int XD ) {
            this.XD = XD;
        }
        @DynamoDBAttribute(attributeName = "curve")
        public int getCurve() {
            return curve;
        }
        public void setCurve( int curve ) {
            this.curve = curve;
        }
        @DynamoDBAttribute(attributeName = "speed")
        public int getSpeed() {
            return speed;
        }
        public void setSpeed( int speed ) {
            this.speed = speed;
        }

        @DynamoDBAttribute(attributeName = "tangle")
        public int getTAngle() {
            return tangle;
        }
        public void setTAngle( int tAngle ) {
            this.tangle = tAngle;
        }
        @DynamoDBAttribute(attributeName = "sAngle")
        public int getSAngle() {
            return sAngle;
        }
        public void setSAngle( int sAngle ) {
            this.sAngle = sAngle;
        }
        @DynamoDBAttribute(attributeName = "sRate")
        public int getSRate() {
            return sRate;
        }
        public void setSRate( int sRate ) {
            this.sRate = sRate;
        }
        @DynamoDBAttribute(attributeName = "tErrorIntegral")
        public int getTErrorIntegral() {
            return tErrorIntegral;
        }
        public void setTErrorIntegral( int tErrorIntegral ) {
            this.tErrorIntegral = tErrorIntegral;
        }
        @DynamoDBAttribute(attributeName = "tError")
        public int getTError() {
            return tError;
        }
        public void setTError( int tError ) {
            this.tError = tError;
        }
        @DynamoDBAttribute(attributeName = "commandTorque")
        public int getCommandTorque() {
            return commandTorque;
        }
        public void setCommandTorque( int commandTorque ) {
            this.commandTorque = commandTorque;
        }
        @DynamoDBAttribute(attributeName = "userTorque")
        public int getUserTorque() {
            return userTorque;
        }
        public void setUserTorque( int userTorque ) {
            this.userTorque = userTorque;
        }
        @DynamoDBAttribute(attributeName = "totalTorque")
        public int getTotalTorque() {
            return totalTorque;
        }
        public void setTotalTorque( int totalTorque ) {
            this.totalTorque = totalTorque;
        }

        //backOff
        @DynamoDBAttribute(attributeName = "backOff")
        public int getBackOff() {
            return backOff;
        }
        public void setBackOff( int backOff ) {
        this.backOff = backOff;
    }








//        @DynamoDBAttribute(attributeName = "gps")
//        public String getGPS() {
//            return gps;
//        }
//
//        public void setGPS(String gps) {
//            this.gps = gps;
//        }

//        @DynamoDBAttribute(attributeName = "data")
//        public byte[] getData() {
//            return data;
//        }
//
//        public void setData(byte[] data) {
//            this.data = data;
//        }

//        @DynamoDBAttribute(attributeName = "canID") //DynamoDBHashKey
//        public String getID() {
//            return canID;
//        }
//
//        public void setCanID(String canID) {
//            this.canID = canID;
//        }

//        @DynamoDBAttribute(attributeName = "canIDMeaning")
//        public String getCanIDMeaning() {
//            return canIDMeaning;
//        }
//
//        public void setCanIDMeaning(String canIDMeaning) {
//            this.canIDMeaning = canIDMeaning;
//        }


}

