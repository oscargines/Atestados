package com.oscar.atestados.viewModel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.oscar.atestados.data.CitacionDataProvider
import com.oscar.atestados.utils.HtmlParser
import com.oscar.atestados.utils.PDFLabelPrinterZebra
import com.oscar.atestados.utils.PdfToBitmapConverter
import com.oscar.atestados.utils.PDFA4Printer
import com.oscar.atestados.utils.PDFToBitmapPrinter
import com.oscar.atestados.utils.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "CitacionViewModel"
private const val PREFS_NAME = "CitacionPrefs"

class CitacionViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences: SharedPreferences
        get() = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Estados observables para los campos de la citación
    private val _provincia = MutableLiveData<String>("")
    val provincia: LiveData<String> get() = _provincia

    private val _localidad = MutableLiveData<String>("")
    val localidad: LiveData<String> get() = _localidad

    private val _juzgado = MutableLiveData<String>("")
    val juzgado: LiveData<String> get() = _juzgado

    private val _fechaInicio = MutableLiveData<String>("")
    val fechaInicio: LiveData<String> get() = _fechaInicio

    private val _hora = MutableLiveData<String>("")
    val hora: LiveData<String> get() = _hora

    private val _numeroDiligencias = MutableLiveData<String>("")
    val numeroDiligencias: LiveData<String> get() = _numeroDiligencias

    private val _fechaNotificacion = MutableLiveData<String>("")
    val fechaNotificacion: LiveData<String> get() = _fechaNotificacion

    private val _horaNotificacion = MutableLiveData<String>("")
    val horaNotificacion: LiveData<String> get() = _horaNotificacion

    private val _abogadoNombre = MutableLiveData<String>("")
    val abogadoNombre: LiveData<String> get() = _abogadoNombre

    private val _abogadoColegiado = MutableLiveData<String>("")
    val abogadoColegiado: LiveData<String> get() = _abogadoColegiado

    private val _abogadoColegio = MutableLiveData<String>("")
    val abogadoColegio: LiveData<String> get() = _abogadoColegio

    private val _comunicacionNumero = MutableLiveData<String>("")
    val comunicacionNumero: LiveData<String> get() = _comunicacionNumero

    private val _abogadoDesignado = MutableLiveData<Boolean>(false)
    val abogadoDesignado: LiveData<Boolean> get() = _abogadoDesignado

    private val _abogadoOficio = MutableLiveData<Boolean>(false)
    val abogadoOficio: LiveData<Boolean> get() = _abogadoOficio

    // Estados para la UI
    private val _printStatus = MutableStateFlow("")
    val printStatus: StateFlow<String> = _printStatus.asStateFlow()

    private val _showPreviewDialog = MutableStateFlow(false)
    val showPreviewDialog: StateFlow<Boolean> = _showPreviewDialog.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _missingFields = MutableStateFlow<List<String>>(emptyList())
    val missingFields: StateFlow<List<String>> = _missingFields.asStateFlow()

    fun loadData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Cargando datos desde SharedPreferences")
            try {
                with(sharedPreferences) {
                    _provincia.postValue(getString("provincia", "") ?: "")
                    _localidad.postValue(getString("localidad", "") ?: "")
                    _juzgado.postValue(getString("juzgado", "") ?: "")
                    _fechaInicio.postValue(getString("fechaInicio", "") ?: "")
                    _hora.postValue(getString("hora", "") ?: "")
                    _numeroDiligencias.postValue(getString("numeroDiligencias", "") ?: "")
                    _fechaNotificacion.postValue(getString("fechaNotificacion", "") ?: "")
                    _horaNotificacion.postValue(getString("horaNotificacion", "") ?: "")
                    _abogadoNombre.postValue(getString("abogadoNombre", "") ?: "")
                    _abogadoColegiado.postValue(getString("abogadoColegiado", "") ?: "")
                    _abogadoColegio.postValue(getString("abogadoColegio", "") ?: "")
                    _comunicacionNumero.postValue(getString("comunicacionNumero", "") ?: "")
                    _abogadoDesignado.postValue(getBoolean("abogadoDesignado", false))
                    _abogadoOficio.postValue(getBoolean("abogadoOficio", false))
                }
                Log.d(TAG, "Datos cargados correctamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar datos: ${e.message}", e)
            }
        }
    }

    fun guardarDatos(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Guardando datos en SharedPreferences")
            try {
                with(sharedPreferences.edit()) {
                    putString("provincia", provincia.value ?: "")
                    putString("localidad", localidad.value ?: "")
                    putString("juzgado", juzgado.value ?: "")
                    putString("fechaInicio", fechaInicio.value ?: "")
                    putString("hora", hora.value ?: "")
                    putString("numeroDiligencias", numeroDiligencias.value ?: "")
                    putString("fechaNotificacion", fechaNotificacion.value ?: "")
                    putString("horaNotificacion", horaNotificacion.value ?: "")
                    putString("abogadoNombre", abogadoNombre.value ?: "")
                    putString("abogadoColegiado", abogadoColegiado.value ?: "")
                    putString("abogadoColegio", abogadoColegio.value ?: "")
                    putString("comunicacionNumero", comunicacionNumero.value ?: "")
                    putBoolean("abogadoDesignado", abogadoDesignado.value ?: false)
                    putBoolean("abogadoOficio", abogadoOficio.value ?: false)
                    apply()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar datos: ${e.message}", e)
            }
        }
    }

    fun limpiarDatos(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Limpiando datos")
            try {
                _provincia.postValue("")
                _localidad.postValue("")
                _juzgado.postValue("")
                _fechaInicio.postValue("")
                _hora.postValue("")
                _numeroDiligencias.postValue("")
                _fechaNotificacion.postValue("")
                _horaNotificacion.postValue("")
                _abogadoNombre.postValue("")
                _abogadoColegiado.postValue("")
                _abogadoColegio.postValue("")
                _comunicacionNumero.postValue("")
                _abogadoDesignado.postValue(false)
                _abogadoOficio.postValue(false)

                with(sharedPreferences.edit()) {
                    clear()
                    apply()
                }
                Log.d(TAG, "Datos limpiados correctamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al limpiar datos: ${e.message}", e)
            }
        }
    }

    fun updateProvincia(value: String) {
        _provincia.value = value
    }

    fun updateLocalidad(value: String) {
        _localidad.value = value
    }

    fun updateJuzgado(value: String) {
        _juzgado.value = value
    }

    fun updateFechaInicio(value: String) {
        _fechaInicio.value = value
    }

    fun updateHora(value: String) {
        _hora.value = value
    }

    fun updateNumeroDiligencias(value: String) {
        _numeroDiligencias.value = value
    }

    fun updateFechaNotificacion(value: String) {
        _fechaNotificacion.value = value
    }

    fun updateHoraNotificacion(value: String) {
        _horaNotificacion.value = value
    }

    fun updateAbogadoNombre(value: String) {
        _abogadoNombre.value = value
    }

    fun updateAbogadoColegiado(value: String) {
        _abogadoColegiado.value = value
    }

    fun updateAbogadoColegio(value: String) {
        _abogadoColegio.value = value
    }

    fun updateComunicacionNumero(value: String) {
        _comunicacionNumero.value = value
    }

    fun updateAbogadoSelection(designado: Boolean, oficio: Boolean) {
        if (designado && oficio) {
            _abogadoDesignado.value = true
            _abogadoOficio.value = false
        } else {
            _abogadoDesignado.value = designado
            _abogadoOficio.value = oficio
        }
    }

    // Métodos para actualizar estados de StateFlow
    fun updateMissingFields(fields: List<String>) {
        _missingFields.value = fields
    }

    fun updateShowPreviewDialog(show: Boolean) {
        _showPreviewDialog.value = show
    }

    fun updatePreviewBitmap(bitmap: Bitmap?) {
        _previewBitmap.value = bitmap
    }

    fun updatePrintStatus(status: String) {
        _printStatus.value = status
    }

    fun generateAndPrintActa(
        context: Context,
        htmlParser: HtmlParser,
        dataProvider: CitacionDataProvider,
        zebraPrinter: PDFLabelPrinterZebra,
        impresoraViewModel: ImpresoraViewModel,
        personaViewModel: PersonaViewModel,
        guardiasViewModel: GuardiasViewModel,
        alcoholemiaDosViewModel: AlcoholemiaDosViewModel
    ) {
        viewModelScope.launch {
            try {
                _printStatus.value = "Preparando documento..."

                // Iniciar temporizador para restablecer printStatus
                launch {
                    delay(15_000L) // 15 segundos
                    if (_printStatus.value.isNotEmpty() && !_showPreviewDialog.value) {
                        _printStatus.value = ""
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Tiempo de procesamiento agotado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val macAddress = impresoraViewModel.getSelectedPrinterMac()
                if (macAddress.isNullOrEmpty()) {
                    _printStatus.value = "No hay impresora seleccionada"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No hay impresora seleccionada", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Guardar datos
                guardarDatos(context)
                personaViewModel.saveData(context)
                guardiasViewModel.saveData(context)
                alcoholemiaDosViewModel.guardarDatos(context)

                // Validar datos
                val (isValid, missingFields) = dataProvider.validateData()
                if (!isValid) {
                    _missingFields.value = missingFields
                    _printStatus.value = "Datos incompletos"
                    return@launch
                }

                // Generar HTML
                _printStatus.value = "Generando HTML..."
                val htmlContent = withContext(Dispatchers.IO) {
                    val tempHtmlFilePath = htmlParser.generateHtmlFile(
                        templateAssetPath = "documents/acta_citacion.html",
                        dataProvider = dataProvider
                    )
                    val content = File(tempHtmlFilePath).readText(Charsets.UTF_8)
                    File(tempHtmlFilePath).delete()
                    content
                }

                // Generar PDF para Zebra (previsualización)
                _printStatus.value = "Generando PDF para impresora Zebra..."
                val previewFile = File.createTempFile("citacion_zebra_preview", ".pdf", context.cacheDir)
                zebraPrinter.generarEtiquetaPdf(htmlContent, previewFile)

                if (!previewFile.exists() || previewFile.length() == 0L) {
                    _printStatus.value = "Error al generar PDF para Zebra"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error al generar PDF para Zebra", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Generar PDF A4
                _printStatus.value = "Generando PDF A4..."
                val outputFile = PdfUtils.writePdfToStorage(
                    htmlContent,
                    "acta_citacion_a4.pdf",
                    PDFA4Printer(context),
                    context
                )
                if (outputFile == null) {
                    _printStatus.value = "Error al generar PDF A4"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error al generar PDF A4", Toast.LENGTH_LONG).show()
                    }
                    previewFile.delete()
                    return@launch
                }

                // Abrir PDF A4
                withContext(Dispatchers.Main) {
                    try {
                        val contentUri = FileProvider.getUriForFile(
                            context,
                            "com.oscar.atestados.fileprovider",
                            outputFile
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(contentUri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(intent, "Seleccionar aplicación para abrir PDF"))
                        _printStatus.value = "PDF A4 abierto"
                    } catch (e: Exception) {
                        _printStatus.value = "No hay aplicación para abrir PDFs"
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "No hay aplicación para abrir PDFs", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Generar previsualización
                if (isValidPdf(previewFile)) {
                    val bitmaps = PdfToBitmapConverter.convertAllPagesToBitmaps(previewFile)
                    if (bitmaps.isNotEmpty() && bitmaps[0] != null) {
                        _previewBitmap.value = bitmaps[0]
                        _showPreviewDialog.value = true
                        _printStatus.value = "Mostrando previsualización"
                    } else {
                        _printStatus.value = "Error al generar previsualización"
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error al generar la imagen", Toast.LENGTH_SHORT).show()
                        }
                        previewFile.delete()
                    }
                } else {
                    _printStatus.value = "Error: El archivo PDF no es válido"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: El archivo PDF no es válido", Toast.LENGTH_LONG).show()
                    }
                    previewFile.delete()
                }
            } catch (e: Exception) {
                _printStatus.value = "Error al generar documentos: ${e.message}"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al generar documentos: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun confirmPrint(
        context: Context,
        htmlParser: HtmlParser,
        dataProvider: CitacionDataProvider,
        pdfToBitmapPrinter: PDFToBitmapPrinter,
        impresoraViewModel: ImpresoraViewModel
    ) {
        viewModelScope.launch {
            try {
                val macAddress = impresoraViewModel.getSelectedPrinterMac() ?: throw Exception("No hay impresora seleccionada")
                val htmlContent = withContext(Dispatchers.IO) {
                    val tempHtmlFilePath = htmlParser.generateHtmlFile(
                        templateAssetPath = "documents/acta_citacion.html",
                        dataProvider = dataProvider
                    )
                    val content = File(tempHtmlFilePath).readText(Charsets.UTF_8)
                    File(tempHtmlFilePath).delete()
                    content
                }

                _printStatus.value = "Enviando a imprimir..."
                val printResult = pdfToBitmapPrinter.printHtmlAsBitmap(
                    htmlAssetPath = "",
                    macAddress = macAddress,
                    outputFileName = "acta_citacion_temp.pdf",
                    htmlContent = htmlContent,
                    onStatusUpdate = { status -> _printStatus.value = status }
                )

                when (printResult) {
                    is PDFToBitmapPrinter.PrintResult.Success -> {
                        _printStatus.value = "Impresión enviada"
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Acta de citación enviada a imprimir", Toast.LENGTH_SHORT).show()
                        }
                        val pdfFileName = "Acta_Citacion_${System.currentTimeMillis()}.pdf"
                        val savedFile = PdfUtils.writePdfToStorage(
                            htmlContent,
                            pdfFileName,
                            PDFA4Printer(context),
                            context
                        )
                        if (savedFile != null && isValidPdf(savedFile)) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "PDF guardado en Documents/Atestados/$pdfFileName", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    is PDFToBitmapPrinter.PrintResult.Error -> {
                        _printStatus.value = "Error: ${printResult.message}"
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error al imprimir: ${printResult.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                _printStatus.value = "Error al imprimir: ${e.message}"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _previewBitmap.value = null
                _showPreviewDialog.value = false
            }
        }
    }

    private fun isValidPdf(file: File): Boolean {
        return try {
            PdfToBitmapConverter.convertAllPagesToBitmaps(file).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}