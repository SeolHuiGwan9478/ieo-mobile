package com.ieo.mobile.ui.common

import java.text.NumberFormat
import java.util.Locale

/** 만 원 단위로 떨어지면 "38만 원", 아니면 "372,229원" */
fun formatWon(amount: Long): String =
    if (amount >= 10_000 && amount % 10_000 == 0L) {
        "${amount / 10_000}만 원"
    } else {
        NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"
    }
