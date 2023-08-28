package org.thoughtcrime.securesms.components.webrtc

import android.content.Context
import android.content.DialogInterface
import android.media.AudioDeviceInfo
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpointCompat
import androidx.fragment.app.FragmentActivity
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.service.webrtc.AndroidTelecomUtil
import org.thoughtcrime.securesms.webrtc.audio.AudioDeviceMapping
import org.thoughtcrime.securesms.webrtc.audio.SignalAudioManager

/**
 * This launches the bottom sheet on Android 12+ devices for selecting which audio device to use during a call.
 * In cases where there are fewer than the provided threshold number of devices, it will cycle through them without presenting a bottom sheet.
 */
class WebRtcAudioPickerTelecom(private val audioOutputChangedListener: OnAudioOutputChangedListener, private val outputState: ToggleButtonOutputState, private val stateUpdater: AudioStateUpdater) {

  companion object {
    const val TAG = "WebRtcAudioPickerTelecom"
  }

  @RequiresApi(Build.VERSION_CODES.O)
  fun showPicker(fragmentActivity: FragmentActivity, onDismiss: (DialogInterface) -> Unit): DialogInterface? {
    var connectionSession = AndroidTelecomUtil.getActiveConnection()
    var currentDeviceId = connectionSession.currentCallEndpoint!!.identifier
    var devices = connectionSession.availableEndpoints?.map { it.toTelecomAudioOutputOption(fragmentActivity) }!!
      return TelecomAudioOutputBottomSheet.show(fragmentActivity.supportFragmentManager, devices, currentDeviceId, onAudioDeviceSelected, onDismiss)
  }


  val onAudioDeviceSelected: (TelecomAudioOutputOption) -> Unit = {
    //audioOutputChangedListener.audioOutputChanged(WebRtcAudioDevice(it.toWebRtcAudioOutput(), it.deviceId))
    AndroidTelecomUtil.getActiveConnection().setAudioRoute(it.deviceId)

    when (it.type) {
      CallEndpointCompat.TYPE_WIRED_HEADSET -> {
        outputState.isWiredHeadsetAvailable = true
        stateUpdater.updateAudioOutputState(WebRtcAudioOutput.WIRED_HEADSET)
      }

      CallEndpointCompat.TYPE_EARPIECE -> {
        outputState.isEarpieceAvailable = true
        stateUpdater.updateAudioOutputState(WebRtcAudioOutput.HANDSET)
      }

      CallEndpointCompat.TYPE_BLUETOOTH -> {
        outputState.isBluetoothHeadsetAvailable = true
        stateUpdater.updateAudioOutputState(WebRtcAudioOutput.BLUETOOTH_HEADSET)
      }

      CallEndpointCompat.TYPE_SPEAKER -> stateUpdater.updateAudioOutputState(WebRtcAudioOutput.SPEAKER)
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun CallEndpointCompat.toTelecomAudioOutputOption(context: Context): TelecomAudioOutputOption {
    return when(this.type){
      CallEndpointCompat.TYPE_EARPIECE -> TelecomAudioOutputOption( context.getString(R.string.WebRtcAudioOutputToggle__phone_earpiece), this.type, this.identifier)
      CallEndpointCompat.TYPE_SPEAKER -> TelecomAudioOutputOption(  context.getString(R.string.WebRtcAudioOutputToggle__speaker), this.type, this.identifier)
      CallEndpointCompat.TYPE_WIRED_HEADSET -> TelecomAudioOutputOption(context.getString(R.string.WebRtcAudioOutputToggle__wired_headset_usb), this.type, this.identifier)
      CallEndpointCompat.TYPE_BLUETOOTH -> TelecomAudioOutputOption(this.name.toString(), this.type, this.identifier)
      else -> { TelecomAudioOutputOption(this.name.toString(), this.type, this.identifier)}
    }
  }

  private fun CallEndpointCompat.toAudioDevice(): SignalAudioManager.AudioDevice {
    val device = when (this.type) {
      CallEndpointCompat.TYPE_EARPIECE -> SignalAudioManager.AudioDevice(SignalAudioManager.AudioDeviceType.EARPIECE)
      CallEndpointCompat.TYPE_BLUETOOTH -> SignalAudioManager.AudioDevice(SignalAudioManager.AudioDeviceType.BLUETOOTH)
      CallEndpointCompat.TYPE_WIRED_HEADSET -> SignalAudioManager.AudioDevice(SignalAudioManager.AudioDeviceType.WIRED_HEADSET)
      CallEndpointCompat.TYPE_SPEAKER -> SignalAudioManager.AudioDevice(SignalAudioManager.AudioDeviceType.SPEAKER_PHONE)
      else -> {
        SignalAudioManager.AudioDevice(SignalAudioManager.AudioDeviceType.NONE)
      }
    }

    device.Id = this.identifier

    return device
  }
}
