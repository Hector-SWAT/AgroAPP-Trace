package com.agrotrace.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agrotrace.app.data.Aplicacion
import com.agrotrace.app.data.Catalogo
import com.agrotrace.app.data.Categoria
import com.agrotrace.app.data.Epp
import com.agrotrace.app.domain.Condiciones
import com.agrotrace.app.domain.Entrada
import com.agrotrace.app.domain.Evaluador
import com.agrotrace.app.domain.Nivel
import com.agrotrace.app.util.Formato

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaAplicacionScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val lote = vm.lote
    val pantalla = vm.actual
    val cultivo = lote?.cultivo
    if (lote == null || cultivo == null || (pantalla is Pantalla.NuevaAplicacion && pantalla.loteId != lote.id)) {
        Text("Cargando lote…", modifier.padding(16.dp)); return
    }

    // ----- Estado del formulario (sobrevive a la rotación) -----
    var categoria by rememberSaveable { mutableStateOf<Categoria?>(null) }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var productoId by rememberSaveable { mutableStateOf(-1) }
    var superficieTxt by rememberSaveable { mutableStateOf(Formato.numero(lote.superficie).replace(',', '.')) }
    var plaga by rememberSaveable { mutableStateOf("") }
    var presion by rememberSaveable { mutableStateOf(1) }
    var dosis by rememberSaveable { mutableStateOf(0.0) }
    var volumenTxt by rememberSaveable { mutableStateOf(cultivo.volumenCaldoSugerido.toInt().toString()) }
    var tanqueTxt by rememberSaveable { mutableStateOf("20") }
    var tempTxt by rememberSaveable { mutableStateOf("") }
    var humTxt by rememberSaveable { mutableStateOf("") }
    var vientoTxt by rememberSaveable { mutableStateOf("") }
    var lluvia by rememberSaveable { mutableStateOf(false) }
    var eppTxt by rememberSaveable { mutableStateOf("") } // nombres de Epp separados por coma
    var aplicador by rememberSaveable { mutableStateOf(vm.aplicadorPredeterminado) }
    var observaciones by rememberSaveable { mutableStateOf("") }
    val fecha = remember { System.currentTimeMillis() }

    val epp: Set<Epp> = eppTxt.split(',').filter { it.isNotBlank() }.mapNotNull { n -> Epp.entries.firstOrNull { it.name == n } }.toSet()
    val disponibles = Catalogo.productosPara(cultivo)
    val categorias = Categoria.entries.filter { c -> disponibles.any { c in it.categorias } }
    val lista = disponibles.filter { (categoria == null || categoria in it.categorias) && it.coincide(busqueda) }
    val producto = Catalogo.producto(productoId)?.takeIf { it in disponibles }

    val superficie = superficieTxt.aNumero()
    val temp = tempTxt.aNumero()
    val hum = humTxt.aNumero()
    val viento = vientoTxt.aNumero()
    val volumen = volumenTxt.aNumero()
    val tanque = tanqueTxt.aNumero()
    val climaCompleto = temp != null && hum != null && viento != null

    val resultado = if (producto != null && superficie != null && climaCompleto) {
        Evaluador.evaluar(
            Entrada(
                producto = producto, cultivo = cultivo, superficieLote = lote.superficie, superficie = superficie,
                dosisHa = dosis, condiciones = Condiciones(temp!!, hum!!, viento!!, lluvia), eppMarcado = epp,
                volumenCaldoHa = volumen ?: 0.0, capacidadTanque = tanque ?: 0.0,
                fechaMillis = fecha, historial = vm.historialPrevio()
            )
        )
    } else null

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Lote
        Seccion("1. Lote ${lote.id}") {
            Text("${lote.nombre} · ${cultivo.nombre} (${cultivo.tipo}) · ${Formato.numero(lote.superficie)} ha")
            if (cultivo.soloBajaToxicidad) LineaNivel(Nivel.AVISO, "${cultivo.nombre}: solo se muestran productos de baja toxicidad.")
            CampoNumero(
                "Superficie a tratar", superficieTxt, { superficieTxt = it }, Modifier.fillMaxWidth(), "ha",
                error = superficie == null || superficie <= 0 || superficie > lote.superficie
            )
        }

        // 2. Producto
        Seccion("2. Producto") {
            if (producto == null) {
                OutlinedTextField(
                    value = busqueda, onValueChange = { busqueda = it }, singleLine = true,
                    label = { Text("Buscar producto o plaga") }, modifier = Modifier.fillMaxWidth()
                )
                FiltroCategorias(categoria, categorias) { categoria = it }
                Text("${lista.size} producto(s) autorizados para ${cultivo.nombre}", style = MaterialTheme.typography.bodySmall)
                lista.forEach { p ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            productoId = p.id
                            dosis = Evaluador.dosisPorPresion(p, presion)
                            plaga = if (plaga.isBlank() && p.usos.size == 1) p.usos.first() else plaga
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(p.nombre, fontWeight = FontWeight.SemiBold)
                            Text("${p.formulacion} · ${p.usos.joinToString()}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(producto.nombre, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { productoId = -1 }) { Text("Cambiar") }
                }
                FichaProducto(producto)
            }
        }

        if (producto != null) {
            // 3. Objetivo y dosis
            Seccion("3. Plaga objetivo y dosis") {
                OutlinedTextField(
                    value = plaga, onValueChange = { plaga = it.take(60) }, singleLine = true,
                    label = { Text("Plaga, enfermedad o maleza") }, modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    producto.usos.forEach { u -> AssistChip(onClick = { plaga = u }, label = { Text(u) }) }
                }
                Text("Nivel de infestación", style = MaterialTheme.typography.labelLarge)
                val opciones = listOf("Baja", "Media", "Alta")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    opciones.forEachIndexed { i, t ->
                        SegmentedButton(
                            selected = presion == i,
                            onClick = { presion = i; dosis = Evaluador.dosisPorPresion(producto, i) },
                            shape = SegmentedButtonDefaults.itemShape(i, opciones.size)
                        ) { Text(t) }
                    }
                }
                Text(
                    "Dosis: ${Formato.cantidad(dosis, producto.unidad)}/ha  (rango ${producto.dosisMin}–${producto.dosisMax} ${producto.unidad.principal}/ha)",
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = dosis.toFloat().coerceIn(producto.dosisMin.toFloat(), producto.dosisMax.toFloat()),
                    onValueChange = { dosis = Math.round(it * 1000.0) / 1000.0 },
                    valueRange = producto.dosisMin.toFloat()..producto.dosisMax.toFloat()
                )
            }

            // 4. Preparación del caldo
            if (producto.enCaldo) {
                Seccion("4. Preparación de la mezcla") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoNumero("Caldo por hectárea", volumenTxt, { volumenTxt = it }, Modifier.weight(1f), "L/ha",
                            error = (volumen ?: 0.0) <= 0)
                        CampoNumero("Capacidad del tanque", tanqueTxt, { tanqueTxt = it }, Modifier.weight(1f), "L",
                            error = (tanque ?: 0.0) <= 0)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("15", "20", "200").forEach { c ->
                            AssistChip(onClick = { tanqueTxt = c }, label = { Text(if (c == "200") "Tambo 200 L" else "Mochila $c L") })
                        }
                    }
                }
            } else {
                Seccion("4. Forma de aplicación") {
                    Text("Producto granulado: se aplica directamente al suelo, sin preparar caldo.")
                }
            }

            // 5. Clima
            Seccion("5. Condiciones del clima") {
                Text(
                    "Mida en el lote justo antes de aplicar (los sensores no están conectados).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoNumero("Temperatura", tempTxt, { tempTxt = it }, Modifier.weight(1f), "°C")
                    CampoNumero("Humedad", humTxt, { humTxt = it }, Modifier.weight(1f), "%")
                }
                CampoNumero("Velocidad del viento", vientoTxt, { vientoTxt = it }, Modifier.fillMaxWidth(), "km/h")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { lluvia = !lluvia }) {
                    Checkbox(checked = lluvia, onCheckedChange = { lluvia = it })
                    Text("Se espera lluvia en las próximas horas")
                }
            }

            // 6. EPP
            Seccion("6. Equipo de protección personal") {
                Epp.entries.forEach { e ->
                    val obligatorio = e in producto.epp
                    val marcado = e in epp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            val nuevo = if (marcado) epp - e else epp + e
                            eppTxt = nuevo.joinToString(",") { it.name }
                        }
                    ) {
                        Checkbox(checked = marcado, onCheckedChange = { c ->
                            val nuevo = if (c) epp + e else epp - e
                            eppTxt = nuevo.joinToString(",") { it.name }
                        })
                        Text(e.etiqueta, modifier = Modifier.weight(1f))
                        if (obligatorio) Text("Obligatorio", style = MaterialTheme.typography.labelSmall,
                            color = if (marcado) Verde else Rojo)
                    }
                }
            }

            // 7. Responsable
            Seccion("7. Responsable") {
                OutlinedTextField(
                    value = aplicador, onValueChange = { aplicador = it.take(60) }, singleLine = true,
                    label = { Text("Nombre del aplicador") }, isError = aplicador.isBlank(), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = observaciones, onValueChange = { observaciones = it.take(500) },
                    label = { Text("Observaciones (opcional)") }, minLines = 2, modifier = Modifier.fillMaxWidth()
                )
            }

            // 8. Resultado
            ResultadoCard(
                producto = producto,
                resultado = resultado,
                climaCompleto = climaCompleto,
                superficieValida = superficie != null
            )

            val puedeGuardar = resultado != null && resultado.puedeRegistrar && aplicador.isNotBlank()
            BotonBrillo(
                onClick = {
                    val r = resultado ?: return@BotonBrillo
                    vm.registrar(
                        Aplicacion(
                            loteId = lote.id, productoId = producto.id, cultivoId = cultivo.id,
                            fecha = System.currentTimeMillis(), superficie = superficie!!, dosisHa = r.dosisHa,
                            cantidadTotal = r.cantidadTotal,
                            volumenCaldoHa = if (producto.enCaldo) volumen ?: 0.0 else 0.0,
                            capacidadTanque = if (producto.enCaldo) tanque ?: 0.0 else 0.0,
                            tanques = r.mezcla?.tanquesTotales ?: 0,
                            temperatura = temp!!, humedad = hum!!, viento = viento!!, lluvia = lluvia,
                            epp = epp.joinToString { it.etiqueta }, aplicador = aplicador.trim(),
                            plaga = plaga.trim(), observaciones = observaciones.trim(),
                            estado = if (r.estado == Nivel.AVISO) "AVISO" else "OK",
                            advertencias = r.hallazgos.filter { it.nivel == Nivel.AVISO }.joinToString("\n") { it.texto },
                            reentradaHasta = r.reentradaHasta, carenciaHasta = r.carenciaHasta,
                            reaplicacionDesde = r.reaplicacionDesde
                        )
                    )
                },
                enabled = puedeGuardar,
                modifier = Modifier.fillMaxWidth().height(58.dp)
            ) { Text("Registrar aplicación") }
            if (!puedeGuardar && resultado != null && resultado.puedeRegistrar && aplicador.isBlank())
                Text("Escriba el nombre del aplicador para registrar.", color = Rojo, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResultadoCard(
    producto: com.agrotrace.app.data.Producto,
    resultado: com.agrotrace.app.domain.Resultado?,
    climaCompleto: Boolean,
    superficieValida: Boolean
) {
    if (resultado == null) {
        Seccion("Resultado") {
            Text(
                when {
                    !superficieValida -> "Indique una superficie válida."
                    !climaCompleto -> "Capture temperatura, humedad y viento para evaluar la aplicación."
                    else -> "Complete los datos."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    val (titulo, fondo, color) = when (resultado.estado) {
        Nivel.OK -> Triple("APTO PARA APLICAR", VerdeClaro, Verde)
        Nivel.AVISO -> Triple("APLICAR CON PRECAUCIÓN", AmbarClaro, Color(0xFF8D6E00))
        Nivel.BLOQUEO -> Triple("NO APLICAR", RojoClaro, Rojo)
    }
    val u = producto.unidad
    Card(colors = CardDefaults.cardColors(containerColor = fondo), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            resultado.hallazgos.sortedByDescending { it.nivel.ordinal }.forEach { LineaNivel(it.nivel, it.texto) }
            if (resultado.hallazgos.isEmpty()) LineaNivel(Nivel.OK, "Todas las verificaciones se cumplen.")
            HorizontalDivider()
            Row(Modifier.fillMaxWidth()) {
                Dato("Dosis", Formato.cantidad(resultado.dosisHa, u) + "/ha", Modifier.weight(1f))
                Dato("Producto total", Formato.cantidad(resultado.cantidadTotal, u), Modifier.weight(1f))
            }
            resultado.mezcla?.let { m ->
                Text("Cómo preparar la mezcla", fontWeight = FontWeight.SemiBold)
                Text("Caldo total: ${Formato.numero(m.caldoTotalL, 0)} L en ${m.tanquesTotales} tanque(s).")
                if (m.tanquesCompletos > 0)
                    Text("• ${m.tanquesCompletos} tanque(s) llenos: ${Formato.cantidad(m.productoPorTanque, u)} de producto en cada uno.")
                if (m.caldoUltimoTanque > 0)
                    Text("• Último tanque con ${Formato.numero(m.caldoUltimoTanque, 1)} L de agua: ${Formato.cantidad(m.productoUltimoTanque, u)} de producto.")
            }
            HorizontalDivider()
            Text("Reentrada al lote: ${Formato.fechaHora(resultado.reentradaHasta)}")
            Text("Cosecha permitida desde: ${Formato.fecha(resultado.carenciaHasta)}")
            Text("Repetir este producto desde: ${Formato.fecha(resultado.reaplicacionDesde)}")
            Text(
                "Cálculo con valores de referencia. Confirme dosis e intervalos en la etiqueta del producto.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
