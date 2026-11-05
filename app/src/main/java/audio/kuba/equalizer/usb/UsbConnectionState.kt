package audio.kuba.equalizer.usb

import android.hardware.usb.UsbDevice

sealed interface UsbConnectionState {
    data object Disconnected : UsbConnectionState

    data class DeviceDetected(
        val device: UsbDevice
    ) : UsbConnectionState

    data class PermissionDenied(
        val device: UsbDevice
    ) : UsbConnectionState

    data class Connected(
        val device: UsbDevice,
        val productName: String
    ) : UsbConnectionState

    data class Writing(
        val presetName: String,
        val currentPacket: Int,
        val totalPackets: Int
    ) : UsbConnectionState

    data class Success(
        val presetName: String
    ) : UsbConnectionState

    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : UsbConnectionState
}
