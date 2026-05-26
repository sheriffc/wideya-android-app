package uk.org.cgatechnologies.wideya.school_management

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolFeedingStock

@Dao
abstract class SchoolFeedingStockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(record: SchoolFeedingStock)

    @Update
    abstract suspend fun update(record: SchoolFeedingStock)

    @Query("""
        SELECT * FROM school_feeding_stock
        WHERE school_uuid = :schoolUuid
          AND stock_month = :month
          AND (deleted_at IS NULL OR deleted_at = '')
        LIMIT 1
    """)
    abstract fun getBySchoolAndMonth(schoolUuid: String, month: String): SchoolFeedingStock?
}
