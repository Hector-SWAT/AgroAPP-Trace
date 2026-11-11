package com.agrotrace.app.domain

import com.agrotrace.app.data.Cultivo
import com.agrotrace.app.data.Epp
import com.agrotrace.app.data.Producto
import com.agrotrace.app.data.Toxicidad
import kotlin.math.ceil
import kotlin.math.floor

enum class Nivel { OK, AVISO, BLOQUEO }

data class Hallazgo(val nivel: Nivel, val texto: String)

data class Condiciones(
    val temperatura: Double,
    val humedad: Double,
    val viento: Double,
    val lluviaProxima: Boolean
)

/** Aplicación previa del mismo lote, usada para validar intervalos y número máximo por ciclo. */
data class AplicacionPrevia(val productoId: Int, val fechaMillis: Long)

data class Entrada(
    val producto: Producto,
    val cultivo: Cultivo,
    val superficieLote: Double,
    val superficie: Double,
    val dosisHa: Double,
    val condiciones: Condiciones,
    val eppMarcado: Set<Epp>,
    val volumenCaldoHa: Double,
    val capacidadTanque: Double,
    val fechaMillis: Long,
    val historial: List<AplicacionPrevia>
)

/** Preparación del caldo por tanque (mochila o tanque de aspersora). */
data class Mezcla(
    val caldoTotalL: Double,
    val tanquesTotales: Int,
    val tanquesCompletos: Int,
    val productoPorTanque: Double,
    val caldoUltimoTanque: Double,     // 0 si todos los tanques van llenos
    val productoUltimoTanque: Double
)

data class Resultado(
    val hallazgos: List<Hallazgo>,
    val dosisHa: Double,
    val cantidadTotal: Double,
    val mezcla: Mezcla?,
    val reentradaHasta: Long,
    val carenciaHasta: Long,
    val reaplicacionDesde: Long
) {
    val estado: Nivel = hallazgos.maxByOrNull { it.nivel.ordinal }?.nivel ?: Nivel.OK
    val puedeRegistrar: Boolean get() = estado != Nivel.BLOQUEO
}

object Evaluador {
    const val HORA = 3_600_000L
    const val DIA = 24 * HORA

    /** Dosis sugerida según presión de plaga, siempre dentro del rango de etiqueta. */
    fun dosisPorPresion(p: Producto, presion: Int): Double = when (presion) {
        0 -> p.dosisMin
        2 -> p.dosisMax
        else -> (p.dosisMin + p.dosisMax) / 2.0
    }

    fun calcularMezcla(dosisHa: Double, superficie: Double, volumenHa: Double, capacidad: Double): Mezcla {
        val caldoTotal = volumenHa * superficie
        val completos = floor(caldoTotal / capacidad + 1e-9).toInt()
        var resto = caldoTotal - completos * capacidad
        if (resto < 1e-6) resto = 0.0
        val totales = completos + if (resto > 0) 1 else 0
        val concentracion = dosisHa / volumenHa // producto por litro de caldo
        return Mezcla(
            caldoTotalL = caldoTotal,
            tanquesTotales = totales,
            tanquesCompletos = completos,
            productoPorTanque = concentracion * capacidad,
            caldoUltimoTanque = resto,
            productoUltimoTanque = concentracion * resto
        )
    }

