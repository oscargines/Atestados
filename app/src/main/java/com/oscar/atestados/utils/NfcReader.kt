package com.oscar.atestados.utils

import android.content.Context
import android.content.ContextWrapper
import android.nfc.Tag
import android.util.Log
import es.gob.fnmt.dniedroid.gui.PasswordUI
import es.gob.jmulticard.jse.provider.DnieLoadParameter
import es.gob.jmulticard.jse.provider.DnieProvider
import java.io.IOException
import java.security.KeyStore
import java.security.Security
import java.text.SimpleDateFormat
import java.util.*
import de.tsenger.androsmex.mrtd.DG1_Dnie
import de.tsenger.androsmex.mrtd.DG11
import de.tsenger.androsmex.mrtd.DG13

/** Etiqueta utilizada para los logs de esta clase. */
private const val TAG = "NfcReader"

/**
 * Clase para leer datos de un DNI electrónico utilizando tecnología NFC.
 * Gestiona la interacción con el proveedor DnieProvider y procesa los datos de los grupos de datos MRTD (DG1, DG11, DG13).
 *
 * @property context Contexto de la aplicación necesario para inicializar el proveedor y obtener la actividad.
 * @property tag Etiqueta NFC que contiene los datos del DNI.
 */
class NfcReader(private val context: Context, private val tag: Tag) {

    /** Proveedor de seguridad para el DNI electrónico. */
    private val dnieProvider = DnieProvider()

    init {
        val activity = getActivityFromContext(context)
        if (activity != null) {
            PasswordUI.setAppContext(activity)
            PasswordUI.setPasswordDialog(null)
        } else {
            Log.w(TAG, "No se pudo obtener Activity del Context, el diálogo de contraseña podría no funcionar")
        }
    }

