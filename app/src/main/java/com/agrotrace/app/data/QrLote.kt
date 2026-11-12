package com.agrotrace.app.data

import java.net.URLDecoder

/**
 * Datos de un lote contenidos en el código QR generado por la web.
 *
 * Formato:  agrotrace://lote?v=1&id=LT-2611-4821&nom=Parcela%20Norte&cul=3&ha=2.5&prod=Juan%20P%C3%A9rez&ubi=Zacapoaxtla
 * También se acepta solo el identificador (p. ej. "LT-2611-4821") para abrir un lote ya registrado.
 */
data class QrLote(
    val id: String,
    val nombre: String?,
    val cultivoId: Int?,
    val superficie: Double?,
    val productor: String?,
    val ubicacion: String?
) {
    /** true si trae los datos necesarios para registrar el lote por primera vez. */
    val completo: Boolean get() = cultivoId != null && Catalogo.cultivo(cultivoId) != null && superficie != null && superficie > 0

    companion object {
        private val ID_REGEX = Regex("^[A-Z0-9][A-Z0-9-]{2,31}$")
        const val ESQUEMA = "agrotrace://lote"

        fun parse(texto: String): QrLote? {
            val t = texto.trim()
            if (t.isEmpty()) return null
            if (!t.lowercase().startsWith(ESQUEMA)) {
                val id = t.uppercase()
                return if (ID_REGEX.matches(id)) QrLote(id, null, null, null, null, null) else null
            }
            val q = t.substringAfter('?', "")
            val params = q.split('&').filter { it.contains('=') }.associate {
                val k = it.substringBefore('=')
                val v = URLDecoder.decode(it.substringAfter('='), "UTF-8")
                k to v
            }
            val id = params["id"]?.trim()?.uppercase() ?: return null
            if (!ID_REGEX.matches(id)) return null
            return QrLote(
                id = id,
                nombre = params["nom"]?.trim()?.take(60)?.ifBlank { null },
                cultivoId = params["cul"]?.toIntOrNull(),
                superficie = params["ha"]?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it > 0 && it < 100000 },
                productor = params["prod"]?.trim()?.take(60)?.ifBlank { null },
                ubicacion = params["ubi"]?.trim()?.take(80)?.ifBlank { null }
            )
        }
    }
}

/** Credencial QR del productor: agrotrace://productor?v=1&id=PR-2611-1234&nom=Juan%20P%C3%A9rez */
data class QrProductor(val id: String, val nombre: String?) {
    companion object {
        const val ESQUEMA = "agrotrace://productor"
        private val ID_REGEX = Regex("^PR-[A-Z0-9-]{2,29}$")

        fun parse(texto: String): QrProductor? {
            val t = texto.trim()
            if (!t.lowercase().startsWith(ESQUEMA)) {
                val id = t.uppercase()
                return if (ID_REGEX.matches(id)) QrProductor(id, null) else null
            }
            val params = t.substringAfter('?', "").split('&').filter { it.contains('=') }.associate {
                it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), "UTF-8")
            }
            val id = params["id"]?.trim()?.uppercase() ?: return null
            if (!ID_REGEX.matches(id)) return null
            return QrProductor(id, params["nom"]?.trim()?.take(60)?.ifBlank { null })
        }
    }
}

/** Construye el texto de los códigos QR (mismo formato que la web AgroTrace QR). */
object TextoQr {
    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    fun lote(l: Lote): String {
        val ha = (Math.round(l.superficie * 100.0) / 100.0).toString().removeSuffix(".0")
        val p = mutableListOf("v=1", "id=" + enc(l.id), "nom=" + enc(l.nombre), "cul=" + l.cultivoId, "ha=$ha")
        if (l.productor.isNotBlank()) p += "prod=" + enc(l.productor)
        if (l.ubicacion.isNotBlank()) p += "ubi=" + enc(l.ubicacion)
        return QrLote.ESQUEMA + "?" + p.joinToString("&")
    }

    fun productor(p: Productor): String = QrProductor.ESQUEMA + "?v=1&id=" + enc(p.id) + "&nom=" + enc(p.nombre)

    /** Identificador corto: PREFIJO-AAMM-#### */
    fun nuevoId(prefijo: String, existe: (String) -> Boolean): String {
        val f = java.text.SimpleDateFormat("yyMM", java.util.Locale.US).format(java.util.Date())
        var id: String
        do { id = "$prefijo-$f-" + (1000 + java.util.Random().nextInt(9000)) } while (existe(id))
        return id
    }
}
