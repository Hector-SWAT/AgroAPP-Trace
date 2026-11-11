package com.agrotrace.app.util

import com.agrotrace.app.data.Unidad
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formato {
    private val LOC = Locale.forLanguageTag("es-MX")

    /** 0.35 L -> "350 mL"; 2.5 kg -> "2.50 kg". */
    fun cantidad(valor: Double, unidad: Unidad): String =
        if (valor < 1.0) String.format(LOC, "%.0f %s", valor * 1000.0, unidad.sub)
        else String.format(LOC, "%.2f %s", valor, unidad.principal)

    fun numero(valor: Double, decimales: Int = 2): String = String.format(LOC, "%.${decimales}f", valor)

    fun fecha(millis: Long): String = SimpleDateFormat("dd/MM/yyyy", LOC).format(Date(millis))
    fun fechaHora(millis: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm", LOC).format(Date(millis))
}
