// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import dev.itsvic.parceltracker.DEMO_MODE
import dev.itsvic.parceltracker.ParcelApplication
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.dataStore
import dev.itsvic.parceltracker.db.ParcelWithStatus
import dev.itsvic.parceltracker.db.demoModeParcels
import dev.itsvic.parceltracker.ui.components.ParcelRow
import dev.itsvic.parceltracker.ui.theme.ParcelTrackerTheme
import kotlinx.coroutines.launch

class ParcelWidgetConfigureActivity : ComponentActivity() {
  private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setResult(Activity.RESULT_CANCELED)

    appWidgetId =
        intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish()
      return
    }

    setContent {
      ParcelTrackerTheme {
        Box(
            modifier =
                Modifier.background(color = MaterialTheme.colorScheme.background).fillMaxSize()) {
              ConfigureContent(onParcelSelected = ::onParcelSelected)
            }
      }
    }
  }

  private fun onParcelSelected(parcelId: Int) {
    lifecycleScope.launch {
      val glanceId =
          GlanceAppWidgetManager(this@ParcelWidgetConfigureActivity).getGlanceIdBy(appWidgetId)
      updateAppWidgetState(
          this@ParcelWidgetConfigureActivity, PreferencesGlanceStateDefinition, glanceId) { prefs
        ->
        prefs.toMutablePreferences().apply { this[widgetParcelIdKey] = parcelId }
      }

      // Push the selection before returning a result. The host binds the widget id
      // (which starts a Glance session that composes with no parcel id set) before
      // this activity is launched, and update() is what hands the new state to a
      // session that's already running.
      try {
        ParcelWidget().update(applicationContext, glanceId)
      } catch (e: Exception) {
        Log.w("ParcelWidgetConfigure", "Failed to update widget $appWidgetId", e)
      }

      val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      setResult(Activity.RESULT_OK, resultValue)
      finish()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigureContent(onParcelSelected: (Int) -> Unit) {
  val context = LocalContext.current
  val preferences by context.dataStore.data.collectAsState(emptyPreferences())
  val demoMode = preferences[DEMO_MODE] == true
  val db = ParcelApplication.db

  val parcels: List<ParcelWithStatus> =
      if (demoMode) demoModeParcels
      else db.parcelDao().getAllWithStatus().collectAsState(initial = emptyList()).value

  Scaffold(
      topBar = { TopAppBar(title = { Text(stringResource(R.string.widget_configure_title)) }) }) {
          innerPadding ->
        if (parcels.isEmpty()) {
          Text(
              stringResource(R.string.no_parcels_flavor),
              modifier = Modifier.padding(innerPadding).padding(16.dp))
        } else {
          LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(parcels.reversed()) { parcel ->
              ParcelRow(parcel.parcel, parcel.status?.status) { onParcelSelected(parcel.parcel.id) }
            }
          }
        }
      }
}
