package com.agrotrace.app.data

/*
 * Catálogo de productos y cultivos.
 *
 * IMPORTANTE: los valores numéricos (dosis, carencia, reentrada, intervalos) son
 * DATOS DE REFERENCIA DEL PROTOTIPO, derivados del catálogo original del proyecto web.
 * No sustituyen la etiqueta autorizada del producto comercial ni el registro vigente.
 */

enum class Categoria(val etiqueta: String) {
    INSECTICIDA("Insecticida"),
    FUNGICIDA("Fungicida"),
    BACTERICIDA("Bactericida"),
    HERBICIDA("Herbicida"),
    ACARICIDA("Acaricida"),
    NEMATICIDA("Nematicida")
}

enum class Toxicidad(val etiqueta: String, val nivel: Int) {
    BAJA("Baja", 1), MEDIA("Media", 2), ALTA("Alta", 3)
}

enum class Epp(val etiqueta: String) {
    GUANTES("Guantes"),
    MASCARILLA("Mascarilla / respirador"),
    OVEROL("Overol"),
    GAFAS("Gafas / careta"),
    BOTAS("Botas de hule")
}

/** Unidad del producto comercial: litros (líquidos) o kilogramos (polvos/granulados). */
enum class Unidad(val principal: String, val sub: String) {
    LITRO("L", "mL"), KILO("kg", "g")
}

data class Producto(
    val id: Int,
    val nombre: String,              // ingrediente activo
    val formulacion: String,         // presentación / concentración
    val categorias: List<Categoria>,
    val clase: String,               // grupo químico
    val toxicidad: Toxicidad,
    val dosisMin: Double,            // por hectárea, en [unidad]
    val dosisMax: Double,
    val unidad: Unidad,
    val carenciaDias: Int,           // intervalo de seguridad: última aplicación -> cosecha
    val reentradaHoras: Int,         // tiempo antes de reingresar al lote sin EPP
    val reaplicacionDias: Int,       // intervalo mínimo entre aplicaciones del mismo producto
    val maxAplicacionesCiclo: Int,
    val lmr: Double,                 // mg/kg (ppm)
    val epp: Set<Epp>,
    val cultivosPermitidos: Set<Int>,
    val usos: List<String>,
    val restricciones: List<String> = emptyList(),
    val tempMax: Double? = null,     // °C máxima recomendada para este producto
    val vientoMax: Double? = null,   // km/h máxima específica (productos sensibles a deriva)
    val enCaldo: Boolean = true,     // false = granulado aplicado directo al suelo
    val restringido: Boolean = false // uso restringido/prohibido en varios países
)

data class Cultivo(
    val id: Int,
    val nombre: String,
    val familia: String,
    val tipo: String,
    val cicloDias: Int,
    val soloBajaToxicidad: Boolean = false,
    val notas: String = ""
) {
    /** Volumen de caldo sugerido (L/ha). Editable por el usuario. */
    val volumenCaldoSugerido: Double get() = if (tipo == "Frutal") 1000.0 else 200.0
}

object Catalogo {
    private val G = Epp.GUANTES
    private val M = Epp.MASCARILLA
    private val O = Epp.OVEROL
    private val A = Epp.GAFAS
    private val B = Epp.BOTAS
    private val HORT_FRUT = setOf(1, 6, 7, 8, 9, 10, 11, 12, 13, 14)
    private val FRUTALES = setOf(11, 12, 13, 14)

    val cultivos: List<Cultivo> = listOf(
        Cultivo(1, "Tomate", "Solanaceae", "Hortaliza", 120),
        Cultivo(2, "Trigo", "Poaceae", "Grano", 180),
        Cultivo(3, "Maíz", "Poaceae", "Grano", 120),
        Cultivo(4, "Soya", "Fabaceae", "Grano", 110),
        Cultivo(5, "Arroz", "Poaceae", "Grano", 130, notas = "Cuidado con herbicidas en etapas tempranas"),
        Cultivo(6, "Papa", "Solanaceae", "Tubérculo", 100, notas = "Prioridad fungicidas para tizón"),
        Cultivo(7, "Cebolla", "Amaryllidaceae", "Hortaliza", 120, soloBajaToxicidad = true),
        Cultivo(8, "Zanahoria", "Apiaceae", "Hortaliza", 80, soloBajaToxicidad = true),
        Cultivo(9, "Lechuga", "Asteraceae", "Hortaliza", 60, soloBajaToxicidad = true),
        Cultivo(10, "Brócoli", "Brassicaceae", "Hortaliza", 75, notas = "Cuidado con residuos"),
        Cultivo(11, "Naranjo", "Rutaceae", "Frutal", 365),
        Cultivo(12, "Aguacate", "Lauraceae", "Frutal", 365, notas = "Ácaros: problema principal"),
        Cultivo(13, "Café", "Rubiaceae", "Frutal", 365, notas = "Roya: problema principal"),
        Cultivo(14, "Plátano", "Musaceae", "Frutal", 365, notas = "Nematodos frecuentes")
    )

