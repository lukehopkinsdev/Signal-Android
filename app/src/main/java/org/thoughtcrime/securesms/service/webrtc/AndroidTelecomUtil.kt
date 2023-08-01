package org.thoughtcrime.securesms.service.webrtc

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.telecom.DisconnectCause.REJECTED
import android.telecom.DisconnectCause.UNKNOWN
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlCallback
import androidx.core.telecom.CallsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.webrtc.CallNotificationBuilder


/**
 * Wrapper around various [TelecomManager] methods to make dealing with SDK versions easier. Also
 * maintains a global list of all Signal [AndroidCallScope]s associated with their [RecipientId].
 * There should really only be one ever, but there may be times when dealing with glare or a busy that two
 * may kick off.
 */
@SuppressLint("NewApi", "InlinedApi")
object AndroidTelecomUtil {

  private lateinit var activeId: RecipientId
  private val TAG = Log.tag(AndroidTelecomUtil::class.java)
  private val context = ApplicationDependencies.getApplication()
  private val callsManager = CallsManager(context)
  private val coroutineScope = CoroutineScope(Dispatchers.Unconfined)
  private var accountRegistered = false
  const val ALL_CALL_CAPABILITIES = (CallAttributesCompat.SUPPORTS_SET_INACTIVE
    or CallAttributesCompat.SUPPORTS_STREAM or CallAttributesCompat.SUPPORTS_TRANSFER)

  @JvmStatic
  val telecomSupported: Boolean
    get() {
      if (isTelecomAllowedForDevice()) {
        if (!accountRegistered) {
          registerPhoneAccount()
          accountRegistered = true
        }
        return true
      }
      return false
    }

  @JvmStatic
  val connections: MutableMap<RecipientId, AndroidCallScope> = mutableMapOf()

  @JvmStatic
  fun registerPhoneAccount() {
      var capabilities: @CallsManager.Companion.Capability Int =
        CallsManager.CAPABILITY_BASELINE or CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING or CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING
      callsManager.registerAppWithTelecom(capabilities)
  }


  @JvmStatic
  fun reject(recipientId: RecipientId) {
    if (telecomSupported) {
      connections[recipientId]?.setDisconnected(DisconnectCause(REJECTED))
    }
  }

  @JvmStatic
  fun activateCall(recipientId: RecipientId) {
    if (telecomSupported) {
      activeId = recipientId
      connections[recipientId]?.setActive()
    }
  }

  @JvmStatic
  fun acceptCall(recipientId: RecipientId) {
    if (telecomSupported) {
      connections[recipientId]?.let { connection ->
        connection.onAnswer()
      }
    }
  }

  @JvmStatic
  fun terminateCall(recipientId: RecipientId) {
    if (telecomSupported) {
      connections[recipientId]?.let { connection ->
        if (connection.disconnectCause == null) {
          connection.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        }
        connections.remove(recipientId)
      }
    }
  }

  @JvmStatic
  fun addIncomingCall(recipientId: RecipientId, callId: Long, remoteVideoOffer: Boolean): Boolean {
    if (telecomSupported) {
      val recipient = Recipient.resolved(recipientId)
      val displayName = recipient.getDisplayName(context)

      if (SignalStore.settings().messageNotificationsPrivacy.isDisplayContact && recipient.e164.isPresent) {
        val uriAddress = Uri.fromParts("tel", recipient.e164.get(), null)
        val OUTGOING_CALL_ATTRIBUTES =
          CallAttributesCompat(
            displayName,
            uriAddress,
            CallAttributesCompat.DIRECTION_INCOMING,
            if (remoteVideoOffer) CallAttributesCompat.CALL_TYPE_VIDEO_CALL else CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
            ALL_CALL_CAPABILITIES,
          )

        makeCall(recipientId,callId, OUTGOING_CALL_ATTRIBUTES)
        WebRtcCallService.update(context, CallNotificationBuilder.TYPE_INCOMING_CONNECTING, recipientId, remoteVideoOffer)
      }

    }
    return true
  }

  @JvmStatic
  fun addOutgoingCall(recipientId: RecipientId, callId: Long, isVideoCall: Boolean): Boolean {
    if (telecomSupported) {
      val recipient = Recipient.resolved(recipientId)
      val displayName = recipient.getDisplayName(context)

      val OUTGOING_CALL_ATTRIBUTES =
        CallAttributesCompat(
          displayName,
          recipientId.generateTelecomE164(),
          CallAttributesCompat.DIRECTION_INCOMING,
          if (isVideoCall) CallAttributesCompat.CALL_TYPE_VIDEO_CALL else CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
          ALL_CALL_CAPABILITIES,
        )

      makeCall(recipientId,callId, OUTGOING_CALL_ATTRIBUTES)
    }
    return true
  }

  private fun makeCall(recipientId: RecipientId,callId: Long, callAttributes: CallAttributesCompat) {
    coroutineScope.launch {
      callsManager.addCall(callAttributes) {
        connections[recipientId] = AndroidCallScope(this, callAttributes)
        ApplicationDependencies.getSignalCallManager().setTelecomApproved(callId, recipientId)
        setCallback(callControlCallback)
      }
    }
  }


  private fun isTelecomAllowedForDevice(): Boolean {
    val pm = context.packageManager
    return Build.VERSION.SDK_INT >= 26 && pm.hasSystemFeature(PackageManager.FEATURE_TELECOM)
  }

  fun getActiveConnection(): AndroidCallScope {
    return connections[activeId]!!
  }
}

@RequiresApi(26)
private fun Connection.setAudioRouteIfDifferent(newRoute: Int) {
  if (callAudioState.route != newRoute) {
    setAudioRoute(newRoute)
  }
}

private fun RecipientId.generateTelecomE164(): Uri {
  val pseudoNumber = toLong().toString().padEnd(10, '0').replaceRange(3..5, "555")
  return Uri.fromParts("tel", "+1$pseudoNumber", null)
}


private val callControlCallback = object : CallControlCallback {
  override suspend fun onSetActive(): Boolean {

    return true
  }

  override suspend fun onSetInactive(): Boolean {

    return true
  }

  override suspend fun onAnswer(callType: Int): Boolean {

    return true
  }

  override suspend fun onDisconnect(disconnectCause: DisconnectCause): Boolean {
    return true
  }
}

