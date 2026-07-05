package com.elmotamyez.gallery.util

import kotlin.math.roundToInt

fun Double.formatPrice(): String {
    val cents = (this * 100).roundToInt()
    val egp = cents / 100
    val dec = (cents % 100).toString().padStart(2, '0')
    return "$egp.$dec"
}

// KMP-safe 2-decimal formatter (replaces "%.2f".format(x) which is JVM-only)
fun Double.fmt2f(): String = formatPrice()

// KMP-safe date string builder (replaces "%04d-%02d-%02d".format(...) which is JVM-only)
fun dateString(year: Int, month: Int, day: Int): String =
    "${year.toString().padStart(4,'0')}-${month.toString().padStart(2,'0')}-${day.toString().padStart(2,'0')}"

fun dateTimeString(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): String =
    "${dateString(year,month,day)}T${hour.toString().padStart(2,'0')}:${minute.toString().padStart(2,'0')}:${second.toString().padStart(2,'0')}"

fun twoDigit(n: Int): String = n.toString().padStart(2, '0')
