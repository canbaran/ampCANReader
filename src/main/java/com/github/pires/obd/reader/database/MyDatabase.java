package com.github.pires.obd.reader.database;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.util.Log;

import com.github.pires.obd.reader.database.entity.ampData;

import java.io.Serializable;


/**
 * Created by canbaran on 2/15/18.
 */

@Database(entities = {ampData.class}, version = 2)
//@TypeConverters({DateTypeConverter.class})
public abstract class MyDatabase extends RoomDatabase  {
    public abstract ampDataDAO ampDataDAO();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d("migration", "do nothing");
//            database.execSQL("ALTER TABLE ampData "
//                    + " ADD COLUMN price INTEGER");
//
//             enable flag to force update products
//            App.get().setForceUpdate(true);
        }
    };
}