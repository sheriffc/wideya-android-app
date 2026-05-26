package uk.org.cgatechnologies.wideya.common.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Entity(tableName = "option_list_link", indices = [
    Index(value = ["updated_at"]),
    Index(value = ["parent_list_name", "child_list_name", "deleted_at"]),
])
data class OptionListLink(
    @PrimaryKey(autoGenerate = false) val id: Int,
    val parent_list_name: String,
    val child_list_name: String,
    val parent_id: Int,
    val child_id: Int,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String? = null,
) : Parcelable
