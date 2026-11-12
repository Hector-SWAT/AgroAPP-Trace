package com.agrotrace.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DialogoCodigo(onCerrar: () -> Unit, onAbrir: (String) -> Unit) {
    var texto by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Ingresar código") },
        text = {
            OutlinedTextField(
                value = texto, onValueChange = { texto = it }, singleLine = true,
                label = { Text("ID (LT-… / PR-…) o contenido del QR") }
            )
        },
        confirmButton = { TextButton(onClick = { onCerrar(); onAbrir(texto) }, enabled = texto.isNotBlank()) { Text("Abrir") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

@Composable
fun BienvenidaScreen(vm: AppViewModel, escanear: () -> Unit, modifier: Modifier = Modifier) {
    var dialogo by remember { mutableStateOf(false) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.size(104.dp).background(Brush.verticalGradient(listOf(VerdeBrillo, Verde, VerdeOscuro)), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Agriculture, contentDescription = null, tint = Crema, modifier = Modifier.size(60.dp)) }
        Text("AgroTrace", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Cafe)
        Text(
            "Registro y cálculo seguro de aplicaciones de plaguicidas por lote",
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        vm.lotePendiente?.let { q ->
            Card(colors = CardDefaults.cardColors(containerColor = AmbarClaro), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Lote ${q.id} detectado. Inicia sesión o regístrate para vincularlo a tu cuenta.",
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        BotonBrillo(onClick = escanear, modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Entrar con código QR", style = MaterialTheme.typography.titleMedium)
        }
        BotonBrillo(onClick = { vm.ir(Pantalla.Login) }, cafe = true, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Icon(Icons.Filled.Person, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Iniciar sesión con mis datos")
        }
        BotonBrillo(onClick = { vm.ir(Pantalla.Registro()) }, cafe = true, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Nuevo registro")
        }
        TextButton(onClick = { dialogo = true }) { Text("Ingresar código manualmente") }
        Text(
            "El QR puede ser la etiqueta de un lote o la credencial del productor.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (dialogo) DialogoCodigo(onCerrar = { dialogo = false }, onAbrir = { vm.procesarQr(it) })
}

@Composable
fun LoginScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Seccion("Iniciar sesión") {
            OutlinedTextField(
                value = nombre, onValueChange = { nombre = it.take(60) }, singleLine = true,
                label = { Text("Nombre del productor") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pin, onValueChange = { v -> pin = v.filter { it.isDigit() }.take(6) }, singleLine = true,
                label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            if (vm.productoresRegistrados.isNotEmpty()) {
                Text("Productores registrados en este teléfono:", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    vm.productoresRegistrados.forEach { p -> AssistChip(onClick = { nombre = p.nombre }, label = { Text(p.nombre) }) }
                }
            }
        }
        BotonBrillo(
            onClick = { vm.login(nombre, pin) },
            enabled = nombre.isNotBlank() && pin.length >= 4,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Entrar") }
        TextButton(onClick = { vm.ir(Pantalla.Registro(nombre)) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("¿No tienes cuenta? Crear nuevo registro")
        }
    }
}

@Composable
fun RegistroScreen(vm: AppViewModel, pantalla: Pantalla.Registro, modifier: Modifier = Modifier) {
    var nombre by rememberSaveable { mutableStateOf(pantalla.nombre) }
    var comunidad by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var pin2 by rememberSaveable { mutableStateOf("") }
    val pinValido = pin.length in 4..6
    val coinciden = pin == pin2
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Seccion("Datos del productor") {
            if (pantalla.idFijo != null)
                Text("Se usará el ID de la credencial escaneada: ${pantalla.idFijo}", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = nombre, onValueChange = { nombre = it.take(60) }, singleLine = true,
                label = { Text("Nombre completo *") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = comunidad, onValueChange = { comunidad = it.take(80) }, singleLine = true,
                label = { Text("Comunidad / municipio") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = telefono, onValueChange = { v -> telefono = v.filter { it.isDigit() || it == ' ' || it == '+' }.take(15) },
                singleLine = true, label = { Text("Teléfono") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
            )
        }
        Seccion("PIN de acceso") {
            Text("De 4 a 6 números. Lo usarás para entrar con tu nombre.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = pin, onValueChange = { v -> pin = v.filter { it.isDigit() }.take(6) }, singleLine = true,
                label = { Text("PIN *") }, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = pin.isNotEmpty() && !pinValido, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pin2, onValueChange = { v -> pin2 = v.filter { it.isDigit() }.take(6) }, singleLine = true,
                label = { Text("Confirmar PIN *") }, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = pin2.isNotEmpty() && !coinciden, modifier = Modifier.fillMaxWidth()
            )
            if (pin2.isNotEmpty() && !coinciden) Text("Los PIN no coinciden.", color = Rojo, style = MaterialTheme.typography.bodySmall)
        }
        BotonBrillo(
            onClick = { vm.registrarProductor(nombre, comunidad, telefono, pin, pantalla.idFijo) },
            enabled = nombre.isNotBlank() && pinValido && coinciden,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Crear registro y entrar") }
    }
}
