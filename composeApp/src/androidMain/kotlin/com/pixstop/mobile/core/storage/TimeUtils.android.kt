package com.pixstop.mobile.core.storage

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun currentHourOfDay(): Int =
    java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
