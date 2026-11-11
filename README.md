# AgroTrace Móvil + AgroTrace QR

Versión Android de **AgroTracebility** con lectura de **código QR** en lugar del NFC simulado.

| Parte | Qué es | Dónde |
|---|---|---|
| **AgroTrace QR** (web) | Genera la etiqueta QR de cada lote (ID, nombre, cultivo, superficie, productor, ubicación). | `web-qr/index.html` (un solo archivo, funciona sin internet; se puede subir a GitHub Pages) |
| **AgroTrace** (app Android) | Escanea el QR, abre el lote y sus **registros guardados**, calcula y registra aplicaciones. | carpeta raíz (proyecto de Android Studio) |

## Cómo compilar la app
1. Abrir la carpeta `AgroTraceMovil` en **Android Studio** (Ladybug o más reciente). JDK 17+.
2. Esperar la sincronización de Gradle (descarga AGP 8.7.3, Kotlin 2.1.0, Compose).
3. `Run ▶` en un teléfono (Android 7.0 / API 24 o superior) o generar el APK en
   **Build → Build App Bundle(s) / APK(s) → Build APK(s)** → `app/build/outputs/apk/debug/app-debug.apk`.
4. Pruebas de la lógica: `./gradlew test` (o clic derecho en `EvaluadorTest` → Run).

### Opción sin Android Studio: GitHub Actions
1. Sube **todo el contenido** de la carpeta `AgroTraceMovil` (incluida la carpeta oculta `.github`) a un repositorio de GitHub.
2. Entra a la pestaña **Actions** del repositorio → *Compilar APK*. Se ejecuta solo en cada push (o con **Run workflow**).
3. Al terminar (≈ 5–8 min), abre la ejecución y descarga **AgroTrace-apk** en *Artifacts*: dentro está `app-debug.apk`.
4. Si la ejecución sale en rojo, abre el paso *Compilar APK de depuración* y copia el error.

## Flujo de uso
1. En la web, llene los datos del lote → descargue/imprima la etiqueta y péguela en el lote.
2. En la app: **Escanear QR del lote** → la primera vez registra el lote; las siguientes abre su historial.
   - También funciona leyendo el QR con la cámara normal del teléfono (enlace `agrotrace://lote?...`).
   - En el emulador: en la web pulse **Copiar contenido** y en la app use **Ingresar código**.
3. **Nueva aplicación** → producto (solo los autorizados para el cultivo) → plaga y nivel de infestación →
   mezcla por tanque → clima → EPP → responsable → resultado (APTO / PRECAUCIÓN / NO APLICAR) → **Registrar**.
4. **Reporte CSV** del lote (se abre en Excel) para compartir por WhatsApp, correo, etc.

Formato del QR: `agrotrace://lote?v=1&id=LT-2611-4821&nom=Parcela%20Norte&cul=3&ha=2.5&prod=...&ubi=...`
(`cul` = ID del cultivo, igual en la web y en `Catalogo.kt`).

## Errores del proyecto web original que se corrigieron
| Problema en la web | Corrección en la app |
|---|---|
| `storage.js` no se cargaba en `index.html`: **nada se guardaba** y el historial decía "en desarrollo". | Base de datos SQLite en el teléfono (`lotes` y `aplicaciones`) e historial por lote. |
| NFC simulado con **datos aleatorios**. | QR real generado por la web con los datos del lote. |
| La dosis se **multiplicaba** por factores de cultivo y clima (hasta ~2× la dosis base en lechuga con frío/seco). | La dosis **nunca sale del rango de etiqueta**; se elige por nivel de infestación. El clima decide *si* se aplica, no *cuánto*. |
| Viento > 15 km/h daba dosis **0** y aun así decía "Cálculo validado". | Bloquea con "NO APLICAR" (y límite de 10 km/h en productos sensibles a deriva). |
| No verificaba si el producto estaba autorizado para el cultivo (p. ej. Atrazina en lechuga). | Solo se listan productos autorizados; regla "solo baja toxicidad" aplicada de verdad. |
| EPP faltante solo mostraba aviso. | EPP obligatorio: sin él no se puede registrar. |
| Carencia "ajustada" con factores inventados y usada como "próxima aplicación". | Se separan **reentrada** (horas), **carencia/cosecha** (días) y **reaplicación** (días) + máximo de aplicaciones por ciclo, validado con el historial del lote. |
| `temp = 0` se tomaba como vacío (`|| 25`). | Validación numérica y de rangos. |
| No decía cuánto producto poner en la bomba. | Mezcla por tanque (mochila 15/20 L o tambo 200 L), incluido el último tanque parcial. |
| Clic duplicado en sugerencias de autocompletado; iconos de Font Awesome inexistentes. | Interfaz nativa en Jetpack Compose. |
| Sin "Bactericida", "ingrediente activo / presentación" ni reporte (pizarrón de clase). | Categoría Bactericida, formulación de cada producto y reporte CSV. |

## Aviso
Los valores del catálogo (dosis, carencia, reentrada, intervalos, cultivos permitidos) son **de referencia del prototipo**,
basados en el catálogo original. No están validados contra etiquetas registradas: antes de un uso real deben revisarse
con la etiqueta autorizada de cada producto y su registro vigente (COFEPRIS/SENASICA).
