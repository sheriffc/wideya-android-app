package uk.org.cgatechnologies.wideya.common.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Entity(tableName = "option_list", indices = [
    Index(value = ["updated_at"]),
    Index(value = ["list_name", "item_id", "active"]),
])
data class OptionList(
    @PrimaryKey(autoGenerate = false) val id: Int,
    val parent_id: Int?,
    val list_name: String,
    val item_name: String? = null,
    val item_id: String? = null,
    val item_extra: String? = null,
    val item_asc_fabinc_recordid: String? = null,
    val display_order: Short?,
    val active: Byte = 1,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String? = null,
) : Parcelable
