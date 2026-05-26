package uk.org.cgatechnologies.wideya.common.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "geo", indices = [
    Index(value = ["parent_id"]) ,
    Index(value = ["updated_at"])
])
data class Geo(
    @PrimaryKey(autoGenerate = false) val id: Int,
    val parent_id: Int? = null,
    val name: String,
    val type: Int,
    val fabinc_recordid: Int? = null,
    val display_order: Short?,
    val active: Byte = 1,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String? = null,
)
