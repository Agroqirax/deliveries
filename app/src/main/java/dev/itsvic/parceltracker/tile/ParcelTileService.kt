// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import dev.itsvic.parceltracker.DEMO_MODE
import dev.itsvic.parceltracker.EXTRA_OPEN_PARCEL
import dev.itsvic.parceltracker.MainActivity
import dev.itsvic.parceltracker.ParcelApplication
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.dataStore
import dev.itsvic.parceltracker.db.ParcelWithStatus
import dev.itsvic.parceltracker.db.demoModeParcels
import dev.itsvic.parceltracker.ui.components.statusIconRes
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ParcelTileService : TileService() {
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  // Resolved by the last onStartListening() refresh, so onClick() doesn't need to re-query.
  private var openParcelId: Int? = null

  override fun onDestroy() {
    super.onDestroy()
    scope.cancel()
  }

  override fun onStartListening() {
    super.onStartListening()
    scope.launch { updateTileState() }
  }

  // The deprecated Intent overload is only reachable below API 34, where it doesn't carry
  // the targetSdk-gated UnsupportedOperationException that the replacement API 34 method
  // added — lint can't see that the SDK_INT check below makes it safe.
  @SuppressLint("StartActivityAndCollapseDeprecated")
  override fun onClick() {
    super.onClick()
    val parcelId = openParcelId ?: return

    val intent =
        Intent(this, MainActivity::class.java).apply {
          putExtra(EXTRA_OPEN_PARCEL, parcelId)
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      val pendingIntent =
          PendingIntent.getActivity(
              this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
      startActivityAndCollapse(pendingIntent)
    } else {
      startActivityAndCollapse(intent)
    }
  }

  private suspend fun updateTileState() {
    val tile = qsTile ?: return

    val demoMode = dataStore.data.first()[DEMO_MODE] == true
    val parcel: ParcelWithStatus? =
        try {
          if (demoMode) {
            demoModeParcels.maxByOrNull { it.status?.lastChange ?: Instant.MIN }
          } else {
            ParcelApplication.db.parcelDao().getMostRecentlyUpdatedNonArchived()
          }
        } catch (e: Exception) {
          Log.w("ParcelTileService", "Failed to load most recent parcel", e)
          null
        }

    val status = parcel?.status?.status

    if (parcel == null || status == null) {
      openParcelId = null
      tile.state = Tile.STATE_INACTIVE
      tile.icon = Icon.createWithResource(this, R.drawable.package_2)
      tile.label = getString(R.string.tile_no_parcels)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = null
    } else {
      openParcelId = parcel.parcel.id
      val statusText = getString(status.nameResource)
      tile.state = Tile.STATE_ACTIVE
      tile.icon = Icon.createWithResource(this, statusIconRes(status))
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        tile.label = parcel.parcel.humanName
        tile.subtitle = statusText
      } else {
        tile.label = "${parcel.parcel.humanName}: $statusText"
      }
    }

    tile.updateTile()
  }
}

/**
 * Nudges the tile to refresh even while the Quick Settings panel isn't open, mirroring
 * [dev.itsvic.parceltracker.widget.refreshParcelWidgets].
 */
fun Context.refreshParcelTile() {
  TileService.requestListeningState(this, ComponentName(this, ParcelTileService::class.java))
}
