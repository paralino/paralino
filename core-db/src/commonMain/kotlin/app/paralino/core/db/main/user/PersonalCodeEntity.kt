package app.paralino.core.db.main.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personalCodeEntity")
data class PersonalCodeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "keyId") val keyId: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "encryptedCode") val encryptedCode: String,
    @ColumnInfo(name = "encryptedCodeUserData") val encryptedCodeUserData: String?,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long?
)
