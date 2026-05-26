package uk.org.cgatechnologies.wideya.common.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "person_contact", indices = [
    Index(value = ["person_uuid"]),
    Index(value = ["updated_by"]),
    Index(value = ["sync_flag"]),
    Index(value = ["active"]),
])
data class PersonContact(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val person_uuid: String,
    val contact_type_oid: String,
    val contact_detail: String,
    val active: Byte = 1,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Byte = 0
)
