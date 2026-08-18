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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Entity(tableName = "diagnostic_sessions")
data class DiagnosticSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val startTime: Long,
    val endTime: Long,
    val modeProvenance: String
)

@Entity(tableName = "telemetry_history")
data class TelemetryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val rpm: Int?,
    val speedKmh: Int?,
    val coolantTempC: Int?,
    val batteryVoltage: Float?
)

@Entity(tableName = "raw_communication_logs")
data class RawCommunicationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val direction: String,
    val rawHexOrText: String,
    val protocolId: String
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
interface DiagnosticSessionDao {
    @Insert
    suspend fun insertSession(session: DiagnosticSessionEntity): Long
    
    @Insert
    suspend fun insertTelemetry(telemetry: List<TelemetryHistoryEntity>)
    
    @Insert
    suspend fun insertLogs(logs: List<RawCommunicationLogEntity>)
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
        DiagnosticSessionEntity::class,
        TelemetryHistoryEntity::class,
        RawCommunicationLogEntity::class,
        MaintenanceLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ObdDatabase : RoomDatabase() {
    abstract fun vehicleProfileDao(): VehicleProfileDao
    abstract fun dtcScanDao(): DtcScanDao
    abstract fun diagnosticSessionDao(): DiagnosticSessionDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao

    companion object {
        @Volatile
        private var INSTANCE: ObdDatabase? = null

        fun getDatabase(context: Context): ObdDatabase {
            val MIGRATION_1_2 = object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `diagnostic_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `modeProvenance` TEXT NOT NULL)")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `telemetry_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `rpm` INTEGER, `speedKmh` INTEGER, `coolantTempC` INTEGER, `batteryVoltage` REAL)")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `raw_communication_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `direction` TEXT NOT NULL, `rawHexOrText` TEXT NOT NULL, `protocolId` TEXT NOT NULL)")
                }
            }
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ObdDatabase::class.java,
                    "thai_car_obd_db"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
