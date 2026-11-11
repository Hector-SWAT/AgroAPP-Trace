package com.agrotrace.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrotrace.app.data.AgroDb
import com.agrotrace.app.data.Aplicacion
import com.agrotrace.app.data.Lote
import com.agrotrace.app.data.QrLote
import com.agrotrace.app.domain.AplicacionPrevia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

sealed class Pantalla {
    data object Inicio : Pantalla()
    data object Catalogo : Pantalla()
    data class DetalleLote(val loteId: String) : Pantalla()
    data class NuevaAplicacion(val loteId: String) : Pantalla()
    data class DetalleAplicacion(val aplicacionId: Long) : Pantalla()
}

/** Resumen de un lote para la lista de inicio. */
data class ResumenLote(
    val lote: Lote,
    val aplicaciones: Int,
    val reentradaHasta: Long?,
    val carenciaHasta: Long?
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AgroDb(app)
    private val prefs = app.getSharedPreferences("agrotrace", 0)

    val pila = mutableStateListOf<Pantalla>(Pantalla.Inicio)
    val actual: Pantalla get() = pila.last()

    var resumenes by mutableStateOf<List<ResumenLote>>(emptyList()); private set
    var totalAplicaciones by mutableStateOf(0); private set
    var aplicacionesHoy by mutableStateOf(0); private set

    var lote by mutableStateOf<Lote?>(null); private set
    var historial by mutableStateOf<List<Aplicacion>>(emptyList()); private set
    var aplicacion by mutableStateOf<Aplicacion?>(null); private set

    var mensaje by mutableStateOf<String?>(null)

    var aplicadorPredeterminado: String
        get() = prefs.getString("aplicador", "") ?: ""
        set(v) { prefs.edit().putString("aplicador", v).apply() }

    init { refrescarInicio() }

    // ---------- Navegación ----------
    fun ir(p: Pantalla) { pila.add(p); cargar(p) }

    fun atras(): Boolean {
        if (pila.size <= 1) return false
        pila.removeAt(pila.lastIndex)
        cargar(actual)
        return true
    }

    private fun reemplazarTope(p: Pantalla) {
        pila.removeAt(pila.lastIndex)
        ir(p)
    }

    private fun cargar(p: Pantalla) {
        when (p) {
            is Pantalla.Inicio -> refrescarInicio()
            is Pantalla.DetalleLote -> cargarLote(p.loteId)
            is Pantalla.NuevaAplicacion -> cargarLote(p.loteId)
            is Pantalla.DetalleAplicacion -> viewModelScope.launch {
                aplicacion = withContext(Dispatchers.IO) { db.aplicacion(p.aplicacionId) }
            }
            is Pantalla.Catalogo -> Unit
        }
    }

    private fun refrescarInicio() = viewModelScope.launch {
        val inicioDia = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val ahora = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            val r = db.lotes().map { l ->
                val apps = db.aplicaciones(l.id)
                ResumenLote(
                    lote = l,
                    aplicaciones = apps.size,
                    reentradaHasta = apps.maxOfOrNull { it.reentradaHasta }?.takeIf { it > ahora },
                    carenciaHasta = apps.maxOfOrNull { it.carenciaHasta }?.takeIf { it > ahora }
                )
            }
            Triple(r, db.totalAplicaciones(), db.aplicacionesDesde(inicioDia))
        }.let { (r, total, hoy) ->
            resumenes = r; totalAplicaciones = total; aplicacionesHoy = hoy
        }
    }

    private fun cargarLote(id: String) = viewModelScope.launch {
        val (l, apps) = withContext(Dispatchers.IO) { db.lote(id) to db.aplicaciones(id) }
        lote = l; historial = apps
    }

    fun historialPrevio(): List<AplicacionPrevia> = historial.map { AplicacionPrevia(it.productoId, it.fecha) }

    // ---------- QR (sustituye la lectura NFC) ----------
    fun procesarQr(texto: String) = viewModelScope.launch {
        val q = QrLote.parse(texto)
        if (q == null) { mensaje = "El código no es una etiqueta de lote de AgroTrace."; return@launch }
        val l = withContext(Dispatchers.IO) { db.guardarLoteDesdeQr(q) }
        if (l == null) {
            mensaje = "El lote ${q.id} no está registrado en este teléfono. Escanee su código QR completo."
            return@launch
        }
        mensaje = "Lote ${l.id} – ${l.nombre}"
        // Volver a inicio y abrir el lote escaneado
        while (pila.size > 1) pila.removeAt(pila.lastIndex)
        ir(Pantalla.DetalleLote(l.id))
    }

    // ---------- Registro ----------
    fun registrar(a: Aplicacion) = viewModelScope.launch {
        val id = withContext(Dispatchers.IO) { runCatching { db.insertar(a) } }
        id.onSuccess {
            aplicadorPredeterminado = a.aplicador
            mensaje = "Aplicación registrada en el lote ${a.loteId}"
            reemplazarTope(Pantalla.DetalleAplicacion(it))
        }.onFailure { mensaje = "No se pudo guardar: ${it.message}" }
    }
}
