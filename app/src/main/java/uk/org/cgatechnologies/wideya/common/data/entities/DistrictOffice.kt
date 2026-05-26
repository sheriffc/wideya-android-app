package uk.org.cgatechnologies.wideya.common.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "district_office", indices = [
    Index(value = ["updated_at"])
])
data class DistrictOffice(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val name: String,
    val district_id: Int? = 0,
    val leader_id: String? = null,
    val lat: Float? = null,
    val lng: Float? = null,
    val display_order: Short? = 0,
    val active: Byte? = null,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String? = null,
)
