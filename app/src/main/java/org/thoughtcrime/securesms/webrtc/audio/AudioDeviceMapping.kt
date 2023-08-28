package org.thoughtcrime.securesms.webrtc.audio

import android.media.AudioDeviceInfo
import androidx.annotation.RequiresApi

@RequiresApi(31)
object AudioDeviceMapping {

  private val systemDeviceTypeMap: Map<SignalAudioManager.AudioDeviceType, List<Int>> = mapOf(
    SignalAudioManager.AudioDeviceType.BLUETOOTH to listOf(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_HEARING_AID),
    SignalAudioManager.AudioDeviceType.EARPIECE to listOf(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE),
    SignalAudioManager.AudioDeviceType.SPEAKER_PHONE to listOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE),
    SignalAudioManager.AudioDeviceType.WIRED_HEADSET to listOf(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET),
    SignalAudioManager.AudioDeviceType.NONE to emptyList()
  )

  @JvmStatic
  fun getEquivalentPlatformTypes(audioDevice: SignalAudioManager.AudioDeviceType): List<Int> {
    return systemDeviceTypeMap[audioDevice]!!
  }

  @JvmStatic
  fun fromPlatformType(type: Int): SignalAudioManager.AudioDeviceType {
    for (kind in SignalAudioManager.AudioDeviceType.values()) {
      if (getEquivalentPlatformTypes(kind).contains(type)) return kind
    }
    return SignalAudioManager.AudioDeviceType.NONE
  }
}
