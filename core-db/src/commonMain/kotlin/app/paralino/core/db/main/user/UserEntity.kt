package app.paralino.core.db.main.user

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "userEntity")
data class UserEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "registration") val registration: String,
    @ColumnInfo(name = "emailVerification") val emailVerification: Boolean,
    @ColumnInfo(name = "passwordUpdate") val passwordUpdate: String?,
    @ColumnInfo(name = "user_data_userImage") val userImage: String?,
    @ColumnInfo(name = "user_data_userName") val userName: String?,
    @ColumnInfo(name = "user_data_userKeyVersion") val userKeyVersion: Long?
)