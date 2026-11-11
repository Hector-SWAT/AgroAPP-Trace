package com.agrotrace.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agrotrace.app.data.Catalogo
import com.agrotrace.app.data.Categoria
import com.agrotrace.app.data.Producto

/** Filtro de texto: nombre, clase química, formulación o plaga objetivo. */
fun Producto.coincide(q: String): Boolean {
    if (q.isBlank()) return true
    val t = q.trim().lowercase()
    return nombre.lowercase().contains(t) || clase.lowercase().contains(t) ||
        formulacion.lowercase().contains(t) || usos.any { it.lowercase().contains(t) }
}

@Composable
fun FiltroCategorias(seleccion: Categoria?, disponibles: List<Categoria>, onCambio: (Categoria?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = seleccion == null, onClick = { onCambio(null) }, label = { Text("Todas") })
        disponibles.forEach { c ->
            FilterChip(selected = seleccion == c, onClick = { onCambio(c) }, label = { Text(c.etiqueta) })
        }
    }
}

@Composable
fun FichaProducto(p: Producto) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EtiquetaToxicidad(p.toxicidad)
            if (p.restringido) Etiqueta("Uso restringido", RojoClaro, Rojo)
        }
        Text("${p.formulacion} · ${p.clase} · ${p.categorias.joinToString { it.etiqueta }}", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth()) {
            Dato("Dosis (ref.)", "${p.dosisMin}–${p.dosisMax} ${p.unidad.principal}/ha", Modifier.weight(1f))
            Dato("Carencia", "${p.carenciaDias} días", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            Dato("Reentrada", "${p.reentradaHoras} h", Modifier.weight(1f))
            Dato("Reaplicar cada", "${p.reaplicacionDias} días (máx. ${p.maxAplicacionesCiclo}/ciclo)", Modifier.weight(1f))
        }
        Text("LMR: ${p.lmr} mg/kg", style = MaterialTheme.typography.bodySmall)
        Text("Usos: " + p.usos.joinToString(), style = MaterialTheme.typography.bodySmall)
        Text("EPP obligatorio: " + p.epp.joinToString { it.etiqueta }, style = MaterialTheme.typography.bodySmall)
        if (p.restricciones.isNotEmpty())
            Text("Restricciones: " + p.restricciones.joinToString("; "), style = MaterialTheme.typography.bodySmall, color = Rojo)
        val cultivos = p.cultivosPermitidos.mapNotNull { Catalogo.cultivo(it)?.nombre }
        Text("Cultivos: " + cultivos.joinToString(), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CatalogoScreen(modifier: Modifier = Modifier) {
    var categoria by rememberSaveable { mutableStateOf<Categoria?>(null) }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var abierto by rememberSaveable { mutableStateOf(-1) }
    val lista = Catalogo.productos.filter { (categoria == null || categoria in it.categorias) && it.coincide(busqueda) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = busqueda, onValueChange = { busqueda = it },
                label = { Text("Buscar por nombre, clase o plaga") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { FiltroCategorias(categoria, Categoria.entries) { categoria = it } }
        item {
            Text(
                "Valores de referencia del prototipo. Siempre confirme con la etiqueta autorizada del producto.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(lista, key = { it.id }) { p ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { abierto = if (abierto == p.id) -1 else p.id },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (abierto == p.id) FichaProducto(p)
                    else Text("${p.formulacion} · ${p.categorias.joinToString { it.etiqueta }} · toxicidad ${p.toxicidad.etiqueta.lowercase()}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
