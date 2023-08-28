package org.thoughtcrime.securesms.components.webrtc

import android.content.DialogInterface
import android.os.ParcelUuid
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallEndpointCompat.Companion.TYPE_BLUETOOTH
import androidx.core.telecom.CallEndpointCompat.Companion.TYPE_EARPIECE
import androidx.core.telecom.CallEndpointCompat.Companion.TYPE_SPEAKER
import androidx.core.telecom.CallEndpointCompat.Companion.TYPE_WIRED_HEADSET
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.signal.core.ui.BottomSheets
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.compose.ComposeBottomSheetDialogFragment
import org.thoughtcrime.securesms.util.BottomSheetUtil

/**
 * A bottom sheet that allows the user to select what device they want to route audio to. Intended to be used with Android 31+ APIs.
 */
class TelecomAudioOutputBottomSheet : ComposeBottomSheetDialogFragment(), DialogInterface {
  private val viewModel by viewModels<TelecomAudioOutputViewModel>()

  @Composable
  override fun SheetContent() {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .padding(16.dp)
        .wrapContentSize()
    ) {
      BottomSheets.Handle()
      DeviceList(audioOutputOptions = viewModel.audioRoutes.toImmutableList(), initialDeviceId = viewModel.defaultDeviceId!!, modifier = Modifier.fillMaxWidth(), onDeviceSelected = viewModel.onClick)
    }
  }

  override fun cancel() {
    dismiss()
  }

  fun show(fm: FragmentManager, tag: String?, audioRoutes: List<TelecomAudioOutputOption>, selectedDeviceId: ParcelUuid, onClick: (TelecomAudioOutputOption) -> Unit, onDismiss: (DialogInterface) -> Unit) {
    super.showNow(fm, tag)
    viewModel.audioRoutes = audioRoutes
    viewModel.defaultDeviceId = selectedDeviceId
    viewModel.onClick = onClick
    viewModel.onDismiss = onDismiss
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    viewModel.onDismiss(dialog)
  }

  companion object {
    const val TAG = "TelecomAudioOutputBottomSheet"

    @JvmStatic
    fun show(fragmentManager: FragmentManager, audioRoutes: List<TelecomAudioOutputOption>, selectedDeviceId: ParcelUuid, onClick: (TelecomAudioOutputOption) -> Unit, onDismiss: (DialogInterface) -> Unit): TelecomAudioOutputBottomSheet {
      val bottomSheet = TelecomAudioOutputBottomSheet()
      bottomSheet.show(fragmentManager, BottomSheetUtil.STANDARD_BOTTOM_SHEET_FRAGMENT_TAG, audioRoutes, selectedDeviceId, onClick, onDismiss)
      return bottomSheet
    }
  }
}

@Composable
fun DeviceList(audioOutputOptions: ImmutableList<TelecomAudioOutputOption>, initialDeviceId: ParcelUuid, modifier: Modifier = Modifier.fillMaxWidth(), onDeviceSelected: (TelecomAudioOutputOption) -> Unit) {
  var selectedDeviceId by rememberSaveable { mutableStateOf(initialDeviceId) }
  Column(
    horizontalAlignment = Alignment.Start,
    modifier = modifier
  ) {
    Text(
      text = stringResource(R.string.WebRtcAudioOutputToggle__audio_output),
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier
        .padding(8.dp)
    )
    Column(Modifier.selectableGroup()) {
      audioOutputOptions.forEach { device: TelecomAudioOutputOption ->
        Row(
          Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
              selected = (device.deviceId == selectedDeviceId),
              onClick = {
                onDeviceSelected(device)
                selectedDeviceId = device.deviceId
              },
              role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(
            selected = (device.deviceId == selectedDeviceId),
            onClick = null // null recommended for accessibility with screenreaders
          )
          Icon(
            modifier = Modifier.padding(start = 16.dp),
            painter = painterResource(id = getDrawableResourceForDeviceType(device.type)),
            contentDescription = stringResource(id = getDescriptionStringResourceForDeviceType(device.type)),
            tint = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = device.friendlyName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
          )
        }
      }
    }
  }
}

class TelecomAudioOutputViewModel : ViewModel() {
  var audioRoutes: List<TelecomAudioOutputOption> = emptyList()
  var defaultDeviceId: ParcelUuid? = null
  var onClick: (TelecomAudioOutputOption) -> Unit = {}
  var onDismiss: (DialogInterface) -> Unit = {}
}

private fun getDrawableResourceForDeviceType(@CallEndpointCompat.Companion.EndpointType deviceType: Int): Int {
  return when (deviceType) {
    TYPE_WIRED_HEADSET -> R.drawable.symbol_headphones_outline_24
    TYPE_EARPIECE -> R.drawable.symbol_phone_speaker_outline_24
    TYPE_BLUETOOTH -> R.drawable.symbol_speaker_bluetooth_fill_white_24
    TYPE_SPEAKER -> R.drawable.symbol_speaker_outline_24
    else -> R.drawable.symbol_speaker_outline_24
  }
}

private fun getDescriptionStringResourceForDeviceType(@CallEndpointCompat.Companion.EndpointType deviceType: Int): Int {
  return when (deviceType) {
    TYPE_WIRED_HEADSET -> R.string.WebRtcAudioOutputBottomSheet__headset_icon_content_description
    TYPE_EARPIECE -> R.string.WebRtcAudioOutputBottomSheet__earpiece_icon_content_description
    TYPE_BLUETOOTH-> R.string.WebRtcAudioOutputBottomSheet__bluetooth_icon_content_description
    TYPE_SPEAKER -> R.string.WebRtcAudioOutputBottomSheet__speaker_icon_content_description
    else -> R.string.WebRtcAudioOutputBottomSheet__speaker_icon_content_description
  }
}

data class TelecomAudioOutputOption(val friendlyName: String, @CallEndpointCompat.Companion.EndpointType val type: Int, val deviceId: ParcelUuid)