    val productos: List<Producto> = listOf(
        Producto(1, "Imidacloprid", "SC 350 g/L", listOf(Categoria.INSECTICIDA), "Neonicotinoide", Toxicidad.ALTA,
            0.15, 0.25, Unidad.LITRO, 15, 48, 14, 2, 0.5, setOf(G, M, O, A), setOf(1, 3, 4, 6, 11, 14),
            listOf("Pulgón", "Mosca blanca", "Trips"),
            listOf("No aplicar en floración (tóxico para abejas)", "No aplicar cerca de cuerpos de agua")),
        Producto(2, "Mancozeb", "WP 80%", listOf(Categoria.FUNGICIDA), "Ditiocarbamato", Toxicidad.MEDIA,
            1.5, 2.5, Unidad.KILO, 7, 24, 7, 4, 0.2, setOf(G, M), setOf(1, 3, 4, 6, 7, 8, 9, 10),
            listOf("Tizón tardío", "Antracnosis", "Mildiu"),
            listOf("No mezclar con aceites"), tempMax = 30.0),
        Producto(3, "Lambda cihalotrina", "CE 50 g/L", listOf(Categoria.INSECTICIDA), "Piretroide", Toxicidad.MEDIA,
            0.04, 0.06, Unidad.LITRO, 14, 24, 10, 3, 0.1, setOf(G, M), setOf(1, 2, 3, 4, 5, 6, 7),
            listOf("Gusano ejército", "Palomilla", "Trips"),
            listOf("Muy sensible a deriva"), vientoMax = 10.0),
        Producto(4, "Clorpirifos", "CE 480 g/L", listOf(Categoria.INSECTICIDA), "Organofosforado", Toxicidad.ALTA,
            1.0, 1.5, Unidad.LITRO, 21, 48, 14, 2, 0.05, setOf(G, M, O, A, B), setOf(2, 3, 5),
            listOf("Gusano cogollero", "Gusano elotero", "Chapulín"),
            listOf("No usar en hortalizas", "No aplicar en floración"), restringido = true),
        Producto(5, "Abamectina", "CE 18 g/L", listOf(Categoria.INSECTICIDA, Categoria.ACARICIDA), "Avermectina", Toxicidad.MEDIA,
            0.4, 0.6, Unidad.LITRO, 7, 12, 7, 3, 0.01, setOf(G, M), HORT_FRUT,
            listOf("Ácaros", "Minadores", "Trips"), tempMax = 35.0),
        Producto(6, "Spinosad", "SC 120 g/L", listOf(Categoria.INSECTICIDA), "Espinosina", Toxicidad.BAJA,
            0.2, 0.4, Unidad.LITRO, 3, 4, 7, 3, 0.02, setOf(G), HORT_FRUT,
            listOf("Gusano soldado", "Palomilla", "Trips"),
            listOf("Origen biológico: aplicar en horas frescas")),
        Producto(7, "Oxicloruro de cobre", "WP 50%", listOf(Categoria.FUNGICIDA, Categoria.BACTERICIDA), "Cúprico", Toxicidad.BAJA,
            2.0, 3.5, Unidad.KILO, 7, 24, 7, 5, 5.0, setOf(G, M), HORT_FRUT,
            listOf("Bacteriosis", "Antracnosis", "Mildiu"),
            listOf("No mezclar con fosfitos")),
        Producto(8, "Azoxistrobina", "SC 250 g/L", listOf(Categoria.FUNGICIDA), "Estrobilurina", Toxicidad.BAJA,
            0.4, 0.6, Unidad.LITRO, 14, 4, 10, 2, 0.2, setOf(G), setOf(1, 2, 3, 4, 5, 6, 11, 13),
            listOf("Roya", "Mancha foliar", "Tizón"),
            listOf("Alternar con fungicidas de otro modo de acción")),
        Producto(9, "Tebuconazol", "CE 250 g/L", listOf(Categoria.FUNGICIDA), "Triazol", Toxicidad.MEDIA,
            0.5, 1.0, Unidad.LITRO, 21, 24, 14, 2, 0.05, setOf(G, M), setOf(1, 2, 3, 4, 5, 6, 11, 13),
            listOf("Roya", "Oídio", "Fusarium"),
            listOf("No usar en etapas tempranas")),
        Producto(10, "Glifosato", "SL 360 g/L", listOf(Categoria.HERBICIDA), "Organofosfonato", Toxicidad.BAJA,
            2.0, 4.0, Unidad.LITRO, 7, 4, 30, 2, 0.1, setOf(G, M), setOf(2, 3, 4, 5),
            listOf("Malezas anuales", "Malezas perennes"),
            listOf("Usar con surfactante", "Solo pre-siembra o aplicación dirigida"), vientoMax = 10.0),
        Producto(11, "2,4-D", "SL 480 g/L", listOf(Categoria.HERBICIDA), "Fenoxiacético", Toxicidad.MEDIA,
            1.0, 2.0, Unidad.LITRO, 30, 48, 30, 1, 0.05, setOf(G, M, A), setOf(2, 3, 5),
            listOf("Malezas de hoja ancha"),
            listOf("Solo post-emergencia temprana", "Muy sensible a deriva: lejos de hortalizas"),
            tempMax = 30.0, vientoMax = 10.0),
        Producto(12, "Atrazina", "SC 500 g/L", listOf(Categoria.HERBICIDA), "Triazina", Toxicidad.ALTA,
            1.5, 2.5, Unidad.LITRO, 90, 24, 60, 1, 0.1, setOf(G, M, O, A), setOf(3),
            listOf("Malezas pre-emergencia"),
            listOf("Solo en maíz", "No aplicar cerca de agua ni en suelos arenosos"), restringido = true),
        Producto(13, "Paraquat", "SL 200 g/L", listOf(Categoria.HERBICIDA), "Bipiridilo", Toxicidad.ALTA,
            1.5, 2.5, Unidad.LITRO, 7, 48, 30, 2, 0.01, setOf(G, M, O, A, B), setOf(2, 3, 4, 5),
            listOf("Desecante", "Control rápido"),
            listOf("Solo personal certificado", "Extrema precaución"), restringido = true),
        Producto(14, "Cletodim", "CE 240 g/L", listOf(Categoria.HERBICIDA), "Ciclohexanodiona", Toxicidad.MEDIA,
            0.5, 1.0, Unidad.LITRO, 30, 24, 30, 2, 0.05, setOf(G, M), setOf(1, 4, 6, 7, 8, 9, 10),
            listOf("Gramíneas en cultivos de hoja ancha")),
        Producto(15, "Hexitiazox", "SC 100 g/L", listOf(Categoria.ACARICIDA), "Tiazolidinona", Toxicidad.BAJA,
            0.2, 0.4, Unidad.LITRO, 14, 12, 30, 1, 0.5, setOf(G), FRUTALES,
            listOf("Araña roja", "Tetranychus")),
        Producto(16, "Espirodiclofén", "SC 240 g/L", listOf(Categoria.ACARICIDA), "Ácido tetrónico", Toxicidad.MEDIA,
            0.35, 0.5, Unidad.LITRO, 21, 12, 30, 1, 0.2, setOf(G, M), FRUTALES,
            listOf("Ácaros de amplio espectro"),
            listOf("Rotar con otros acaricidas")),
        Producto(17, "Azufre", "WP 80%", listOf(Categoria.ACARICIDA, Categoria.FUNGICIDA), "Inorgánico", Toxicidad.BAJA,
            3.0, 5.0, Unidad.KILO, 1, 24, 7, 6, 50.0, setOf(G, M), HORT_FRUT,
            listOf("Oídio", "Ácaros", "Cochinilla"), tempMax = 30.0),
        Producto(18, "Oxamil", "SL 240 g/L", listOf(Categoria.NEMATICIDA, Categoria.INSECTICIDA), "Carbamato", Toxicidad.ALTA,
            10.0, 15.0, Unidad.LITRO, 60, 48, 30, 2, 0.01, setOf(G, M, O, A, B), setOf(1, 6, 7, 8, 9, 10, 11, 14),
            listOf("Nematodos del suelo"),
            listOf("Aplicación al suelo"), restringido = true),
        Producto(19, "Fenamifós", "GR 10%", listOf(Categoria.NEMATICIDA), "Organofosforado", Toxicidad.ALTA,
            15.0, 20.0, Unidad.KILO, 120, 48, 90, 1, 0.01, setOf(G, M, O, A, B), FRUTALES,
            listOf("Nematodos e insectos del suelo"),
            listOf("Granulado: aplicar al suelo e incorporar"), enCaldo = false, restringido = true),
        Producto(20, "Metalaxil", "CE 250 g/L", listOf(Categoria.FUNGICIDA), "Acilalanina", Toxicidad.MEDIA,
            1.0, 2.0, Unidad.LITRO, 14, 24, 14, 3, 0.05, setOf(G, M), setOf(1, 6, 11, 14),
            listOf("Mildiu", "Phytophthora"),
            listOf("Alternar con fungicidas de otro modo de acción"))
    )

    fun cultivo(id: Int): Cultivo? = cultivos.firstOrNull { it.id == id }
    fun producto(id: Int): Producto? = productos.firstOrNull { it.id == id }

    /** Productos autorizados para el cultivo (incluye la regla de "solo baja toxicidad"). */
    fun productosPara(cultivo: Cultivo, categoria: Categoria? = null): List<Producto> =
        productos.filter { p ->
            cultivo.id in p.cultivosPermitidos &&
                (!cultivo.soloBajaToxicidad || p.toxicidad == Toxicidad.BAJA) &&
                (categoria == null || categoria in p.categorias)
        }.sortedWith(compareBy({ it.toxicidad.nivel }, { it.nombre }))
}
