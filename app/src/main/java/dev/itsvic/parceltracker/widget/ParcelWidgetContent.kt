// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.itsvic.parceltracker.EXTRA_OPEN_PARCEL
import dev.itsvic.parceltracker.MainActivity
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.api.Status
import dev.itsvic.parceltracker.ui.components.statusIconRes

@Composable
fun ParcelWidgetContent(
    parcel: WidgetParcel,
    appWidgetId: Int,
) {
  val context = LocalContext.current

  val clickAction =
      if (parcel is WidgetParcel.Found) {
        actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
              putExtra(EXTRA_OPEN_PARCEL, parcel.value.parcel.id)
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
      } else {
        // ParcelWidgetConfigureActivity requires EXTRA_APPWIDGET_ID to know which
        // widget instance it's configuring — without it, it immediately finish()es
        // itself, which looks like the app opening and closing right away.
        actionStartActivity(
            Intent(context, ParcelWidgetConfigureActivity::class.java).apply {
              putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
      }

  Box(
      modifier =
          GlanceModifier.fillMaxSize()
              .background(GlanceTheme.colors.background)
              .cornerRadius(16.dp)
              .padding(16.dp, 12.dp)
              .clickable(clickAction),
      contentAlignment = Alignment.CenterStart) {
        when (parcel) {
          WidgetParcel.Loading -> {}
          WidgetParcel.Unconfigured ->
              Text(
                  context.getString(R.string.widget_not_configured),
                  style = TextStyle(color = GlanceTheme.colors.onBackground))
          WidgetParcel.NotFound ->
              Text(
                  context.getString(R.string.widget_parcel_not_found),
                  style = TextStyle(color = GlanceTheme.colors.onBackground))
          is WidgetParcel.Found -> {
            val status: Status? = parcel.value.status?.status
            val iconRes = status?.let { statusIconRes(it) } ?: R.drawable.outline_question_mark_24
            val statusText =
                status?.let { context.getString(it.nameResource) }
                    ?: context.getString(R.string.status_unknown)

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
              Box(
                  modifier =
                      GlanceModifier.size(40.dp)
                          .cornerRadius(20.dp)
                          .background(GlanceTheme.colors.primaryContainer),
                  contentAlignment = Alignment.Center) {
                    Image(
                        provider = ImageProvider(iconRes),
                        contentDescription = statusText,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary))
                  }

              Column(modifier = GlanceModifier.padding(start = 12.dp)) {
                Text(
                    parcel.value.parcel.humanName,
                    style = TextStyle(color = GlanceTheme.colors.onBackground))
                Text(
                    statusText,
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
              }
            }
          }
        }
      }
}
