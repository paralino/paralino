package app.paralino.android.data.models.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_codes")
data class PersonalCodeEntity(
    @PrimaryKey
    val id: String,
    val keyId: String,
    val ownerUserId: String,
    val encryptedCode: String,
    val encryptedCodeData: String,
    val validityUTC: String?,
    val verification: String,
)