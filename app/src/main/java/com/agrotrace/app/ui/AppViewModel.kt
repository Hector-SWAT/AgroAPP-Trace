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
import com.agrotrace.app.data.Productor
import com.agrotrace.app.data.QrLote
import com.agrotrace.app.data.QrProductor
import com.agrotrace.app.data.TextoQr
import com.agrotrace.app.domain.AplicacionPrevia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

sealed class Pantalla {
    data object Bienvenida : Pantalla()
    data object Login : Pantalla()
    data class Registro(val nombre: String = "", val idFijo: String? = null) : Pantalla()
    data object Inicio : Pantalla()
    data object Catalogo : Pantalla()
    data object NuevoLote : Pantalla()
    data object Credencial : Pantalla()
    data class QrDeLote(val loteId: String) : Pantalla()
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

    val pila = mutableStateListOf<Pantalla>(Pantalla.Bienvenida)
    val actual: Pantalla get() = pila.last()

    /** Productor con sesión iniciada (null = sin sesión). */
    var productor by mutableStateOf<Productor?>(null); private set
    /** Lote escaneado sin sesión: se vincula al productor que inicie sesión o se registre. */
    var lotePendiente by mutableStateOf<QrLote?>(null); private set
    var productoresRegistrados by mutableStateOf<List<Productor>>(emptyList()); private set

    var resumenes by mutableStateOf<List<ResumenLote>>(emptyList()); private set
    var totalAplicaciones by mutableStateOf(0); private set
    var aplicacionesHoy by mutableStateOf(0); private set

    var lote by mutableStateOf<Lote?>(null); private set
    var historial by mutableStateOf<List<Aplicacion>>(emptyList()); private set
    var aplicacion by mutableStateOf<Aplicacion?>(null); private set

    var mensaje by mutableStateOf<String?>(null)

    var aplicadorPredeterminado: String
        get() = prefs.getString("aplicador", null) ?: productor?.nombre ?: ""
        set(v) { prefs.edit().putString("aplicador", v).apply() }

    init {
        viewModelScope.launch {
            val id = prefs.getString("sesion", null)
            val p = id?.let { withContext(Dispatchers.IO) { db.productor(it) } }
            if (p != null && productor == null) {
                productor = p
                if (pila.size == 1 && pila[0] == Pantalla.Bienvenida) raiz(Pantalla.Inicio)
            }
        }
    }

    // ---------- Navegación ----------
    fun ir(p: Pantalla) { pila.add(p); cargar(p) }

    fun atras(): Boolean {
        if (pila.size <= 1) return false
        pila.removeAt(pila.lastIndex)
        cargar(actual)
        return true
    }

    private fun raiz(p: Pantalla) {
        pila.clear(); pila.add(p); cargar(p)
    }

    private fun reemplazarTope(p: Pantalla) {
        pila.removeAt(pila.lastIndex)
        ir(p)
    }

    private fun cargar(p: Pantalla) {
        when (p) {
            is Pantalla.Inicio -> refrescarInicio()
            is Pantalla.Login -> viewModelScope.launch {
                productoresRegistrados = withContext(Dispatchers.IO) { db.productores() }
            }
            is Pantalla.DetalleLote -> cargarLote(p.loteId)
            is Pantalla.NuevaAplicacion -> cargarLote(p.loteId)
            is Pantalla.QrDeLote -> cargarLote(p.loteId)
            is Pantalla.DetalleAplicacion -> viewModelScope.launch {
                aplicacion = withContext(Dispatchers.IO) { db.aplicacion(p.aplicacionId) }
            }
            else -> Unit
        }
    }

