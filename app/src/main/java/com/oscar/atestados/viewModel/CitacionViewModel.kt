package com.oscar.atestados.viewModel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.oscar.atestados.data.CitacionDataProvider
import com.oscar.atestados.utils.HtmlParser
import com.oscar.atestados.utils.PDFLabelPrinterZebra
import com.oscar.atestados.utils.PdfToBitmapConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference

private const val TAG = "CitacionViewModel"
private const val PREFS_NAME = "CitacionPrefs"

/**
 * ViewModel para gestionar los datos de la pantalla de citación.
 * Almacena y actualiza los datos relacionados con la diligencia de citación,
 * como fechas, horas, información del juzgado y detalles del abogado.
 * También maneja la persistencia de datos utilizando SharedPreferences.
 *
 * @param application Aplicación Android necesaria para inicializar el ViewModel.
 */
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

    /**
     * Carga los datos guardados desde SharedPreferences al iniciar el ViewModel.
     *
     * @param context Contexto de la aplicación.
     */
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

    /**
     * Guarda los datos actuales en SharedPreferences.
     *
     * @param context Contexto de la aplicación.
     */
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

    /**
     * Limpia todos los datos almacenados en el ViewModel y SharedPreferences.
     *
     * @param context Contexto de la aplicación.
     */
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

    // Funciones para actualizar los estados
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

    /**
     * Actualiza la selección de abogado (designado o de oficio).
     * Asegura que solo una opción esté seleccionada a la vez si es necesario.
     *
     * @param designado Indica si se selecciona un abogado designado.
     * @param oficio Indica si se solicita un abogado de oficio.
     */
    fun updateAbogadoSelection(designado: Boolean, oficio: Boolean) {
        if (designado && oficio) {
            // Solo uno puede estar seleccionado
            _abogadoDesignado.value = true
            _abogadoOficio.value = false
        } else {
            _abogadoDesignado.value = designado
            _abogadoOficio.value = oficio
        }
    }

    /**
     * Imprime el acta de citación generando un archivo HTML y PDF.
     *
     * @param context Contexto de la aplicación.
     * @param htmlParser Instancia de HtmlParser para generar el HTML.
     * @param dataProvider Proveedor de datos para la citación.
     * @param zebraPrinter Instancia de PDFLabelPrinterZebra para generar el PDF.
     * @param impresoraViewModel ViewModel para obtener la dirección MAC de la impresora.
     * @param onStatusUpdate Callback para actualizar el estado de la impresión.
     * @param onError Callback para manejar errores.
     * @param onComplete Callback para indicar que la impresión ha finalizado.
     */
    fun printActa(
        context: Context,
        htmlParser: HtmlParser,
        dataProvider: CitacionDataProvider,
        zebraPrinter: PDFLabelPrinterZebra,
        impresoraViewModel: ImpresoraViewModel,
        personaViewModel: PersonaViewModel,
        guardiasViewModel: GuardiasViewModel,
        alcoholemiaDosViewModel: AlcoholemiaDosViewModel,
        onStatusUpdate: (String) -> Unit,
        onError: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val macAddress = impresoraViewModel.getSelectedPrinterMac()
                Log.d("CitacionViewModel", "Obteniendo MAC de impresora seleccionada: $macAddress")
                if (macAddress.isNullOrEmpty()) {
                    Log.w("CitacionViewModel", "No hay impresora seleccionada")
                    withContext(Dispatchers.Main) {
                        onError("No hay impresora seleccionada")
                    }
                    return@launch
                }
                Log.d("CitacionViewModel", "MAC de impresora: $macAddress")

                // Guardar datos de todos los ViewModels de forma síncrona
                withContext(Dispatchers.IO) {
                    Log.d("CitacionViewModel", "Guardando datos en SharedPreferences")
                    guardarDatos(context)
                    personaViewModel.saveData(context)
                    guardiasViewModel.saveData(context)
                    alcoholemiaDosViewModel.guardarDatos(context)
                }

                Log.d("CitacionViewModel", "Datos guardados antes de validar, numeroDocumento: ${personaViewModel.numeroDocumento.value}")
                if (personaViewModel.numeroDocumento.value.isNullOrEmpty()) {
                    Log.w("CitacionViewModel", "numeroDocumento está vacío")
                    withContext(Dispatchers.Main) {
                        onError("El número de documento está vacío")
                    }
                    return@launch
                }
                Log.d("CitacionViewModel", "numeroDocumento válido antes de validar: ${personaViewModel.numeroDocumento.value}")

                // Validar datos
                Log.d("CitacionViewModel", "Iniciando validación de datos")
                val (isValid, missingFields) = dataProvider.validateData()
                if (!isValid) {
                    Log.w("CitacionViewModel", "Validación fallida, campos faltantes: $missingFields")
                    withContext(Dispatchers.Main) {
                        onError("Datos incompletos: ${missingFields.joinToString(", ")}")
                    }
                    return@launch
                }
                Log.d("CitacionViewModel", "Validación exitosa")

                withContext(Dispatchers.Main) {
                    onStatusUpdate("Preparando documento...")
                }

                val tempHtmlFilePath = htmlParser.generateHtmlFile(
                    templateAssetPath = "documents/acta_citacion.html",
                    dataProvider = dataProvider
                )
                Log.d("CitacionViewModel", "HTML generado en: $tempHtmlFilePath")

                withContext(Dispatchers.Main) {
                    onStatusUpdate("Generando PDF para impresora Zebra...")
                }

                val previewFile = java.io.File.createTempFile("citacion_zebra_preview", ".pdf", context.cacheDir)
                zebraPrinter.generarEtiquetaPdf(tempHtmlFilePath, previewFile)
                Log.d("CitacionViewModel", "PDF Zebra generado en: ${previewFile.absolutePath}")

                withContext(Dispatchers.Main) {
                    onStatusUpdate("Previsualización lista")
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("CitacionViewModel", "Error en printActa: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError("Error al generar el documento: ${e.message}")
                }
            }
        }
    }
}