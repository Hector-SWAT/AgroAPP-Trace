package com.agrotrace.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Lote(
    val id: String,
    val nombre: String,
    val cultivoId: Int,
    val superficie: Double,
    val productor: String,
    val ubicacion: String,
    val creado: Long
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
class AgroDb(context: Context) : SQLiteOpenHelper(context, "agrotrace.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE lotes(
                id TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                cultivo_id INTEGER NOT NULL,
                superficie REAL NOT NULL,
                productor TEXT NOT NULL DEFAULT '',
                ubicacion TEXT NOT NULL DEFAULT '',
                creado INTEGER NOT NULL)"""
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versión 1: sin migraciones todavía.
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    // ---------- Lotes ----------

    /** Registra el lote del QR o actualiza sus datos si ya existía (los registros se conservan). */
    fun guardarLoteDesdeQr(q: QrLote): Lote? {
        val existente = lote(q.id)
        if (!q.completo) return existente
        val lote = Lote(
            id = q.id,
            nombre = q.nombre ?: existente?.nombre ?: q.id,
            cultivoId = q.cultivoId!!,
            superficie = q.superficie!!,
            productor = q.productor ?: existente?.productor ?: "",
            ubicacion = q.ubicacion ?: existente?.ubicacion ?: "",
            creado = existente?.creado ?: System.currentTimeMillis()
        )
        val cv = ContentValues().apply {
            put("id", lote.id); put("nombre", lote.nombre); put("cultivo_id", lote.cultivoId)
            put("superficie", lote.superficie); put("productor", lote.productor)
            put("ubicacion", lote.ubicacion); put("creado", lote.creado)
        }
        writableDatabase.insertWithOnConflict("lotes", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        return lote
    }

    fun lote(id: String): Lote? =
        readableDatabase.rawQuery("SELECT * FROM lotes WHERE id = ?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) c.toLote() else null
        }

    fun lotes(): List<Lote> =
        readableDatabase.rawQuery("SELECT * FROM lotes ORDER BY creado DESC", null).use { c ->
            buildList { while (c.moveToNext()) add(c.toLote()) }
        }

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

    fun totalAplicaciones(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM aplicaciones", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun aplicacionesDesde(millis: Long): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM aplicaciones WHERE fecha >= ?", arrayOf(millis.toString())).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }

    // ---------- Mapeo ----------

    private fun Cursor.s(col: String) = getString(getColumnIndexOrThrow(col))
    private fun Cursor.i(col: String) = getInt(getColumnIndexOrThrow(col))
    private fun Cursor.l(col: String) = getLong(getColumnIndexOrThrow(col))
    private fun Cursor.d(col: String) = getDouble(getColumnIndexOrThrow(col))

    private fun Cursor.toLote() = Lote(
        id = s("id"), nombre = s("nombre"), cultivoId = i("cultivo_id"), superficie = d("superficie"),
        productor = s("productor"), ubicacion = s("ubicacion"), creado = l("creado")
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
