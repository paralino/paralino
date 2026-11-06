package app.paralino.core.db.main.keys

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "userKeysEntity")
data class UserKeysEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long?,
    @ColumnInfo(name = "encryptedSymmetricKey") val encryptedSymmetricKey: String,
    @ColumnInfo(name = "encryptedPrivateKey") val encryptedPrivateKey: String,
    @ColumnInfo(name = "encryptedPublicKey") val encryptedPublicKey: String,
    @ColumnInfo(name = "publicKey") val publicKey: String,
    @ColumnInfo(name = "oldMasterKeys") val oldMasterKeys: String?,
    @ColumnInfo(name = "loginSalt") val loginSalt: String,
    @ColumnInfo(name = "loginAlg") val loginAlg: String,
    @ColumnInfo(name = "dataAlg") val dataAlg: String,
    @ColumnInfo(name = "dataSalt") val dataSalt: String?,
    @ColumnInfo(name = "schemeVersion") val schemeVersion: String?,
    @ColumnInfo(name = "isActive") val isActive: Boolean
)
