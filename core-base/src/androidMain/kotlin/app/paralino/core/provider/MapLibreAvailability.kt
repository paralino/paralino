package app.paralino.core.provider

import android.os.Process

actual object MapLibreAvailability {
    actual val canUseMapLibre: Boolean = Process.is64Bit()
}
