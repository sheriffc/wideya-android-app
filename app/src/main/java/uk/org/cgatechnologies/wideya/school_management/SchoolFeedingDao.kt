package uk.org.cgatechnologies.wideya.school_management

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolFeeding

@Dao
abstract class SchoolFeedingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(record: SchoolFeeding)

    @Update
    abstract suspend fun update(record: SchoolFeeding)

    @Query("""
        SELECT * FROM school_feeding
        WHERE school_uuid = :schoolUuid
          AND (deleted_at IS NULL OR deleted_at = '')
        ORDER BY updated_at DESC
        LIMIT 1
    """)
    abstract fun getLatestBySchool(schoolUuid: String): SchoolFeeding?
}
