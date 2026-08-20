// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dev.itsvic.parceltracker.DEMO_MODE
import dev.itsvic.parceltracker.ParcelApplication
import dev.itsvic.parceltracker.dataStore
import dev.itsvic.parceltracker.db.ParcelWithStatus
import dev.itsvic.parceltracker.db.demoModeParcels
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

val widgetParcelIdKey = intPreferencesKey("parcelId")

const val WIDGET_UNCONFIGURED = -1

/** Not a real parcel id: shows whichever non-archived parcel last changed status. */
const val WIDGET_LATEST_UPDATE = -2

/** What a widget instance knows about its parcel at a given moment. */
sealed interface WidgetParcel {
  /** Nothing has been loaded yet. Renders blank rather than flashing the wrong message. */
  data object Loading : WidgetParcel

  /** No parcel has been picked for this widget instance yet. */
  data object Unconfigured : WidgetParcel

  /** A parcel was picked, but it no longer exists (deleted, or demo mode toggled). */
  data object NotFound : WidgetParcel

  data class Found(val value: ParcelWithStatus) : WidgetParcel
}

class ParcelWidget : GlanceAppWidget() {
  override val stateDefinition = PreferencesGlanceStateDefinition

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

    // Everything the widget renders has to be read from *inside* provideContent.
    // Glance runs provideGlance() once per session: update()/updateAll() against a
    // session that is still alive only refresh the state behind currentState(),
    // they never re-enter this function. Resolving the parcel id or querying the DB
    // out here would pin both for the session's entire lifetime, which is what made
    // a freshly configured widget keep showing "tap to choose a parcel" until the
    // app process was killed — the host binds the widget id (starting a session
    // that sees no parcel id) before the configure activity returns its result.
    provideContent {
      val parcelId = currentState<Preferences>()[widgetParcelIdKey] ?: WIDGET_UNCONFIGURED
      val parcel by
          remember(parcelId) { widgetParcelFlow(context, parcelId) }
              .collectAsState(WidgetParcel.Loading)

      ParcelWidgetTheme { ParcelWidgetContent(parcel = parcel, appWidgetId = appWidgetId) }
    }
  }
}

// Mirrors the demo/real branch used throughout the rest of the app (e.g.
// ParcelAppNavigation): the widget always resolves against whatever demo mode is
// currently set to, same as opening the app itself would.
@OptIn(ExperimentalCoroutinesApi::class)
private fun widgetParcelFlow(context: Context, parcelId: Int): Flow<WidgetParcel> {
  if (parcelId == WIDGET_UNCONFIGURED) return flowOf(WidgetParcel.Unconfigured)
  return context.dataStore.data
      .map { it[DEMO_MODE] == true }
      .distinctUntilChanged()
      .flatMapLatest { demoMode ->
        when {
          parcelId == WIDGET_LATEST_UPDATE && demoMode ->
              flowOf(demoModeParcels.maxByOrNull { it.status?.lastChange ?: Instant.MIN })
          parcelId == WIDGET_LATEST_UPDATE ->
              ParcelApplication.db.parcelDao().getMostRecentlyUpdatedNonArchivedFlow()
          demoMode -> flowOf(demoModeParcels.getOrNull(parcelId))
          else -> ParcelApplication.db.parcelDao().getWithStatusById(parcelId)
        }
      }
      .map { if (it != null) WidgetParcel.Found(it) else WidgetParcel.NotFound }
      .catch {
        Log.w("ParcelWidget", "Failed to load parcel $parcelId", it)
        emit(WidgetParcel.NotFound)
      }
}

suspend fun Context.refreshParcelWidgets() {
  try {
    ParcelWidget().updateAll(this)
  } catch (e: Exception) {
    Log.w("ParcelWidget", "Failed to refresh widgets", e)
  }
}
