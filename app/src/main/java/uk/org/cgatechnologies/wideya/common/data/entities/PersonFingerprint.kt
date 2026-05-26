package uk.org.cgatechnologies.wideya.common.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "person_fingerprint", indices = [
    Index(value = ["person_uuid"]),
    Index(value = ["sync_flag"]),
    Index(value = ["updated_at"]),
])
data class PersonFingerprint(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val person_uuid: String,
    val finger_position_oid: String,
    val fp_a_cbor: String, //base64
    val fp_a_nfiq: Int,
    val fp_b_cbor: String, //base64
    val fp_b_nfiq: Int,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Byte = 0
) {
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as PersonFingerprint
//
//        if (!fp_a_cbor.contentEquals(other.fp_a_cbor)) return false
//        if (!fp_b_cbor.contentEquals(other.fp_b_cbor)) return false
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = fp_a_cbor.contentHashCode()
//        result = 31 * result + fp_b_cbor.contentHashCode()
//        return result
//    }
}
