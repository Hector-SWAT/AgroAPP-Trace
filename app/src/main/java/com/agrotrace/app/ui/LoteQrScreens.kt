package com.agrotrace.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agrotrace.app.data.Catalogo
import com.agrotrace.app.data.TextoQr
import com.agrotrace.app.util.Formato
import com.agrotrace.app.util.ImagenQr

@Composable
fun ImagenCodigoQr(texto: String, modifier: Modifier = Modifier) {
    val bmp = remember(texto) { ImagenQr.qr(texto, 720).asImageBitmap() }
    Image(bitmap = bmp, contentDescription = "Código QR", modifier = modifier)
}

@Composable
fun NuevoLoteScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var cultivoId by rememberSaveable { mutableStateOf(0) }
    var superficieTxt by rememberSaveable { mutableStateOf("") }
    var ubicacion by rememberSaveable { mutableStateOf("") }
    val superficie = superficieTxt.aNumero()
    val valido = nombre.isNotBlank() && Catalogo.cultivo(cultivoId) != null && superficie != null && superficie > 0 && superficie < 100000

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Seccion("Datos del lote") {
            OutlinedTextField(
                value = nombre, onValueChange = { nombre = it.take(60) }, singleLine = true,
                label = { Text("Nombre del lote o parcela *") }, modifier = Modifier.fillMaxWidth()
            )
            Text("Cultivo *", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Catalogo.cultivos.forEach { c ->
                    FilterChip(selected = cultivoId == c.id, onClick = { cultivoId = c.id }, label = { Text(c.nombre) })
                }
            }
            CampoNumero("Superficie *", superficieTxt, { superficieTxt = it }, Modifier.fillMaxWidth(), "ha",
                error = superficieTxt.isNotEmpty() && (superficie == null || superficie <= 0))
            OutlinedTextField(
                value = ubicacion, onValueChange = { ubicacion = it.take(80) }, singleLine = true,
                label = { Text("Ubicación (opcional)") }, modifier = Modifier.fillMaxWidth()
            )
        }
        BotonBrillo(
            onClick = { vm.crearLote(nombre, cultivoId, superficie ?: 0.0, ubicacion) },
            enabled = valido, modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Crear lote y generar su QR") }
    }
}

@Composable
private fun PantallaQr(
    titulo: String, id: String, lineas: List<String>, texto: String, nota: String,
    archivo: String, modifier: Modifier
) {
    val contexto = LocalContext.current
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Seccion(titulo) {
            ImagenCodigoQr(texto, Modifier.fillMaxWidth().padding(8.dp))
            Text(id, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = VerdeOscuro,
                style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            lineas.forEach {
                Text(it, color = Cafe, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        Text(nota, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        BotonBrillo(
            onClick = { ImagenQr.compartir(contexto, ImagenQr.etiqueta(texto, titulo, listOf(id) + lineas), archivo, titulo) },
            cafe = true, modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Compartir / imprimir QR")
        }
    }
}

@Composable
fun QrDeLoteScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val l = vm.lote
    val p = vm.actual
    if (l == null || (p is Pantalla.QrDeLote && p.loteId != l.id)) { Text("Cargando…", modifier.padding(16.dp)); return }
    PantallaQr(
        titulo = "AgroTrace · Lote", id = l.id,
        lineas = listOf(l.nombre, "${l.cultivo?.nombre ?: "?"} · ${Formato.numero(l.superficie)} ha"),
        texto = TextoQr.lote(l),
        nota = "Imprime esta etiqueta y colócala en el lote. Al escanearla se abren sus registros y la sesión de su productor.",
        archivo = "etiqueta_${l.id}.png", modifier = modifier
    )
}

@Composable
fun CredencialScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val p = vm.productor ?: return
    PantallaQr(
        titulo = "AgroTrace · Productor", id = p.id,
        lineas = listOfNotNull(p.nombre, p.comunidad.ifBlank { null }),
        texto = TextoQr.productor(p),
        nota = "Escanea esta credencial en la pantalla de inicio para entrar sin escribir tu nombre ni tu PIN.",
        archivo = "credencial_${p.id}.png", modifier = modifier
    )
}