    /**
     * Lee los datos del DNI utilizando el código CAN y el tag NFC proporcionados.
     * Procesa los grupos de datos DG1, DG11 y DG13 y retorna un objeto [DniData] con la información extraída.
     *
     * @param canCode Código CAN del DNI necesario para autenticarse con el proveedor.
     * @return Objeto [DniData] con los datos procesados del DNI.
     * @throws IOException Si ocurre un error al leer el DNI o si DG1 no está disponible.
     */
    fun readDni(canCode: String): DniData {
        try {
            Security.insertProviderAt(dnieProvider, 1)
            Log.d(TAG, "Inicializando KeyStore con CAN: $canCode")

            val initInfo = DnieLoadParameter.getBuilder(arrayOf(canCode), tag).build()
            Log.d(TAG, "Parámetros de inicialización creados: $initInfo")

            val keyStore = KeyStore.getInstance(DnieProvider.KEYSTORE_PROVIDER_NAME)
            Log.d(TAG, "KeyStore instanciado")

            keyStore.load(initInfo)
            Log.d(TAG, "KeyStore cargado con éxito")

            val mrtdCardInfo = initInfo.getMrtdCardInfo()
            Log.d(TAG, "Información MRTD obtenida: $mrtdCardInfo")

            // Leer DG1
            val dg1 = mrtdCardInfo.getDataGroup1()
            Log.d(TAG, "DG1 data read: ${dg1?.optData ?: "No disponible"}")

            // Generar un log con los datos crudos disponibles
            val rawDataLog = StringBuilder("Datos crudos leídos del DNI:\n")

            // DG1: Datos básicos
            if (dg1 != null) {
                rawDataLog.append("DG1_Dnie: ")
                rawDataLog.append("optData=${dg1.optData}, ")
                rawDataLog.append("docNumber=${dg1.docNumber}, ")
                rawDataLog.append("name=${dg1.name}, ")
                rawDataLog.append("surname=${dg1.surname}, ")
                rawDataLog.append("dateOfBirth=${dg1.dateOfBirth}, ")
                rawDataLog.append("sex=${dg1.sex}, ")
                rawDataLog.append("nationality=${dg1.nationality}, ")
                rawDataLog.append("issuer=${dg1.issuer}, ")
                rawDataLog.append("docType=${dg1.docType}\n")
            } else {
                rawDataLog.append("DG1_Dnie: No disponible\n")
            }

            // Intentar acceder a DG11 y DG13 desde MrtdCardInfo
            var dg11: DG11? = null
            var dg13: DG13? = null
            try {
                dg11 = mrtdCardInfo.getDataGroup11()  // Acceso directo a DG11
                if (dg11 != null) {
                    rawDataLog.append("DG11: ")
                    rawDataLog.append("name=${dg11.getName()}, ")
                    rawDataLog.append("birthPlace=${dg11.getBirthPlace()}, ")
                    rawDataLog.append("address=${dg11.getAddress(DG11.ADDR_DIRECCION)}, ")
                    rawDataLog.append("locality=${dg11.getAddress(DG11.ADDR_LOCALIDAD)}, ")
                    rawDataLog.append("province=${dg11.getAddress(DG11.ADDR_PROVINCIA)}, ")
                    rawDataLog.append("phone=${dg11.getPhone()}\n")
                } else {
                    rawDataLog.append("DG11: No disponible\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al acceder a DG11: ${e.message}")
                rawDataLog.append("DG11: Error (${e.message})\n")
            }

            try {
                dg13 = mrtdCardInfo.getDataGroup13()  // Acceso directo a DG13
                if (dg13 != null) {
                    rawDataLog.append("DG13: ")
                    rawDataLog.append("fatherName=${dg13.getFatherName()}, ")
                    rawDataLog.append("motherName=${dg13.getMotherName()}, ")
                    rawDataLog.append("birthPopulation=${dg13.getBirthPopulation()}, ")
                    rawDataLog.append("actualAddress=${dg13.getActualAddress()}\n")
                } else {
                    rawDataLog.append("DG13: No disponible\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al acceder a DG13: ${e.message}")
                rawDataLog.append("DG13: Error (${e.message})\n")
            }

            // Registrar el log completo
            Log.d(TAG, rawDataLog.toString())

            // Retornar los datos procesados
            return parseDniData(dg1 ?: throw IOException("DG1 no disponible"), dg11, dg13)
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo DNI: ${e.message}", e)
            throw IOException("Error leyendo DNI: ${e.message}", e)
        }
    }

    /**
     * Procesa los datos extraídos de los grupos DG1, DG11 y DG13 para construir un objeto [DniData].
     * Formatea la fecha de nacimiento y traduce el sexo al español.
     *
     * @param dg1 Grupo de datos DG1 que contiene información básica del DNI (obligatorio).
     * @param dg11 Grupo de datos DG11 con información adicional (opcional).
     * @param dg13 Grupo de datos DG13 con información extendida (opcional).
     * @return Objeto [DniData] con los datos procesados del DNI.
     */
    private fun parseDniData(dg1: DG1_Dnie, dg11: DG11?, dg13: DG13?): DniData {
        val documentNumber = dg1.optData
        val birthDate = dg1.dateOfBirth
        val formattedDate = SimpleDateFormat("yyMMdd", Locale.getDefault()).parse(birthDate)?.let { date ->
            SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(date)
        } ?: ""
        val name = dg1.name
        val surname = dg1.surname
        val sexo = if (dg1.sex == "M") "Masculino" else "Femenino"

        // Usar DG13 para datos adicionales si está disponible, de lo contrario DG11 o vacío
        val nombrePadre = dg13?.getFatherName() ?: ""
        val nombreMadre = dg13?.getMotherName() ?: ""
        val lugarNacimiento = dg13?.getBirthPopulation() ?: dg11?.getBirthPlace() ?: ""
        val domicilio = dg13?.getActualAddress() ?: dg11?.getAddress(DG11.ADDR_DIRECCION) ?: ""

        Log.d(TAG, "Datos procesados del DNI - Número Documento: $documentNumber, Nombre: $name, Apellidos: $surname")

        return DniData(
            numeroDocumento = documentNumber,
            nombre = name,
            apellidos = surname,
            fechaNacimiento = formattedDate,
            sexo = sexo,
            nacionalidad = dg1.nationality,
            nombrePadre = nombrePadre,
            nombreMadre = nombreMadre,
            lugarNacimiento = lugarNacimiento,
            domicilio = domicilio
        )
    }

    /**
     * Obtiene la actividad subyacente a partir de un contexto, desenvolviendo los ContextWrapper si es necesario.
     *
     * @param context Contexto desde el cual buscar la actividad.
     * @return La actividad encontrada o null si no se encuentra ninguna.
     */
    private fun getActivityFromContext(context: Context): android.app.Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is android.app.Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }
}

/**
 * Modelo de datos que representa la información extraída de un DNI electrónico.
 *
 * @property numeroDocumento Número del documento del DNI.
 * @property nombre Nombre del titular.
 * @property apellidos Apellidos del titular.
 * @property fechaNacimiento Fecha de nacimiento formateada en español.
 * @property sexo Sexo del titular ("Masculino" o "Femenino").
 * @property nacionalidad Nacionalidad del titular (por defecto "España").
 * @property nombrePadre Nombre del padre del titular.
 * @property nombreMadre Nombre de la madre del titular.
 * @property lugarNacimiento Lugar de nacimiento del titular.
 * @property domicilio Domicilio actual del titular.
 */
data class DniData(
    val numeroDocumento: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val fechaNacimiento: String = "",
    val sexo: String = "",
    val nacionalidad: String = "España",
    val nombrePadre: String = "",
    val nombreMadre: String = "",
    val lugarNacimiento: String = "",
    val domicilio: String = ""
)