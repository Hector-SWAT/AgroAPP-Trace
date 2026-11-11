package com.agrotrace.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.agrotrace.app.data.Aplicacion
import com.agrotrace.app.data.Lote
import java.io.File

/** Genera el reporte CSV (abre en Excel) del historial de un lote y lo comparte. */
object Reporte {

    private fun campo(v: Any?): String {
        val s = v?.toString() ?: ""
        return "\"" + s.replace("\"", "\"\"").replace("\n", " | ") + "\""
    }

    fun csv(lote: Lote, apps: List<Aplicacion>): String = buildString {
        // BOM para que Excel reconozca UTF-8 (acentos)
        append('﻿')
        appendLine("Reporte de aplicaciones - AgroTrace")
        appendLine(listOf("Lote", lote.id, "Nombre", lote.nombre, "Cultivo", lote.cultivo?.nombre, "Superficie (ha)", lote.superficie, "Productor", lote.productor).joinToString(",") { campo(it) })
        appendLine()
        appendLine(listOf(
            "Fecha", "Producto", "Formulación", "Plaga/objetivo", "Superficie (ha)", "Dosis/ha", "Cantidad total", "Unidad",
            "Caldo (L/ha)", "Tanque (L)", "Tanques", "Temp (°C)", "Humedad (%)", "Viento (km/h)", "Lluvia prevista",
            "EPP", "Aplicador", "Reentrada desde", "Cosecha desde", "Reaplicación desde", "Estado", "Advertencias", "Observaciones"
        ).joinToString(",") { campo(it) })
        apps.sortedBy { it.fecha }.forEach { a ->
            val p = a.producto
            appendLine(listOf(
                Formato.fechaHora(a.fecha), p?.nombre, p?.formulacion, a.plaga, a.superficie, a.dosisHa, a.cantidadTotal,
                p?.unidad?.principal, a.volumenCaldoHa, a.capacidadTanque, a.tanques, a.temperatura, a.humedad, a.viento,
                if (a.lluvia) "Sí" else "No", a.epp, a.aplicador, Formato.fechaHora(a.reentradaHasta),
                Formato.fecha(a.carenciaHasta), Formato.fecha(a.reaplicacionDesde), a.estado, a.advertencias, a.observaciones
            ).joinToString(",") { campo(it) })
        }
    }

    fun compartir(context: Context, lote: Lote, apps: List<Aplicacion>) {
        val dir = File(context.cacheDir, "reportes").apply { mkdirs() }
        val archivo = File(dir, "reporte_${lote.id}.csv")
        archivo.writeText(csv(lote, apps), Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte de aplicaciones ${lote.id}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
    }
}
