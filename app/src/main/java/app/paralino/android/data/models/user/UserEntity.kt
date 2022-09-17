package app.paralino.android.data.models.user

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.paralino.core.models.raw.UserMeDataRaw

@Entity(tableName = "user_me")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String = "",
    val registration: String = "",
    val emailVerification: Boolean = false,
    @Embedded(prefix = "user_data")
    val data: UserMeDataRaw?
) {
}