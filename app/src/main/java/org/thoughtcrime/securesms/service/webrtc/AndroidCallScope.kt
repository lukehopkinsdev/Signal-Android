/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.service.webrtc

import android.telecom.DisconnectCause
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies
import org.thoughtcrime.securesms.webrtc.audio.AudioManagerCommand

class AndroidCallScope (private var callControlScope: CallControlScope, private var callAttributes: CallAttributesCompat) {

  val disconnectCause: DisconnectCause? = null
  private val coroutineScope = CoroutineScope(Dispatchers.Unconfined)
  private val context = ApplicationDependencies.getApplication()

  init {
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

  companion object {
    const val LOG = "TELECOM"
  }
}