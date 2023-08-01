/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.service.webrtc

import android.telecom.DisconnectCause
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies
import org.thoughtcrime.securesms.webrtc.audio.AudioManagerCommand
import org.thoughtcrime.securesms.webrtc.audio.SignalAudioManager

class AndroidCallScope (private var callControlScope: CallControlScope, private var callAttributes: CallAttributesCompat) {

  val disconnectCause: DisconnectCause? = null
  private val coroutineScope = CoroutineScope(Dispatchers.Unconfined)
  var currentCallEndpoint: SignalAudioManager.AudioDevice? = null
  var availableEndpoints : List<CallEndpointCompat>? = null
  private val context = ApplicationDependencies.getApplication()

  init {
    callControlScope.availableEndpoints
      .onEach {
        availableEndpoints = it
        onAudioDeviceChanged()
      }
      .launchIn(coroutineScope)

    callControlScope.currentCallEndpoint
      .onEach {
        currentCallEndpoint = it.toAudioDevice()
        onAudioDeviceChanged()
      }
      .launchIn(coroutineScope)

    callControlScope.isMuted
      .onEach { onMute(it) }
      .launchIn(coroutineScope)
  }

  private fun onMute(isMuted: Boolean) {
    if (isMuted)
      WebRtcCallService.sendAudioManagerCommand(context, AudioManagerCommand.SilenceIncomingRinger())
  }

  fun setDisconnected(disconnectCause: DisconnectCause) {
    Log.e(LOG, "Disconnected")
    coroutineScope.launch {
      if (callControlScope.disconnect(disconnectCause)) {
        WebRtcCallService.hangup(context)
      }
    }
  }

  fun setActive() {
    Log.e(LOG, "Active")
    coroutineScope.launch {
      callControlScope.setActive()
    }
  }

  fun onAnswer() {
    Log.e(LOG, "Answered")
    coroutineScope.launch {
      callControlScope.answer(callAttributes.callType)
    }
  }

  private fun onAudioDeviceChanged() {
    if(availableEndpoints != null) {
      currentCallEndpoint?.let {
          ApplicationDependencies.getSignalCallManager().onAudioDeviceChanged(it, availableEndpoints!!.map { device -> device.toAudioDevice() }.toMutableSet())
        }
    }
  }

  fun setAudioRoute(newRoute: SignalAudioManager.AudioDevice) {
    availableEndpoints?.let{ callEndpoints ->
      coroutineScope.launch {
        var audioDevice = when (newRoute) {
          SignalAudioManager.AudioDevice.EARPIECE -> callEndpoints.find { it.type == CallEndpointCompat.TYPE_EARPIECE }
          SignalAudioManager.AudioDevice.SPEAKER_PHONE -> callEndpoints.find { it.type == CallEndpointCompat.TYPE_SPEAKER }
          SignalAudioManager.AudioDevice.BLUETOOTH -> callEndpoints.find { it.type == CallEndpointCompat.TYPE_BLUETOOTH }
          SignalAudioManager.AudioDevice.WIRED_HEADSET -> callEndpoints.find { it.type == CallEndpointCompat.TYPE_WIRED_HEADSET }
          SignalAudioManager.AudioDevice.NONE -> callEndpoints.find { it.type == CallEndpointCompat.TYPE_SPEAKER }
        }

        audioDevice?.let { callControlScope.requestEndpointChange(it) }
      }
    }
  }


  private fun CallEndpointCompat.toAudioDevice(): SignalAudioManager.AudioDevice {
    return when (this.type) {
      CallEndpointCompat.TYPE_EARPIECE -> SignalAudioManager.AudioDevice.EARPIECE
      CallEndpointCompat.TYPE_BLUETOOTH -> SignalAudioManager.AudioDevice.BLUETOOTH
      CallEndpointCompat.TYPE_WIRED_HEADSET -> SignalAudioManager.AudioDevice.WIRED_HEADSET
      CallEndpointCompat.TYPE_SPEAKER -> SignalAudioManager.AudioDevice.SPEAKER_PHONE
      else -> {
        SignalAudioManager.AudioDevice.NONE
      }
    }
  }

  companion object {
    const val LOG = "TELECOM"
  }
}