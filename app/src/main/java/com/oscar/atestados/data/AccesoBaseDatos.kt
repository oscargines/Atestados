package com.oscar.atestados.data

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
 */
class AccesoBaseDatos(context: Context, nombreBaseDatos: String) :
    SQLiteOpenHelper(context, nombreBaseDatos, null, EXPECTED_DATABASE_VERSION) {

    private val DATABASE_NAME = nombreBaseDatos
    private val DATABASE_VERSION = EXPECTED_DATABASE_VERSION
    private val myContext: Context = context
    private val DB_PATH: String = context.getDatabasePath(DATABASE_NAME).toString()

    companion object {
        const val EXPECTED_DATABASE_VERSION = 2 // Incrementar cuando se actualice cualquier .db en assets
        private const val TAG = "AccesoBaseDatos"
    }

    init {
        Log.d(TAG, "Ruta de la base de datos: $DB_PATH")
    }

    /**
     * Inicializa la base de datos, verificando si existe y si su versión es la esperada.
     * Si no existe o está obsoleta, la copia desde assets.
     */
    fun initializeDatabases() {
        if (!checkDatabase()) {
            try {
                createDatabase()
            } catch (e: IOException) {
                Log.e(TAG, "Error inicializando $DATABASE_NAME: ${e.message}", e)
                // Crear una base de datos vacía como respaldo
                ensureTableExists()
            }
        } else {
            Log.d(TAG, "Base de datos $DATABASE_NAME ya está actualizada")
        }
    }

    /**
     * Crea una nueva base de datos copiándola desde los assets.
     *
     * @throws IOException Si ocurre un error al copiar la base de datos.
     */
    @Throws(IOException::class)
    fun createDatabase() {
        this.close()
        try {
            copyDatabase()
        } catch (e: IOException) {
            Log.e(TAG, "Error copiando la base de datos $DATABASE_NAME: ${e.message}", e)
            throw IOException("No se pudo copiar la base de datos desde dataBases/$DATABASE_NAME: ${e.message}", e)
        }
    }

    /**
     * Verifica si la base de datos existe en la ruta especificada y tiene la versión correcta.
     *
     * @return true si la base de datos existe y está actualizada, false si no existe o está obsoleta.
     */
    fun checkDatabase(): Boolean {
        var db: SQLiteDatabase? = null
        return try {
            Log.d(TAG, "Verificando existencia de $DATABASE_NAME en $DB_PATH")
            db = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY)
            val version = db.version
            Log.d(TAG, "Base de datos $DATABASE_NAME encontrada con versión $version")
            if (version < EXPECTED_DATABASE_VERSION) {
                Log.d(TAG, "Versión obsoleta (v$version < v$EXPECTED_DATABASE_VERSION)")
                false
            } else {
                Log.d(TAG, "Versión válida (v$version)")
                true
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error verificando $DATABASE_NAME: ${e.message}", e)
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
        Log.d(TAG, "Iniciando copia de $DATABASE_NAME desde assets")
        if (!dbFile.parentFile.exists()) {
            Log.d(TAG, "Creando directorio padre: ${dbFile.parentFile.path}")
            if (!dbFile.parentFile.mkdirs()) {
                throw IOException("No se pudo crear el directorio padre: ${dbFile.parentFile.path}")
            }
            Log.d(TAG, "Copia de $DATABASE_NAME completada con éxito")
        }
        try {
            val assetPath = "dataBases/$DATABASE_NAME"
            val inputStream = myContext.assets.open(assetPath)
            Log.d(TAG, "Abriendo archivo desde assets: $assetPath")
            val outputStream = FileOutputStream(dbFile)
            val buffer = ByteArray(1024)
            var length: Int
            try {
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
                outputStream.flush()
                Log.d(TAG, "Base de datos $DATABASE_NAME copiada exitosamente a $DB_PATH")
            } catch (e: IOException) {
                Log.e(TAG, "Error durante la copia de la base de datos $DATABASE_NAME: ${e.message}", e)
                throw e
            } finally {
                outputStream.close()
                inputStream.close()
            }
            SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.version = EXPECTED_DATABASE_VERSION
            }
            Log.d(TAG, "Base de datos $DATABASE_NAME copiada con versión $EXPECTED_DATABASE_VERSION")
        } catch (e: IOException) {
            Log.e(TAG, "No se pudo abrir el archivo dataBases/$DATABASE_NAME desde assets: ${e.message}", e)
            throw e
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        when (DATABASE_NAME) {
            "dispositivos.db" -> {
                db?.execSQL("""
                CREATE TABLE IF NOT EXISTS dispositivos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    mac TEXT NOT NULL UNIQUE
                )
            """)
                Log.d(TAG, "Tabla 'dispositivos' creada en $DATABASE_NAME")
            }
            "paises.db" -> {
                db?.execSQL("""
                CREATE TABLE IF NOT EXISTS paises (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL
                )
            """)
                Log.d(TAG, "Tabla 'paises' creada en $DATABASE_NAME")
            }
            "juzgados.db" -> {
                db?.execSQL("""
                CREATE TABLE IF NOT EXISTS juzgados (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    ubicacion TEXT
                )
            """)
                db?.execSQL("""
                CREATE TABLE IF NOT EXISTS partidos_judiciales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    municipio TEXT NOT NULL,
                    partido_judicial TEXT NOT NULL
                )
            """)
                Log.d(TAG, "Tablas 'juzgados' y 'partidos_judiciales' creadas en $DATABASE_NAME")
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Actualizando $DATABASE_NAME de versión $oldVersion a $newVersion")
        try {
            createDatabase() // Copia la nueva base de datos desde assets
        } catch (e: IOException) {
            Log.e(TAG, "Error al actualizar $DATABASE_NAME: ${e.message}", e)
            // Opcional: Crear tablas de respaldo si la copia falla
            onCreate(db)
        }
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Downgrade detectado: de versión $oldVersion a $newVersion")
        val dbFile = File(DB_PATH)
        if (dbFile.exists()) {
            dbFile.delete()
            Log.d(TAG, "Base de datos $DATABASE_NAME eliminada debido a downgrade")
        }
        try {
            createDatabase()
        } catch (e: IOException) {
            Log.e(TAG, "Error recreando la base de datos tras downgrade: ${e.message}", e)
        }
    }

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

    fun ensureTableExists() {
        try {
            val db = this.writableDatabase
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS dispositivos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    mac TEXT NOT NULL UNIQUE
                )
            """)
            Log.d(TAG, "Tabla 'dispositivos' asegurada con UNIQUE en mac")
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error asegurando la tabla dispositivos: ${e.message}", e)
        }
    }

    fun execSQL(query: String, args: Array<Any?> = emptyArray()) {
        writableDatabase.execSQL(query, args)
    }

    fun getDatabasePath(): String {
        return DB_PATH
    }
}