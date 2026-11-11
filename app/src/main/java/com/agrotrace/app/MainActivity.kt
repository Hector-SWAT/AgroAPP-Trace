package com.agrotrace.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.agrotrace.app.ui.AgroTheme
import com.agrotrace.app.ui.AppViewModel
import com.agrotrace.app.ui.CatalogoScreen
import com.agrotrace.app.ui.DetalleAplicacionScreen
import com.agrotrace.app.ui.DetalleLoteScreen
import com.agrotrace.app.ui.InicioScreen
import com.agrotrace.app.ui.NuevaAplicacionScreen
import com.agrotrace.app.ui.Pantalla
import com.agrotrace.app.ui.CafeOscuro
import com.agrotrace.app.ui.Crema
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Si la app se abrió desde un enlace agrotrace://lote?... (QR leído con la cámara del teléfono)
        if (savedInstanceState == null) intent?.dataString?.let { vm.procesarQr(it) }
        setContent { AgroTheme { AppRoot(vm) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let { vm.procesarQr(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: AppViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val escaner = rememberLauncherForActivityResult(ScanContract()) { r ->
        r.contents?.let { vm.procesarQr(it) }
    }
    val escanear = {
        escaner.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Apunte al código QR del lote")
                .setBeepEnabled(true)
                .setOrientationLocked(false)
        )
    }

    LaunchedEffect(vm.mensaje) {
        vm.mensaje?.let { snackbar.showSnackbar(it); vm.mensaje = null }
    }
    BackHandler(enabled = vm.pila.size > 1) { vm.atras() }

    val pantalla = vm.actual
    val titulo = when (pantalla) {
        is Pantalla.Inicio -> "AgroTrace"
        is Pantalla.Catalogo -> "Catálogo de productos"
        is Pantalla.DetalleLote -> vm.lote?.nombre ?: pantalla.loteId
        is Pantalla.NuevaAplicacion -> "Nueva aplicación"
        is Pantalla.DetalleAplicacion -> "Registro de aplicación"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    if (vm.pila.size > 1) {
                        IconButton(onClick = { vm.atras() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CafeOscuro, titleContentColor = Crema, navigationIconContentColor = Crema
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val m = Modifier.padding(padding)
        when (pantalla) {
            is Pantalla.Inicio -> InicioScreen(vm, escanear, m)
            is Pantalla.Catalogo -> CatalogoScreen(m)
            is Pantalla.DetalleLote -> DetalleLoteScreen(vm, m)
            is Pantalla.NuevaAplicacion -> NuevaAplicacionScreen(vm, m)
            is Pantalla.DetalleAplicacion -> DetalleAplicacionScreen(vm, m)
        }
    }
}
