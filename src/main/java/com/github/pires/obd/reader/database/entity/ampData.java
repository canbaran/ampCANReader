package com.github.pires.obd.reader.database.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.io.Serializable;


/**
 * Created by canbaran on 2/15/18.
 */
@Entity(tableName = "ampData")
public class ampData {
    @PrimaryKey(autoGenerate = false)
    private long timestamp;
    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp( long timestamp ) {
        this.timestamp = timestamp;
    }

    @ColumnInfo(name = "LLQ")
    private int LLQ;
    public int getLLQ() {
        return LLQ;
    }
    public void setLLQ( int LLQ ) {
        this.LLQ = LLQ;
    }

    @ColumnInfo(name = "RLQ")
    private int RLQ;
    public int getRLQ() {
        return RLQ;
    }
    public void setRLQ( int RLQ ) {
        this.RLQ = RLQ;
    }

    @ColumnInfo(name = "trim")
    private int trim;
    public int getTrim() {
        return trim;
    }
    public void setTrim( int trim ) {
        this.trim = trim;
    }

    @ColumnInfo(name = "LLD")
    private int LLD;
    public int getLLD() {
        return LLD;
    }
    public void setLLD( int LLD ) {
        this.LLD = LLD;
    }

    @ColumnInfo(name = "RLD")
    private int RLD;
    public int getRLD() {
        return RLD;
    }
    public void setRLD( int RLD ) {
        this.RLD = RLD;
    }

    @ColumnInfo(name = "XD")
    private int XD;
    public int getXD() {
        return XD;
    }
    public void setXD( int XD ) {
        this.XD = XD;
    }

    @ColumnInfo(name = "curve")
    private int curve;
    public int getCurve() {
        return curve;
    }
    public void setCurve( int curve ) {
        this.curve = curve;
    }

    @ColumnInfo(name = "speed")
    private int speed;
    public int getSpeed() {
        return speed;
    }
    public void setSpeed( int speed ) {
        this.speed = speed;
    }

//    @ColumnInfo(name = "tangle")
//    private int tangle;
//    public int getTAngle() {
//        return tangle;
//    }
//    public void setTAngle( int tAngle ) {
//        this.tangle = tAngle;
//    }

    @ColumnInfo(name = "sAngle")
    private int sAngle;
    public int getSAngle() {
        return sAngle;
    }
    public void setSAngle( int sAngle ) {
        this.sAngle = sAngle;
    }

    @ColumnInfo(name = "sRate")
    private int sRate;
    public int getSRate() {
        return sRate;
    }
    public void setSRate( int sRate ) {
        this.sRate = sRate;
    }

    @ColumnInfo(name = "tErrorIntegral")
    private int tErrorIntegral;
    public int getTErrorIntegral() {
        return tErrorIntegral;
    }
    public void setTErrorIntegral( int tErrorIntegral ) {
        this.tErrorIntegral = tErrorIntegral;
    }

    @ColumnInfo(name = "tError")
    private int tError;
    public int getTError() {
        return tError;
    }
    public void setTError( int tError ) {
        this.tError = tError;
    }

    @ColumnInfo(name = "commandTorque")
    private int commandTorque;
    public int getCommandTorque() {
        return commandTorque;
    }
    public void setCommandTorque( int commandTorque ) {
        this.commandTorque = commandTorque;
    }


    @ColumnInfo(name = "userTorque")
    private int userTorque;
    public int getUserTorque() {
        return userTorque;
    }
    public void setUserTorque( int userTorque ) {
        this.userTorque = userTorque;
    }

    @ColumnInfo(name = "totalTorque")
    private int totalTorque;
    public int getTotalTorque() {
        return totalTorque;
    }
    public void setTotalTorque( int totalTorque ) {
        this.totalTorque = totalTorque;
    }



}
