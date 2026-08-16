// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ParcelWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = ParcelWidget()
}
