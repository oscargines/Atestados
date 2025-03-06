package com.oscar.atestados.data

import android.content.ContentValues.TAG
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Clase para gestionar el acceso a una base de datos SQLite en la aplicación.
 * Extiende [SQLiteOpenHelper] para manejar la creación, copia y acceso a la base de datos.
 *
 * @property context Contexto de la aplicación necesario para acceder a los assets y la ruta de la base de datos.
 * @property nombreBaseDatos Nombre de la base de datos a gestionar.
 * @property version Versión de la base de datos.
 */
class AccesoBaseDatos(context: Context, nombreBaseDatos: String, version: Int) :
    SQLiteOpenHelper(context, nombreBaseDatos, null, version) {

    /** Nombre de la base de datos. */
    private val DATABASE_NAME = nombreBaseDatos

    /** Versión de la base de datos. */
    private val DATABASE_VERSION = version

    /** Ruta completa donde se almacenará la base de datos en el dispositivo. */
    private lateinit var DB_PATH: String

    /** Contexto de la aplicación utilizado para operaciones de archivo y assets. */
    private val myContext: Context = context

    /**
     * Inicializa la clase configurando la ruta de la base de datos y verificando su existencia.
     * Si la base de datos no existe, se crea copiándola desde los assets.
     */
    init {
        DB_PATH = context.getDatabasePath(DATABASE_NAME).toString()
        Log.d("AccesoBaseDatos", "Ruta de la base de datos: $DB_PATH")
        if (!checkDatabase()) {
            createDatabase()
        }
    }

    /**
     * Crea la base de datos si no existe en el dispositivo.
     * Cierra la conexión actual y copia la base de datos desde los assets.
     *
     * @throws IOException Si ocurre un error al copiar la base de datos desde los assets.
     */
    @Throws(IOException::class)
    fun createDatabase() {
        val dbExist = checkDatabase()
        if (!dbExist) {
            this.close()
            try {
                copyDatabase()
            } catch (e: IOException) {
                Log.e("AccesoBaseDatos", "Error copiando la base de datos: ${e.message}")
                throw Error("Error copiando la base de datos")
            }
        }
    }

    /**
     * Verifica si la base de datos ya existe en la ruta especificada.
     *
     * @return `true` si la base de datos existe y es accesible, `false` en caso contrario.
     */
    private fun checkDatabase(): Boolean {
        var db: SQLiteDatabase? = null
        return try {
            db = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY)
            Log.e(TAG, "Se ha encontrado la base de datos, se encuentra en $DB_PATH")
            true
        } catch (e: SQLiteException) {
            Log.e(TAG, "La base de datos no existe")
            false
        } finally {
            db?.close()
        }
    }

    /**
     * Copia la base de datos desde los assets al almacenamiento interno del dispositivo.
     *
     * @throws IOException Si ocurre un error al leer desde los assets o escribir en el dispositivo.
     */
    @Throws(IOException::class)
    private fun copyDatabase() {
        // Asegúrate de que el directorio de la base de datos exista
        val dbFile = File(DB_PATH)
        if (!dbFile.parentFile.exists()) {
            dbFile.parentFile.mkdirs() // Crea el directorio si no existe
        }

        // Abre el archivo de la base de datos desde los assets
        val inputStream = myContext.assets.open(DATABASE_NAME)
        val outputStream = FileOutputStream(dbFile)
        val buffer = ByteArray(1024)
        var length: Int

        // Copia el archivo
        try {
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush()
            Log.d("AccesoBaseDatos", "Base de datos copiada exitosamente a $DB_PATH")
        } catch (e: IOException) {
            Log.e("AccesoBaseDatos", "Error copiando la base de datos: ${e.message}")
            throw e
        } finally {
            outputStream.close()
            inputStream.close()
        }
    }

    /**
     * Método sobrescrito de [SQLiteOpenHelper] para la creación de la base de datos.
     * Está vacío porque la base de datos se copia desde los assets en lugar de crearse desde cero.
     *
     * @param db Instancia de la base de datos SQLite.
     */
    override fun onCreate(db: SQLiteDatabase?) {
        // Función vacía ya que la base de datos ya está creada
    }

    /**
     * Método sobrescrito de [SQLiteOpenHelper] para la actualización de la base de datos.
     * Actualmente no implementa lógica de actualización.
     *
     * @param db Instancia de la base de datos SQLite.
     * @param oldVersion Versión anterior de la base de datos.
     * @param newVersion Nueva versión de la base de datos.
     */
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Aquí puedes manejar la actualización de la base de datos si es necesario
    }

    /**
     * Ejecuta una consulta SQL en la base de datos y devuelve los resultados como una lista de mapas.
     *
     * @param query Consulta SQL a ejecutar.
     * @return Lista de mapas donde cada mapa representa una fila con nombres de columnas como claves y valores como datos.
     */
    fun query(query: String): List<Map<String, Any>> {
        return SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery(query, null).use { cursor ->
                val result = mutableListOf<Map<String, Any>>()
                if (cursor.moveToFirst()) {
                    do {
                        val row = mutableMapOf<String, Any>()
                        for (i in 0 until cursor.columnCount) {
                            row[cursor.getColumnName(i)] = cursor.getString(i)
                        }
                        result.add(row)
                    } while (cursor.moveToNext())
                }
                result
            }
        }
    }

    /**
     * Realiza una copia de la base de datos desde los assets al dispositivo.
     *
     * @return `true` si la copia fue exitosa y la base de datos existe, `false` si ocurrió un error.
     */
    fun copiaBD(): Boolean {
        try {
            copyDatabase()
            return checkDatabase()
        } catch (e: IOException) {
            Log.e("AccesoBaseDatos", "Error copiando la base de datos: ${e.message}")
            return false
        }
    }

    /**
     * Asegura que la tabla 'dispositivos' exista en la base de datos.
     * Crea la tabla si no existe con columnas para id, nombre y mac.
     */
    fun ensureTableExists() {
        val db = this.writableDatabase
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS dispositivos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                mac TEXT NOT NULL UNIQUE
            )
        """)
    }

    /**
     * Ejecuta una sentencia SQL en la base de datos con argumentos opcionales.
     *
     * @param query Sentencia SQL a ejecutar.
     * @param args Argumentos para la sentencia SQL, por defecto es un arreglo vacío.
     */
    fun execSQL(query: String, args: Array<Any?> = emptyArray()) {
        writableDatabase.execSQL(query, args)
    }
}