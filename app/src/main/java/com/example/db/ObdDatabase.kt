package com.example.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vehicle_profiles")
data class VehicleProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val make: String,
    val model: String,
    val year: Int,
    val engineType: String,
    val licensePlate: String,
    val odometerKm: Int,
    val isDefault: Boolean = false
)

@Entity(tableName = "dtc_scan_records")
data class DtcScanRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val totalCodesFound: Int,
    val codesJson: String,
    val modeProvenance: String,
    val notes: String = ""
)

@Entity(tableName = "maintenance_logs")
data class MaintenanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val titleTh: String,
    val dateTimestamp: Long,
    val costBaht: Double,
    val mileageKm: Int,
    val category: String,
    val notes: String = ""
)

@Dao
interface VehicleProfileDao {
    @Query("SELECT * FROM vehicle_profiles ORDER BY isDefault DESC, id DESC")
    fun getAllProfiles(): Flow<List<VehicleProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VehicleProfileEntity): Long

    @Query("DELETE FROM vehicle_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)
}

@Dao
interface DtcScanDao {
    @Query("SELECT * FROM dtc_scan_records WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    fun getScansForVehicle(vehicleId: Long): Flow<List<DtcScanRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanRecord(record: DtcScanRecordEntity): Long
}

@Dao
interface MaintenanceLogDao {
    @Query("SELECT * FROM maintenance_logs WHERE vehicleId = :vehicleId ORDER BY dateTimestamp DESC")
    fun getLogsForVehicle(vehicleId: Long): Flow<List<MaintenanceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MaintenanceLogEntity): Long
}

@Database(
    entities = [
        VehicleProfileEntity::class,
        DtcScanRecordEntity::class,
        MaintenanceLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ObdDatabase : RoomDatabase() {
    abstract fun vehicleProfileDao(): VehicleProfileDao
    abstract fun dtcScanDao(): DtcScanDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao

    companion object {
        @Volatile
        private var INSTANCE: ObdDatabase? = null

        fun getDatabase(context: Context): ObdDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ObdDatabase::class.java,
                    "thai_car_obd_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
