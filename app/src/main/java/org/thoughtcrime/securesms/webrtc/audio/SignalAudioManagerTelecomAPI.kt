/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.webrtc.audio

import android.content.Context
import android.net.Uri
import org.thoughtcrime.securesms.recipients.RecipientId

class SignalAudioManagerTelecomAPI(context: Context, eventListener: EventListener?) : SignalAudioManager(context, eventListener) {
  override fun initialize() {

  }

  override fun start() {
  }

  override fun stop(playDisconnect: Boolean) {
  }

  override fun setDefaultAudioDevice(recipientId: RecipientId?, newDefaultDevice: AudioDevice, clearUserEarpieceSelection: Boolean) {

  }

  override fun selectAudioDevice(recipientId: RecipientId?, device: Int, isId: Boolean) {

  }

  override fun startIncomingRinger(ringtoneUri: Uri?, vibrate: Boolean) {
  }

  override fun startOutgoingRinger() {

  }
}