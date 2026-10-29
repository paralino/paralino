package app.paralino.core.domain.privacyzone

import app.paralino.core.resources.moko.*
import app.paralino.core.models.privacyzones.GroupPrivacyZone
import app.paralino.core.provider.localizedString

val GroupPrivacyZone.nameOrDefault: String get() {
    return this.nameOrNull?.ifBlank { MR.strings.privacy_zones_create_name_default.localizedString() } ?: "---"
}