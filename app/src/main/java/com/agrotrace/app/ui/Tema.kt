package com.agrotrace.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.agrotrace.app.data.Toxicidad
import com.agrotrace.app.domain.Nivel

// Paleta tierra: café oscuro y verde oscuro, saturados
val Verde = Color(0xFF1E6B2E)
val VerdeBrillo = Color(0xFF2F9142)
val VerdeOscuro = Color(0xFF123F1C)
val VerdeClaro = Color(0xFFE4F2DF)
val Cafe = Color(0xFF5A3418)
val CafeBrillo = Color(0xFF8A5328)
val CafeOscuro = Color(0xFF3B200C)
val Crema = Color(0xFFFFF6E8)
val Ambar = Color(0xFFF9A825)
val AmbarClaro = Color(0xFFFFF8E1)
val Rojo = Color(0xFFB3261E)
val RojoClaro = Color(0xFFFFEBEE)

private val Esquema = lightColorScheme(
    primary = Verde,
    onPrimary = Crema,
    primaryContainer = Color(0xFFD3E8CC),
    onPrimaryContainer = VerdeOscuro,
    secondary = Cafe,
    onSecondary = Crema,
    secondaryContainer = Color(0xFFEADBC4),
    onSecondaryContainer = CafeOscuro,
    background = Color(0xFFF3EADB),
    onBackground = Color(0xFF2B1A0E),
    surface = Color(0xFFFFFBF4),
    onSurface = Color(0xFF2B1A0E),
    onSurfaceVariant = Color(0xFF6E5A47),
    outline = Color(0xFFBFA585),
    error = Rojo
)

@Composable
fun AgroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, content = content)
}

@Composable
fun Seccion(titulo: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Cafe)
            content()
        }
    }
}

@Composable
fun CampoNumero(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    sufijo: String? = null,
    error: Boolean = false
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo -> onCambio(nuevo.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }.take(8)) },
        label = { Text(etiqueta) },
        suffix = sufijo?.let { { Text(it) } },
        isError = error,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

/** Botón con degradado brillante. verde = acción principal; cafe = acción secundaria. */
@Composable
fun BotonBrillo(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cafe: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val forma = RoundedCornerShape(12.dp)
    val colores = if (cafe) listOf(CafeBrillo, Cafe, CafeOscuro) else listOf(VerdeBrillo, Verde, VerdeOscuro)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = forma,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent, contentColor = Crema,
            disabledContainerColor = Color.Transparent, disabledContentColor = Crema.copy(alpha = 0.75f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .shadow(if (enabled) 5.dp else 0.dp, forma)
            .background(Brush.verticalGradient(colores), forma, alpha = if (enabled) 1f else 0.4f)
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)), forma),
        content = content
    )
}

fun String.aNumero(): Double? = replace(',', '.').toDoubleOrNull()

@Composable
fun Etiqueta(texto: String, fondo: Color, color: Color) {
    Surface(color = fondo, shape = MaterialTheme.shapes.small) {
        Text(texto, color = color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun EtiquetaToxicidad(t: Toxicidad) = when (t) {
    Toxicidad.BAJA -> Etiqueta("Toxicidad baja", VerdeClaro, Verde)
    Toxicidad.MEDIA -> Etiqueta("Toxicidad media", AmbarClaro, Color(0xFF8D6E00))
    Toxicidad.ALTA -> Etiqueta("Toxicidad alta", RojoClaro, Rojo)
}

@Composable
fun LineaNivel(nivel: Nivel, texto: String) {
    val (icono, color) = when (nivel) {
        Nivel.OK -> Icons.Filled.CheckCircle to Verde
        Nivel.AVISO -> Icons.Filled.Warning to Ambar
        Nivel.BLOQUEO -> Icons.Filled.Block to Rojo
    }
    Row(verticalAlignment = Alignment.Top) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Dato(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
