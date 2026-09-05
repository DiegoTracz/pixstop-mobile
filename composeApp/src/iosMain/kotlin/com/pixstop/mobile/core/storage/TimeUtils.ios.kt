package com.pixstop.mobile.core.storage

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun currentHourOfDay(): Int =
    platform.Foundation.NSCalendar.currentCalendar.component(
        platform.Foundation.NSCalendarUnitHour,
        fromDate = NSDate(),
    ).toInt()
