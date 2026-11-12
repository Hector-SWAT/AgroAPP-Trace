package com.agrotrace.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agrotrace.app.domain.Nivel
import com.agrotrace.app.util.Formato
import com.agrotrace.app.util.Reporte

@Composable
private fun Cargando(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun DetalleLoteScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val lote = vm.lote
    val pantalla = vm.actual
    if (lote == null || (pantalla is Pantalla.DetalleLote && pantalla.loteId != lote.id)) { Cargando(modifier); return }
    val contexto = LocalContext.current
    val ahora = System.currentTimeMillis()
    val reentrada = vm.historial.maxOfOrNull { it.reentradaHasta }?.takeIf { it > ahora }
    val carencia = vm.historial.maxOfOrNull { it.carenciaHasta }?.takeIf { it > ahora }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Seccion("Lote ${lote.id}") {
                Row(Modifier.fillMaxWidth()) {
                    Dato("Cultivo", lote.cultivo?.nombre ?: "?", Modifier.weight(1f))
                    Dato("Superficie", "${Formato.numero(lote.superficie)} ha", Modifier.weight(1f))
                }
                if (lote.productor.isNotBlank() || lote.ubicacion.isNotBlank()) Row(Modifier.fillMaxWidth()) {
                    Dato("Productor", lote.productor.ifBlank { "—" }, Modifier.weight(1f))
                    Dato("Ubicación", lote.ubicacion.ifBlank { "—" }, Modifier.weight(1f))
                }
                if (reentrada != null) LineaNivel(Nivel.BLOQUEO, "No reingresar sin EPP hasta ${Formato.fechaHora(reentrada)}")
                if (carencia != null) LineaNivel(Nivel.AVISO, "Cosecha permitida a partir del ${Formato.fecha(carencia)}")
                if (reentrada == null && carencia == null) LineaNivel(Nivel.OK, "Sin restricciones activas de reentrada ni de cosecha")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonBrillo(onClick = { vm.ir(Pantalla.NuevaAplicacion(lote.id)) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Nueva aplicación")
                }
                BotonBrillo(
                    cafe = true,
                    onClick = { Reporte.compartir(contexto, lote, vm.historial) },
                    enabled = vm.historial.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Reporte CSV")
                }
            }
            Spacer(Modifier.height(12.dp))
            BotonBrillo(onClick = { vm.ir(Pantalla.QrDeLote(lote.id)) }, cafe = true, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.QrCode, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Ver código QR del lote")
            }
        }
        item { Text("Historial (${vm.historial.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if (vm.historial.isEmpty()) item {
            Text("Este lote aún no tiene aplicaciones registradas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(vm.historial, key = { it.id }) { a ->
            val p = a.producto
            Card(
                modifier = Modifier.fillMaxWidth().clickable { vm.ir(Pantalla.DetalleAplicacion(a.id)) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p?.nombre ?: "Producto ${a.productoId}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(Formato.fechaHora(a.fecha), style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        "${Formato.numero(a.superficie)} ha · " +
                            (p?.let { Formato.cantidad(a.cantidadTotal, it.unidad) } ?: Formato.numero(a.cantidadTotal)) +
                            " · ${a.plaga.ifBlank { "sin objetivo" }}"
                    )
                    if (a.estado == "AVISO") Etiqueta("Aplicada con advertencias", AmbarClaro, MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun DetalleAplicacionScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val a = vm.aplicacion
    val pantalla = vm.actual
    if (a == null || (pantalla is Pantalla.DetalleAplicacion && pantalla.aplicacionId != a.id)) { Cargando(modifier); return }
    val p = a.producto
    val u = p?.unidad

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Seccion(p?.nombre ?: "Producto ${a.productoId}") {
            if (p != null) Text("${p.formulacion} · ${p.clase}", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth()) {
                Dato("Lote", a.loteId, Modifier.weight(1f))
                Dato("Fecha", Formato.fechaHora(a.fecha), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                Dato("Objetivo", a.plaga.ifBlank { "—" }, Modifier.weight(1f))
                Dato("Aplicador", a.aplicador, Modifier.weight(1f))
            }
        }
        Seccion("Cantidades") {
            Row(Modifier.fillMaxWidth()) {
                Dato("Dosis", if (u != null) Formato.cantidad(a.dosisHa, u) + "/ha" else Formato.numero(a.dosisHa), Modifier.weight(1f))
                Dato("Superficie", "${Formato.numero(a.superficie)} ha", Modifier.weight(1f))
            }
            Dato("Producto total", if (u != null) Formato.cantidad(a.cantidadTotal, u) else Formato.numero(a.cantidadTotal))
            if (a.tanques > 0) {
                Text("Caldo: ${Formato.numero(a.volumenCaldoHa, 0)} L/ha · ${a.tanques} tanque(s) de ${Formato.numero(a.capacidadTanque, 0)} L")
            }
        }
        Seccion("Fechas de seguridad") {
            LineaNivel(Nivel.BLOQUEO, "Reentrada al lote: ${Formato.fechaHora(a.reentradaHasta)}")
            LineaNivel(Nivel.AVISO, "Cosecha permitida desde: ${Formato.fecha(a.carenciaHasta)}")
            LineaNivel(Nivel.OK, "Puede repetir este producto desde: ${Formato.fecha(a.reaplicacionDesde)}")
        }
        Seccion("Condiciones y seguridad") {
            Text("Temperatura ${Formato.numero(a.temperatura, 1)} °C · Humedad ${Formato.numero(a.humedad, 0)} % · Viento ${Formato.numero(a.viento, 1)} km/h")
            Text("Lluvia prevista: " + if (a.lluvia) "Sí" else "No")
            Text("EPP utilizado: ${a.epp}")
            if (a.advertencias.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                a.advertencias.lines().filter { it.isNotBlank() }.forEach { LineaNivel(Nivel.AVISO, it) }
            }
        }
        if (a.observaciones.isNotBlank()) Seccion("Observaciones") { Text(a.observaciones) }
    }
}
