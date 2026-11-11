package com.agrotrace.app

import com.agrotrace.app.data.Catalogo
import com.agrotrace.app.data.Epp
import com.agrotrace.app.data.QrLote
import com.agrotrace.app.data.Toxicidad
import com.agrotrace.app.domain.AplicacionPrevia
import com.agrotrace.app.domain.Condiciones
import com.agrotrace.app.domain.Entrada
import com.agrotrace.app.domain.Evaluador
import com.agrotrace.app.domain.Nivel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluadorTest {
    private val ahora = 1_800_000_000_000L
    private val maiz = Catalogo.cultivo(3)!!
    private val imida = Catalogo.producto(1)!!
    private val base = Entrada(imida, maiz, 2.5, 2.5, 0.2, Condiciones(25.0, 60.0, 6.0, false),
        imida.epp, 200.0, 20.0, ahora, emptyList())

    @Test fun casoIdeal() {
        val r = Evaluador.evaluar(base)
        assertEquals(Nivel.OK, r.estado)
        assertEquals(0.5, r.cantidadTotal, 1e-9)
        assertEquals(25, r.mezcla!!.tanquesTotales)
        assertEquals(0.02, r.mezcla!!.productoPorTanque, 1e-9)
    }

    @Test fun ultimoTanqueParcial() {
        val m = Evaluador.calcularMezcla(0.2, 1.35, 200.0, 20.0)
        assertEquals(14, m.tanquesTotales); assertEquals(13, m.tanquesCompletos)
        assertEquals(10.0, m.caldoUltimoTanque, 1e-6)
    }

    @Test fun bloqueos() {
        fun bloquea(e: Entrada) = assertEquals(Nivel.BLOQUEO, Evaluador.evaluar(e).estado)
        bloquea(base.copy(condiciones = Condiciones(25.0, 60.0, 20.0, false)))       // viento
        bloquea(base.copy(eppMarcado = setOf(Epp.GUANTES)))                          // EPP
        bloquea(base.copy(dosisHa = 0.5))                                            // fuera de rango
        bloquea(base.copy(superficie = 3.0))                                         // mayor al lote
        bloquea(base.copy(cultivo = Catalogo.cultivo(9)!!))                          // no autorizado
        bloquea(base.copy(historial = listOf(AplicacionPrevia(1, ahora - 5 * Evaluador.DIA)))) // reaplicación
    }

    @Test fun cultivosSoloBajaToxicidad() {
        Catalogo.cultivos.filter { it.soloBajaToxicidad }.forEach { c ->
            assertTrue(Catalogo.productosPara(c).all { it.toxicidad == Toxicidad.BAJA })
        }
    }

    @Test fun lecturaQr() {
        val q = QrLote.parse("agrotrace://lote?v=1&id=LT-2611-4821&nom=Parcela%20Norte&cul=3&ha=2.5")
        assertNotNull(q); assertTrue(q!!.completo); assertEquals("Parcela Norte", q.nombre)
        assertNull(QrLote.parse("texto cualquiera"))
    }
}
