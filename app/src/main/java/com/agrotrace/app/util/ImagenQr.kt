package com.agrotrace.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.io.FileOutputStream

object ImagenQr {

    fun qr(texto: String, lado: Int = 720): Bitmap =
        BarcodeEncoder().encodeBitmap(texto, BarcodeFormat.QR_CODE, lado, lado)

    /** Etiqueta imprimible: encabezado, código QR y líneas de texto. */
    fun etiqueta(texto: String, titulo: String, lineas: List<String>): Bitmap {
        val ancho = 800
        val alto = 120 + 720 + 40 + lineas.size * 56 + 30
        val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val fondo = Paint().apply { color = Color.rgb(0x3B, 0x20, 0x0C) }
        c.drawRect(0f, 0f, ancho.toFloat(), 100f, fondo)
        val pTitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0xFF, 0xF6, 0xE8); textSize = 44f; typeface = Typeface.DEFAULT_BOLD
        }
        c.drawText(titulo, 36f, 66f, pTitulo)
        c.drawBitmap(qr(texto, 720), (ancho - 720) / 2f, 110f, null)
        var y = 110f + 720f + 60f
        lineas.forEachIndexed { i, l ->
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (i == 0) Color.rgb(0x12, 0x3F, 0x1C) else Color.rgb(0x5A, 0x34, 0x18)
                textSize = if (i == 0) 46f else 34f
                typeface = if (i == 0) Typeface.MONOSPACE else Typeface.DEFAULT
                isFakeBoldText = i == 0
                textAlign = Paint.Align.CENTER
            }
            c.drawText(l.take(40), ancho / 2f, y, p)
            y += 56f
        }
        return bmp
    }

    fun compartir(context: Context, bmp: Bitmap, nombreArchivo: String, asunto: String) {
        val dir = File(context.cacheDir, "reportes").apply { mkdirs() }
        val archivo = File(dir, nombreArchivo)
        FileOutputStream(archivo).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir o imprimir código QR"))
    }
}
