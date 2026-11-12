package com.agrotrace.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

data class Productor(
    val id: String,
    val nombre: String,
    val comunidad: String,
    val telefono: String,
    val pinHash: String,
    val creado: Long
) {
    companion object {
        fun hashPin(id: String, pin: String): String =
            MessageDigest.getInstance("SHA-256").digest("$id:$pin".toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}

data class Lote(
    val id: String,
    val nombre: String,
    val cultivoId: Int,
    val superficie: Double,
    val productor: String,
    val ubicacion: String,
    val creado: Long,
    val productorId: String? = null
) {
    val cultivo: Cultivo? get() = Catalogo.cultivo(cultivoId)
}

data class Aplicacion(
    val id: Long = 0,
    val loteId: String,
    val productoId: Int,
    val cultivoId: Int,
    val fecha: Long,
    val superficie: Double,
    val dosisHa: Double,
    val cantidadTotal: Double,
    val volumenCaldoHa: Double,
    val capacidadTanque: Double,
    val tanques: Int,
    val temperatura: Double,
    val humedad: Double,
    val viento: Double,
    val lluvia: Boolean,
    val epp: String,
    val aplicador: String,
    val plaga: String,
    val observaciones: String,
    val estado: String,          // OK | AVISO
    val advertencias: String,    // separadas por salto de línea
    val reentradaHasta: Long,
    val carenciaHasta: Long,
    val reaplicacionDesde: Long
) {
    val producto: Producto? get() = Catalogo.producto(productoId)
}

/** Base de datos local SQLite (sin dependencias externas). */
class AgroDb(context: Context) : SQLiteOpenHelper(context, "agrotrace.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        crearProductores(db)
        db.execSQL(
            """CREATE TABLE lotes(
                id TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                cultivo_id INTEGER NOT NULL,
                superficie REAL NOT NULL,
                productor TEXT NOT NULL DEFAULT '',
                ubicacion TEXT NOT NULL DEFAULT '',
                creado INTEGER NOT NULL,
                productor_id TEXT)"""
        )
        db.execSQL(
            """CREATE TABLE aplicaciones(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                lote_id TEXT NOT NULL REFERENCES lotes(id),
                producto_id INTEGER NOT NULL,
                cultivo_id INTEGER NOT NULL,
                fecha INTEGER NOT NULL,
                superficie REAL NOT NULL,
                dosis_ha REAL NOT NULL,
                cantidad_total REAL NOT NULL,
                volumen_caldo_ha REAL NOT NULL,
                capacidad_tanque REAL NOT NULL,
                tanques INTEGER NOT NULL,
                temperatura REAL NOT NULL,
                humedad REAL NOT NULL,
                viento REAL NOT NULL,
                lluvia INTEGER NOT NULL,
                epp TEXT NOT NULL,
                aplicador TEXT NOT NULL,
                plaga TEXT NOT NULL,
                observaciones TEXT NOT NULL,
                estado TEXT NOT NULL,
                advertencias TEXT NOT NULL,
                reentrada_hasta INTEGER NOT NULL,
                carencia_hasta INTEGER NOT NULL,
                reaplicacion_desde INTEGER NOT NULL)"""
        )
        db.execSQL("CREATE INDEX idx_apl_lote ON aplicaciones(lote_id, fecha)")
        db.execSQL("CREATE INDEX idx_lote_prod ON lotes(productor_id)")
    }

    private fun crearProductores(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE productores(
                id TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                comunidad TEXT NOT NULL DEFAULT '',
                telefono TEXT NOT NULL DEFAULT '',
                pin_hash TEXT NOT NULL,
                creado INTEGER NOT NULL)"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            crearProductores(db)
            db.execSQL("ALTER TABLE lotes ADD COLUMN productor_id TEXT")
            db.execSQL("CREATE INDEX idx_lote_prod ON lotes(productor_id)")
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    // ---------- Productores ----------

    fun crearProductor(p: Productor) {
        val cv = ContentValues().apply {
            put("id", p.id); put("nombre", p.nombre); put("comunidad", p.comunidad)
            put("telefono", p.telefono); put("pin_hash", p.pinHash); put("creado", p.creado)
        }
        writableDatabase.insertOrThrow("productores", null, cv)
    }

    fun productor(id: String): Productor? =
        readableDatabase.rawQuery("SELECT * FROM productores WHERE id = ?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) c.toProductor() else null
        }

    fun productores(): List<Productor> =
        readableDatabase.rawQuery("SELECT * FROM productores ORDER BY nombre COLLATE NOCASE", null).use { c ->
            buildList { while (c.moveToNext()) add(c.toProductor()) }
        }

    fun productoresPorNombre(nombre: String): List<Productor> =
        readableDatabase.rawQuery("SELECT * FROM productores WHERE TRIM(nombre) = TRIM(?) COLLATE NOCASE", arrayOf(nombre)).use { c ->
            buildList { while (c.moveToNext()) add(c.toProductor()) }
        }

    // ---------- Lotes ----------

    /**
     * Registra el lote del QR o actualiza sus datos si ya existía (los registros se conservan).
     * Si el lote no tiene productor y se indica [dueno], queda vinculado a él.
     */
    fun guardarLoteDesdeQr(q: QrLote, dueno: Productor?): Lote? {
        val existente = lote(q.id)
        if (!q.completo) {
            if (existente != null && existente.productorId == null && dueno != null) asignarProductor(existente.id, dueno)
            return lote(q.id)
        }
        val lote = Lote(
            id = q.id,
            nombre = q.nombre ?: existente?.nombre ?: q.id,
            cultivoId = q.cultivoId!!,
            superficie = q.superficie!!,
            productor = q.productor ?: existente?.productor?.ifBlank { null } ?: dueno?.nombre ?: "",
            ubicacion = q.ubicacion ?: existente?.ubicacion ?: "",
            creado = existente?.creado ?: System.currentTimeMillis(),
            productorId = existente?.productorId ?: dueno?.id
        )
        guardarLote(lote)
        return lote
    }

    fun guardarLote(lote: Lote) {
        val cv = ContentValues().apply {
            put("id", lote.id); put("nombre", lote.nombre); put("cultivo_id", lote.cultivoId)
            put("superficie", lote.superficie); put("productor", lote.productor)
            put("ubicacion", lote.ubicacion); put("creado", lote.creado); put("productor_id", lote.productorId)
        }
        writableDatabase.insertWithOnConflict("lotes", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun asignarProductor(loteId: String, p: Productor) {
        val cv = ContentValues().apply { put("productor_id", p.id); put("productor", p.nombre) }
        writableDatabase.update("lotes", cv, "id = ?", arrayOf(loteId))
    }

    fun lote(id: String): Lote? =
        readableDatabase.rawQuery("SELECT * FROM lotes WHERE id = ?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) c.toLote() else null
        }

    fun lotesDe(productorId: String): List<Lote> =
        readableDatabase.rawQuery("SELECT * FROM lotes WHERE productor_id = ? ORDER BY creado DESC", arrayOf(productorId)).use { c ->
            buildList { while (c.moveToNext()) add(c.toLote()) }
        }

    fun existeId(tabla: String, id: String): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM $tabla WHERE id = ?", arrayOf(id)).use { it.moveToFirst() }

    // ---------- Aplicaciones ----------

    fun insertar(a: Aplicacion): Long {
        val cv = ContentValues().apply {
            put("lote_id", a.loteId); put("producto_id", a.productoId); put("cultivo_id", a.cultivoId)
            put("fecha", a.fecha); put("superficie", a.superficie); put("dosis_ha", a.dosisHa)
            put("cantidad_total", a.cantidadTotal); put("volumen_caldo_ha", a.volumenCaldoHa)
            put("capacidad_tanque", a.capacidadTanque); put("tanques", a.tanques)
            put("temperatura", a.temperatura); put("humedad", a.humedad); put("viento", a.viento)
            put("lluvia", if (a.lluvia) 1 else 0); put("epp", a.epp); put("aplicador", a.aplicador)
            put("plaga", a.plaga); put("observaciones", a.observaciones); put("estado", a.estado)
            put("advertencias", a.advertencias); put("reentrada_hasta", a.reentradaHasta)
            put("carencia_hasta", a.carenciaHasta); put("reaplicacion_desde", a.reaplicacionDesde)
        }
        return writableDatabase.insertOrThrow("aplicaciones", null, cv)
    }

    fun aplicaciones(loteId: String): List<Aplicacion> =
        readableDatabase.rawQuery("SELECT * FROM aplicaciones WHERE lote_id = ? ORDER BY fecha DESC", arrayOf(loteId)).use { c ->
            buildList { while (c.moveToNext()) add(c.toAplicacion()) }
        }

    fun aplicacion(id: Long): Aplicacion? =
        readableDatabase.rawQuery("SELECT * FROM aplicaciones WHERE id = ?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) c.toAplicacion() else null
        }

    /** Aplicaciones de los lotes de un productor a partir de [desde] (0 = todas). */
    fun contarAplicaciones(productorId: String, desde: Long = 0L): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM aplicaciones a JOIN lotes l ON l.id = a.lote_id WHERE l.productor_id = ? AND a.fecha >= ?",
            arrayOf(productorId, desde.toString())
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    // ---------- Mapeo ----------

    private fun Cursor.s(col: String) = getString(getColumnIndexOrThrow(col))
    private fun Cursor.sn(col: String): String? = getColumnIndexOrThrow(col).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.i(col: String) = getInt(getColumnIndexOrThrow(col))
    private fun Cursor.l(col: String) = getLong(getColumnIndexOrThrow(col))
    private fun Cursor.d(col: String) = getDouble(getColumnIndexOrThrow(col))

    private fun Cursor.toProductor() = Productor(
        id = s("id"), nombre = s("nombre"), comunidad = s("comunidad"), telefono = s("telefono"),
        pinHash = s("pin_hash"), creado = l("creado")
    )

    private fun Cursor.toLote() = Lote(
        id = s("id"), nombre = s("nombre"), cultivoId = i("cultivo_id"), superficie = d("superficie"),
        productor = s("productor"), ubicacion = s("ubicacion"), creado = l("creado"), productorId = sn("productor_id")
    )

    private fun Cursor.toAplicacion() = Aplicacion(
        id = l("id"), loteId = s("lote_id"), productoId = i("producto_id"), cultivoId = i("cultivo_id"),
        fecha = l("fecha"), superficie = d("superficie"), dosisHa = d("dosis_ha"), cantidadTotal = d("cantidad_total"),
        volumenCaldoHa = d("volumen_caldo_ha"), capacidadTanque = d("capacidad_tanque"), tanques = i("tanques"),
        temperatura = d("temperatura"), humedad = d("humedad"), viento = d("viento"), lluvia = i("lluvia") == 1,
        epp = s("epp"), aplicador = s("aplicador"), plaga = s("plaga"), observaciones = s("observaciones"),
        estado = s("estado"), advertencias = s("advertencias"), reentradaHasta = l("reentrada_hasta"),
        carenciaHasta = l("carencia_hasta"), reaplicacionDesde = l("reaplicacion_desde")
    )
}
