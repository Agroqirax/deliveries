// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.widget

import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.material3.ColorProviders
import dev.itsvic.parceltracker.ui.theme.darkScheme
import dev.itsvic.parceltracker.ui.theme.lightScheme

@Composable
fun ParcelWidgetTheme(content: @Composable () -> Unit) {
  val context = LocalContext.current
  val colors =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ColorProviders(
            light = dynamicLightColorScheme(context), dark = dynamicDarkColorScheme(context))
      } else {
        ColorProviders(light = lightScheme, dark = darkScheme)
      }
  GlanceTheme(colors = colors, content = content)
}
