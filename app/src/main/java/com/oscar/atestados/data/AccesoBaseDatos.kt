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
 * Clase helper para manejar operaciones con bases de datos SQLite.
 *
 * @property context Contexto de la aplicación.
 * @property nombreBaseDatos Nombre del archivo de la base de datos.
 * @property version Versión de la base de datos.
 */
class AccesoBaseDatos(context: Context, nombreBaseDatos: String, version: Int) :
    SQLiteOpenHelper(context, nombreBaseDatos, null, version) {

    private val DATABASE_NAME = nombreBaseDatos
    private val DATABASE_VERSION = version
    private lateinit var DB_PATH: String
    private val myContext: Context = context

    init {
        DB_PATH = context.getDatabasePath(DATABASE_NAME).toString()
        Log.d("AccesoBaseDatos", "Ruta de la base de datos: $DB_PATH")
        if (!checkDatabase()) {
            createDatabase()
        }
    }

    /**
     * Crea una nueva base de datos copiándola desde los assets.
     *
     * @throws IOException Si ocurre un error al copiar la base de datos.
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
     * Verifica si la base de datos existe en la ruta especificada.
     *
     * @return true si la base de datos existe, false en caso contrario.
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
     * Copia la base de datos desde los assets al directorio de la aplicación.
     *
     * @throws IOException Si ocurre un error durante la copia.
     */
    @Throws(IOException::class)
    private fun copyDatabase() {
        val dbFile = File(DB_PATH)
        if (!dbFile.parentFile.exists()) {
            dbFile.parentFile.mkdirs()
        }
        val inputStream = myContext.assets.open(DATABASE_NAME)
        val outputStream = FileOutputStream(dbFile)
        val buffer = ByteArray(1024)
        var length: Int
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
     * Método llamado cuando se crea la base de datos por primera vez.
     *
     * @param db La base de datos.
     */
    override fun onCreate(db: SQLiteDatabase?) {}

    /**
     * Método llamado cuando la base de datos necesita ser actualizada.
     *
     * @param db La base de datos.
     * @param oldVersion La versión antigua de la base de datos.
     * @param newVersion La nueva versión de la base de datos.
     */
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    /**
     * Ejecuta una consulta SQL en la base de datos y devuelve los resultados como una lista de mapas.
     *
     * @param query Consulta SQL a ejecutar.
     * @param args Argumentos para la consulta SQL (opcional).
     * @return Lista de mapas donde cada mapa representa una fila con nombres de columnas como claves y valores como datos.
     */
    fun query(query: String, args: Array<String>? = null): List<Map<String, Any>> {
        return SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery(query, args).use { cursor ->
                val result = mutableListOf<Map<String, Any>>()
                if (cursor.moveToFirst()) {
                    do {
                        val row = mutableMapOf<String, Any>()
                        for (i in 0 until cursor.columnCount) {
                            row[cursor.getColumnName(i)] = cursor.getString(i) ?: ""
                        }
                        result.add(row)
                    } while (cursor.moveToNext())
                }
                result
            }
        }
    }

    /**
     * Copia la base de datos desde los assets y verifica si se copió correctamente.
     *
     * @return true si la copia fue exitosa, false en caso contrario.
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
     * Asegura que la tabla 'dispositivos' exista en la base de datos con las columnas requeridas.
     * Si la tabla no existe, la crea con las columnas id, nombre y mac (única).
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
        Log.d("AccesoBaseDatos", "Tabla 'dispositivos' asegurada con UNIQUE en mac")
    }

    /**
     * Ejecuta una sentencia SQL en la base de datos.
     *
     * @param query Sentencia SQL a ejecutar.
     * @param args Argumentos para la sentencia SQL (opcional).
     */
    fun execSQL(query: String, args: Array<Any?> = emptyArray()) {
        writableDatabase.execSQL(query, args)
    }
}