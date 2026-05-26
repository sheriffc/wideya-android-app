package uk.org.cgatechnologies.wideya.teacher_management.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "teacher_payroll", indices = [
    Index(value = ["pin"]),
    Index(value = ["updated_at"]),
])
data class TeacherPayroll(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val school_sid: Int? = null,
    val school_emis_id: String? = null,
    val first_name: String,
    val middle_name: String? = null,
    val last_name: String,
    val sex: String? = null,
    val date_of_birth: String? = null,
    val pin: String? = null,
    val nin: String? = null,
    val nassit_number: String? = null,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String? = null,
)
