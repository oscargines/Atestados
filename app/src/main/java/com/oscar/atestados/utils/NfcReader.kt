package com.oscar.atestados.utils

import android.content.Context
import android.nfc.Tag
import android.util.Log
import es.gob.fnmt.dniedroid.help.Loader
import de.tsenger.androsmex.mrtd.DG1_Dnie
import de.tsenger.androsmex.mrtd.DG11
import de.tsenger.androsmex.mrtd.DG13
import es.gob.jmulticard.card.CryptoCardException
import com.oscar.atestados.data.RawNfcData
import java.io.IOException
import java.security.GeneralSecurityException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NfcReader(
    private val context: Context,
    private val tag: Tag?
) {

    private val TAG = "NfcReader"

    suspend fun readDni(can: String): RawNfcData {
        if (tag == null) {
            Log.e(TAG, "No se detectó ningún tag NFC")
            return RawNfcData(
                uid = null,
                can = null,
                dg1Bytes = null,
                dg11Bytes = null,
                dg13Bytes = null
            )
        }

        val uid = byteArrayToHexString(tag.id)
        Log.d(TAG, "UID del tag NFC: $uid")

        if (can.isEmpty()) {
            Log.e(TAG, "Código CAN no proporcionado")
            return RawNfcData(
                uid = uid,
                can = null,
                dg1Bytes = null,
                dg11Bytes = null,
                dg13Bytes = null
            )
        }

        Log.d(TAG, "Iniciando lectura del DNI con CAN: $can")

        try {
            Log.d(TAG, "Inicializando Loader con CAN: $can y tag: $tag")
            val initInfo = Loader.init(can, tag)
            val mrtdCard = initInfo.getMrtdCardInfo()
            Log.d(TAG, "MrtdCard inicializado correctamente")

            // Leer DG1
            val dg1Bytes: ByteArray? = try {
                Log.d(TAG, "Intentando leer DG1")
                val dg1: DG1_Dnie? = mrtdCard.getDataGroup1()
                dg1?.getBytes()?.also { Log.d(TAG, "DG1 leído exitosamente: ${it.joinToString("") { "%02x".format(it) }}") }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar DG1: ${e.message}", e)
                null
            }

            // Leer DG11
            val dg11Bytes: ByteArray? = try {
                Log.d(TAG, "Intentando leer DG11")
                val dg11: DG11? = mrtdCard.getDataGroup11()
                dg11?.getBytes()?.also { Log.d(TAG, "DG11 leído exitosamente: ${it.joinToString("") { "%02x".format(it) }}") }
            } catch (e: Exception) {
                Log.w(TAG, "DG11 no disponible: ${e.message}")
                null
            }

            // Leer DG13
            val dg13Bytes: ByteArray? = try {
                Log.d(TAG, "Intentando leer DG13")
                val dg13: DG13? = mrtdCard.getDataGroup13()
                dg13?.getBytes()?.also { Log.d(TAG, "DG13 leído exitosamente: ${it.joinToString("") { "%02x".format(it) }}") }
            } catch (e: Exception) {
                Log.w(TAG, "Error al cargar DG13: ${e.message}", e)
                null
            }

            Log.d(TAG, "Disponibilidad - DG1: ${dg1Bytes != null}, DG11: ${dg11Bytes != null}, DG13: ${dg13Bytes != null}")
            return RawNfcData(
                uid = uid,
                can = can,
                dg1Bytes = dg1Bytes,
                dg11Bytes = dg11Bytes,
                dg13Bytes = dg13Bytes
            )
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Error de seguridad al leer DNIe: ${e.message}", e)
            return RawNfcData(
                uid = uid,
                can = null,
                dg1Bytes = null,
                dg11Bytes = null,
                dg13Bytes = null
            )
        } catch (e: CryptoCardException) {
            Log.e(TAG, "Error específico de la tarjeta al leer DNIe: ${e.message}", e)
            return RawNfcData(
                uid = uid,
                can = null,
                dg1Bytes = null,
                dg11Bytes = null,
                dg13Bytes = null
            )
        } catch (e: IOException) {
            Log.e(TAG, "Error de E/S al leer DNIe: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al leer DNIe: ${e.message}", e)
            return RawNfcData(
                uid = uid,
                can = null,
                dg1Bytes = null,
                dg11Bytes = null,
                dg13Bytes = null
            )
        }
    }

    private fun byteArrayToHexString(bArr: ByteArray): String {
        val hexChars = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F")
        var result = ""
        for (b in bArr) {
            result += hexChars[((b.toInt() and 0xFF) shr 4) and 0x0F]
            result += hexChars[b.toInt() and 0x0F]
        }
        Log.d(TAG, "UID convertido a hexadecimal: $result")
        return result
    }
}