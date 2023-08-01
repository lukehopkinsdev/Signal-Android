package org.thoughtcrime.securesms.components.webrtc

import android.content.Context
import android.content.DialogInterface
import android.media.AudioDeviceInfo
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

  fun showPicker(fragmentActivity: FragmentActivity, onDismiss: (DialogInterface) -> Unit): DialogInterface? {
    var connectionSession = AndroidTelecomUtil.getActiveConnection()
    var currentDeviceId = connectionSession.currentCallEndpoint!!.ordinal

    var devices = connectionSession.availableEndpoints?.map {it.toAudioOutputOption(fragmentActivity) }!!.distinct().filterNot { it.deviceType == SignalAudioManager.AudioDevice.NONE }
      return WebRtcAudioOutputBottomSheet.show(fragmentActivity.supportFragmentManager, devices, currentDeviceId, onAudioDeviceSelected, onDismiss)
  }

  val onAudioDeviceSelected: (AudioOutputOption) -> Unit = {
   // audioOutputChangedListener.audioOutputChanged(WebRtcAudioDevice(it.toWebRtcAudioOutput(), it.deviceId))
    AndroidTelecomUtil.getActiveConnection().setAudioRoute(it.deviceType)

    when (it.deviceType) {
      SignalAudioManager.AudioDevice.WIRED_HEADSET -> {
        outputState.isWiredHeadsetAvailable = true
        stateUpdater.updateAudioOutputState(WebRtcAudioOutput.WIRED_HEADSET)
      }

      SignalAudioManager.AudioDevice.EARPIECE -> {
        outputState.isEarpieceAvailable = true
        stateUpdater.updateAudioOutputState(WebRtcAudioOutput.HANDSET)
      }

      SignalAudioManager.AudioDevice.BLUETOOTH -> {
        outputState.isBluetoothHeadsetAvailable = true
        stateUpdater.updateAudioOutputState(WebRtcAudioOutput.BLUETOOTH_HEADSET)
      }

      SignalAudioManager.AudioDevice.SPEAKER_PHONE, SignalAudioManager.AudioDevice.NONE -> stateUpdater.updateAudioOutputState(WebRtcAudioOutput.SPEAKER)
    }
  }

  private fun CallEndpointCompat.toAudioOutputOption(context: Context): AudioOutputOption {
    return when(this.type){
      CallEndpointCompat.TYPE_EARPIECE -> AudioOutputOption( context.getString(R.string.WebRtcAudioOutputToggle__phone_earpiece), SignalAudioManager.AudioDevice.EARPIECE, SignalAudioManager.AudioDevice.EARPIECE.ordinal)
      CallEndpointCompat.TYPE_SPEAKER -> AudioOutputOption(  context.getString(R.string.WebRtcAudioOutputToggle__speaker), SignalAudioManager.AudioDevice.SPEAKER_PHONE, SignalAudioManager.AudioDevice.SPEAKER_PHONE.ordinal)
      CallEndpointCompat.TYPE_WIRED_HEADSET -> AudioOutputOption(context.getString(R.string.WebRtcAudioOutputToggle__wired_headset_usb), SignalAudioManager.AudioDevice.WIRED_HEADSET, SignalAudioManager.AudioDevice.WIRED_HEADSET.ordinal)
      CallEndpointCompat.TYPE_BLUETOOTH -> AudioOutputOption(this.name.toString(), SignalAudioManager.AudioDevice.BLUETOOTH, SignalAudioManager.AudioDevice.BLUETOOTH.ordinal)
      else -> { AudioOutputOption(this.name.toString(), SignalAudioManager.AudioDevice.SPEAKER_PHONE, SignalAudioManager.AudioDevice.SPEAKER_PHONE.ordinal)}
    }
  }
}
