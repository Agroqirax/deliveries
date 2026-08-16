// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.components

import androidx.annotation.DrawableRes
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.api.Status

@DrawableRes
fun statusIconRes(status: Status): Int =
    when (status) {
      Status.Preadvice -> R.drawable.outline_other_admission_24
      Status.LockerboxAcceptedParcel -> R.drawable.outline_deployed_code_update_24
      Status.PickedUpByCourier -> R.drawable.outline_deployed_code_account_24
      Status.InTransit -> R.drawable.outline_local_shipping_24
      Status.InWarehouse -> R.drawable.outline_warehouse_24
      Status.Customs -> R.drawable.outline_search_24
      Status.OutForDelivery -> R.drawable.outline_delivery_truck_speed_24
      Status.DeliveryFailure -> R.drawable.outline_error_24
      Status.PickupTimeEndingSoon -> R.drawable.outline_notifications_active_24
      Status.AwaitingPickup -> R.drawable.outline_pin_drop_24
      Status.Delivered,
      Status.PickedUp -> R.drawable.outline_check_24
      Status.DeliveredToNeighbor -> R.drawable.outline_holiday_village_24
      Status.DeliveredToASafePlace -> R.drawable.outline_roofing_24
      Status.DroppedAtCustomerService -> R.drawable.outline_support_agent_24
      Status.ReturningToSender -> R.drawable.outline_arrow_top_left_24
      Status.ReturnedToSender -> R.drawable.outline_arrow_top_left_24
      Status.Delayed -> R.drawable.outline_deployed_code_history_24
      Status.Damaged -> R.drawable.outline_deployed_code_alert_24
      Status.Destroyed -> R.drawable.outline_destruction_24
      else -> R.drawable.outline_question_mark_24
    }