    private fun refrescarInicio() = viewModelScope.launch {
        val p = productor ?: return@launch
        val inicioDia = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val ahora = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            val r = db.lotesDe(p.id).map { l ->
                val apps = db.aplicaciones(l.id)
                ResumenLote(
                    lote = l,
                    aplicaciones = apps.size,
                    reentradaHasta = apps.maxOfOrNull { it.reentradaHasta }?.takeIf { it > ahora },
                    carenciaHasta = apps.maxOfOrNull { it.carenciaHasta }?.takeIf { it > ahora }
                )
            }
            Triple(r, db.contarAplicaciones(p.id), db.contarAplicaciones(p.id, inicioDia))
        }.let { (r, total, hoy) ->
            resumenes = r; totalAplicaciones = total; aplicacionesHoy = hoy
        }
    }

    private fun cargarLote(id: String) = viewModelScope.launch {
        val (l, apps) = withContext(Dispatchers.IO) { db.lote(id) to db.aplicaciones(id) }
        lote = l; historial = apps
    }

    fun historialPrevio(): List<AplicacionPrevia> = historial.map { AplicacionPrevia(it.productoId, it.fecha) }

    // ---------- Sesión ----------

    /** Inicia la sesión del productor; si había un lote escaneado pendiente, lo vincula y lo abre. */
    private suspend fun entrar(p: Productor) {
        productor = p
        prefs.edit().putString("sesion", p.id).apply()
        val pendiente = lotePendiente
        lotePendiente = null
        val vinculado = pendiente?.let { q -> withContext(Dispatchers.IO) { db.guardarLoteDesdeQr(q, p) } }
        raiz(Pantalla.Inicio)
        if (vinculado != null) {
            ir(Pantalla.DetalleLote(vinculado.id))
            mensaje = "Bienvenido, ${p.nombre}. Lote ${vinculado.id} vinculado."
        } else {
            mensaje = "Bienvenido, ${p.nombre}"
        }
    }

    fun login(nombre: String, pin: String) = viewModelScope.launch {
        val p = withContext(Dispatchers.IO) {
            db.productoresPorNombre(nombre).firstOrNull { it.pinHash == Productor.hashPin(it.id, pin) }
        }
        if (p == null) mensaje = "Nombre o PIN incorrectos." else entrar(p)
    }

    fun registrarProductor(nombre: String, comunidad: String, telefono: String, pin: String, idFijo: String?) = viewModelScope.launch {
        val p = withContext(Dispatchers.IO) {
            val id = idFijo?.takeIf { !db.existeId("productores", it) } ?: TextoQr.nuevoId("PR") { db.existeId("productores", it) }
            val nuevo = Productor(id, nombre.trim(), comunidad.trim(), telefono.trim(), Productor.hashPin(id, pin), System.currentTimeMillis())
            runCatching { db.crearProductor(nuevo); nuevo }.getOrNull()
        }
        if (p == null) mensaje = "No se pudo crear el registro." else entrar(p)
    }

    fun cerrarSesion() {
        productor = null
        lotePendiente = null
        prefs.edit().remove("sesion").apply()
        raiz(Pantalla.Bienvenida)
        mensaje = "Sesión cerrada"
    }

    // ---------- Lotes ----------

    fun crearLote(nombre: String, cultivoId: Int, superficie: Double, ubicacion: String) = viewModelScope.launch {
        val p = productor ?: return@launch
        val l = withContext(Dispatchers.IO) {
            val id = TextoQr.nuevoId("LT") { db.existeId("lotes", it) }
            Lote(id, nombre.trim(), cultivoId, superficie, p.nombre, ubicacion.trim(), System.currentTimeMillis(), p.id)
                .also { db.guardarLote(it) }
        }
        pila.removeAt(pila.lastIndex) // quita "Nuevo lote"
        ir(Pantalla.DetalleLote(l.id))
        ir(Pantalla.QrDeLote(l.id))
        mensaje = "Lote ${l.id} creado. Imprime su código QR."
    }

    // ---------- Lectura de códigos QR (sustituye al NFC) ----------
    fun procesarQr(texto: String) = viewModelScope.launch {
        // 1) Credencial de productor -> iniciar sesión
        val qp = QrProductor.parse(texto)
        if (qp != null) {
            val p = withContext(Dispatchers.IO) { db.productor(qp.id) }
            if (p != null) { entrar(p); return@launch }
            raiz(Pantalla.Bienvenida)
            ir(Pantalla.Registro(qp.nombre ?: "", qp.id))
            mensaje = "Este productor no está en el teléfono. Completa su registro."
            return@launch
        }
        // 2) Etiqueta de lote
        val q = QrLote.parse(texto)
        if (q == null) { mensaje = "El código no es de AgroTrace."; return@launch }
        val existente = withContext(Dispatchers.IO) { db.lote(q.id) }
        if (existente == null && !q.completo) {
            mensaje = "El lote ${q.id} no está registrado en este teléfono. Escanee su código QR completo."
            return@launch
        }
        val duenoId = existente?.productorId
        val dueno = duenoId?.let { withContext(Dispatchers.IO) { db.productor(it) } } ?: productor
        if (dueno == null) {
            // Sin sesión y lote sin productor: pedir inicio de sesión o registro
            lotePendiente = q
            raiz(Pantalla.Bienvenida)
            mensaje = "Lote ${q.id}: inicia sesión o regístrate para vincularlo."
            return@launch
        }
        val l = withContext(Dispatchers.IO) { db.guardarLoteDesdeQr(q, dueno) } ?: return@launch
        if (productor?.id != dueno.id) {
            productor = dueno
            prefs.edit().putString("sesion", dueno.id).apply()
            mensaje = "Sesión iniciada como ${dueno.nombre} · Lote ${l.id}"
        } else {
            mensaje = "Lote ${l.id} – ${l.nombre}"
        }
        raiz(Pantalla.Inicio)
        ir(Pantalla.DetalleLote(l.id))
    }

    // ---------- Registro de aplicaciones ----------
    fun registrar(a: Aplicacion) = viewModelScope.launch {
        val id = withContext(Dispatchers.IO) { runCatching { db.insertar(a) } }
        id.onSuccess {
            aplicadorPredeterminado = a.aplicador
            mensaje = "Aplicación registrada en el lote ${a.loteId}"
            reemplazarTope(Pantalla.DetalleAplicacion(it))
        }.onFailure { mensaje = "No se pudo guardar: ${it.message}" }
    }
}
