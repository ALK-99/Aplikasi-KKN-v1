package com.kkn.almufid.sekretaris.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatTanggal = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

fun formatTanggal(millis: Long): String = formatTanggal.format(Date(millis))

fun formatRupiah(nominal: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.maximumFractionDigits = 0
    return format.format(nominal)
}
