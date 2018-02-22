package com.github.pires.obd.reader.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.github.pires.obd.reader.database.entity.ampData;

import java.util.List;

/**
 * Created by canbaran on 2/15/18.
 */
@Dao
public interface ampDataDAO {
    @Query("SELECT * FROM ampData")
    List<ampData> getAll();

    @Query("SELECT * FROM ampData order by timestamp asc limit 1")
    List<ampData> findByFirstTimestamp();

    @Query("SELECT * FROM ampData WHERE timestamp >= :t1 and timestamp <= :t2 order by timestamp asc")
    List<ampData> findByTimeStampInterval(Long t1, Long t2);

    @Query("DELETE FROM ampData")
    int deleteTable();

    @Insert
    void insertAll(ampData... products);

    @Update
    void update(ampData product);

    @Delete
    void delete(ampData product);

}
