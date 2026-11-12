package com.agrotrace.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agrotrace.app.util.Formato

@Composable
fun InicioScreen(vm: AppViewModel, escanear: () -> Unit, modifier: Modifier = Modifier) {
    var dialogo by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Hola, ${vm.productor?.nombre ?: ""}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = VerdeOscuro)
                    Text("${vm.productor?.id ?: ""}${vm.productor?.comunidad?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Dato("Lotes", vm.resumenes.size.toString())
                        Dato("Aplicaciones", vm.totalAplicaciones.toString())
                        Dato("Hoy", vm.aplicacionesHoy.toString())
                    }
                }
            }
        }
        item {
            BotonBrillo(onClick = escanear, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Escanear QR del lote")
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonBrillo(onClick = { vm.ir(Pantalla.NuevoLote) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Nuevo lote")
                }
                BotonBrillo(onClick = { vm.ir(Pantalla.Credencial) }, cafe = true, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Badge, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Mi QR")
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonBrillo(onClick = { dialogo = true }, cafe = true, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Código")
                }
                BotonBrillo(onClick = { vm.ir(Pantalla.Catalogo) }, cafe = true, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Science, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Catálogo")
                }
            }
        }
        item {
            Text("Mis lotes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (vm.resumenes.isEmpty()) {
            item {
                Text(
                    "Aún no tienes lotes. Escanea la etiqueta QR de un lote (generada en la web) o crea uno con «Nuevo lote».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(vm.resumenes, key = { it.lote.id }) { r ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { vm.ir(Pantalla.DetalleLote(r.lote.id)) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.lote.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(r.lote.id, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${r.lote.cultivo?.nombre ?: "?"} · ${Formato.numero(r.lote.superficie)} ha · ${r.aplicaciones} aplicación(es)")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        r.reentradaHasta?.let { Etiqueta("No reingresar hasta ${Formato.fechaHora(it)}", RojoClaro, Rojo) }
                        r.carenciaHasta?.let { Etiqueta("Cosecha desde ${Formato.fecha(it)}", AmbarClaro, MaterialTheme.colorScheme.onSurface) }
                    }
                }
            }
        }
    }

    if (dialogo) DialogoCodigo(onCerrar = { dialogo = false }, onAbrir = { vm.procesarQr(it) })
}
