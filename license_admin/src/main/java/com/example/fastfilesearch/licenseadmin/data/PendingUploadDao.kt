package com.example.fastfilesearch.licenseadmin.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingUploadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingUploadEntity): Long

    @Query("SELECT * FROM pending_uploads ORDER BY localId ASC")
    suspend fun getAllPending(): List<PendingUploadEntity>

    @Query("SELECT COUNT(*) FROM pending_uploads")
    suspend fun countPending(): Int

    @Query("DELETE FROM pending_uploads WHERE localId = :localId")
    suspend fun deleteById(localId: Long)

    @Query("UPDATE pending_uploads SET attempts = attempts + 1 WHERE localId = :localId")
    suspend fun incrementAttempts(localId: Long)
}