    fun evaluar(e: Entrada): Resultado {
        val h = mutableListOf<Hallazgo>()
        val p = e.producto
        val c = e.cultivo
        val cond = e.condiciones

        // 1. Superficie
        if (e.superficie <= 0.0) h += Hallazgo(Nivel.BLOQUEO, "La superficie debe ser mayor que 0 ha.")
        if (e.superficie > e.superficieLote + 1e-9)
            h += Hallazgo(Nivel.BLOQUEO, "La superficie (${e.superficie} ha) supera la del lote (${e.superficieLote} ha).")

        // 2. Dosis dentro de etiqueta (nunca se ajusta fuera del rango)
        if (e.dosisHa < p.dosisMin - 1e-9 || e.dosisHa > p.dosisMax + 1e-9)
            h += Hallazgo(Nivel.BLOQUEO, "Dosis fuera del rango de referencia (${p.dosisMin}–${p.dosisMax} ${p.unidad.principal}/ha).")

        // 3. Compatibilidad producto–cultivo
        if (c.id !in p.cultivosPermitidos)
            h += Hallazgo(Nivel.BLOQUEO, "${p.nombre} no está autorizado para ${c.nombre} en el catálogo.")
        if (c.soloBajaToxicidad && p.toxicidad != Toxicidad.BAJA)
            h += Hallazgo(Nivel.BLOQUEO, "${c.nombre} solo admite productos de baja toxicidad.")
        if (p.restringido)
            h += Hallazgo(Nivel.AVISO, "${p.nombre} es de uso restringido o prohibido en varios países. Verifique su registro vigente.")

        // 4. Equipo de protección personal (obligatorio)
        val faltantes = p.epp - e.eppMarcado
        if (faltantes.isNotEmpty())
            h += Hallazgo(Nivel.BLOQUEO, "Falta EPP obligatorio: " + faltantes.joinToString { it.etiqueta } + ".")

        // 5. Condiciones ambientales (deciden si se aplica; NO modifican la dosis)
        val rangosOk = cond.temperatura in -10.0..55.0 && cond.humedad in 0.0..100.0 && cond.viento in 0.0..120.0
        if (!rangosOk) {
            h += Hallazgo(Nivel.BLOQUEO, "Revise los datos de clima: temperatura −10 a 55 °C, humedad 0 a 100 %, viento 0 a 120 km/h.")
        } else {
            val vientoLimite = minOf(15.0, p.vientoMax ?: 15.0)
            if (p.enCaldo) {
                when {
                    cond.viento > vientoLimite ->
                        h += Hallazgo(Nivel.BLOQUEO, "Viento de ${fmt(cond.viento)} km/h: no aplicar (límite ${fmt(vientoLimite)} km/h, riesgo de deriva).")
                    cond.viento > 10.0 ->
                        h += Hallazgo(Nivel.AVISO, "Viento moderado-fuerte: use boquillas antideriva y aplique lejos de cultivos vecinos.")
                    cond.viento < 3.0 ->
                        h += Hallazgo(Nivel.AVISO, "Viento casi nulo: posible inversión térmica; las gotas finas pueden quedar suspendidas.")
                }
            }
            val tempLimite = minOf(35.0, p.tempMax ?: 35.0)
            when {
                cond.temperatura > tempLimite ->
                    h += Hallazgo(Nivel.BLOQUEO, "Temperatura de ${fmt(cond.temperatura)} °C: no aplicar (máximo ${fmt(tempLimite)} °C para este producto).")
                cond.temperatura > 30.0 ->
                    h += Hallazgo(Nivel.AVISO, "Temperatura alta: aplique temprano por la mañana o al atardecer.")
                cond.temperatura < 5.0 ->
                    h += Hallazgo(Nivel.AVISO, "Temperatura baja: la eficacia del producto puede disminuir.")
            }
            if (p.enCaldo && cond.humedad < 40.0)
                h += Hallazgo(Nivel.AVISO, "Humedad baja (<40 %): mayor evaporación y deriva de gotas.")
            if (cond.lluviaProxima)
                h += Hallazgo(Nivel.AVISO, "Lluvia prevista: el producto puede lavarse. Considere posponer la aplicación.")
        }

        // 6. Historial del lote: intervalo de reaplicación y máximo por ciclo
        val mismas = e.historial.filter { it.productoId == p.id && it.fechaMillis <= e.fechaMillis }
        val ultima = mismas.maxByOrNull { it.fechaMillis }
        if (ultima != null) {
            val permitida = ultima.fechaMillis + p.reaplicacionDias * DIA
            if (e.fechaMillis < permitida)
                h += Hallazgo(Nivel.BLOQUEO, "Intervalo de reaplicación no cumplido: ${p.nombre} solo puede repetirse cada ${p.reaplicacionDias} días.")
        }
        val inicioCiclo = e.fechaMillis - c.cicloDias * DIA
        val enCiclo = mismas.count { it.fechaMillis > inicioCiclo }
        if (enCiclo >= p.maxAplicacionesCiclo)
            h += Hallazgo(Nivel.BLOQUEO, "Ya se alcanzó el máximo de ${p.maxAplicacionesCiclo} aplicación(es) de ${p.nombre} por ciclo.")

        // 7. Mezcla por tanque
        var mezcla: Mezcla? = null
        if (p.enCaldo) {
            if (e.volumenCaldoHa <= 0.0 || e.capacidadTanque <= 0.0) {
                h += Hallazgo(Nivel.BLOQUEO, "Indique el volumen de caldo por hectárea y la capacidad del tanque.")
            } else if (e.superficie > 0.0) {
                mezcla = calcularMezcla(e.dosisHa, e.superficie, e.volumenCaldoHa, e.capacidadTanque)
            }
        }

        return Resultado(
            hallazgos = h,
            dosisHa = e.dosisHa,
            cantidadTotal = e.dosisHa * maxOf(e.superficie, 0.0),
            mezcla = mezcla,
            reentradaHasta = e.fechaMillis + p.reentradaHoras * HORA,
            carenciaHasta = e.fechaMillis + p.carenciaDias * DIA,
            reaplicacionDesde = e.fechaMillis + p.reaplicacionDias * DIA
        )
    }

    private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(java.util.Locale.US, "%.1f", v)
}
