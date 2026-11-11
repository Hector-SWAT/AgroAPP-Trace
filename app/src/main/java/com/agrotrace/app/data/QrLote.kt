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
