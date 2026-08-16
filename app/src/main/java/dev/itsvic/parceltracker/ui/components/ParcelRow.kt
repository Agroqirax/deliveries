// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.itsvic.parceltracker.api.Service
import dev.itsvic.parceltracker.api.Status
import dev.itsvic.parceltracker.api.getDeliveryServiceName
import dev.itsvic.parceltracker.db.Parcel
import dev.itsvic.parceltracker.ui.theme.ParcelTrackerTheme

@Composable
fun ParcelRow(parcel: Parcel, status: Status?, onClick: () -> Unit) {
  Row(
      modifier = Modifier.clickable(onClick = onClick).fillMaxWidth().padding(16.dp, 12.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically) {
        if (status != null)
            Box(
                modifier =
                    Modifier.size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center) {
                  Icon(
                      painterResource(statusIconRes(status)),
                      stringResource(status.nameResource),
                      tint = MaterialTheme.colorScheme.primary)
                }

        Column {
          Text(parcel.humanName, color = MaterialTheme.colorScheme.onBackground)

          Text(
              "${stringResource(getDeliveryServiceName(parcel.service)!!)}: ${parcel.parcelId}",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
}

@Composable
@PreviewLightDark
fun ParcelRowPreview() {
  ParcelTrackerTheme {
    Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.background)) {
      ParcelRow(
          Parcel(0, "My precious package", "EXMPL0001", null, Service.EXAMPLE),
          status = Status.InTransit,
          onClick = {},
      )
    }
  }
}
