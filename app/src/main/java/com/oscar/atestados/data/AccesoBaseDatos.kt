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

class AccesoBaseDatos(context: Context, nombreBaseDatos: String, version: Int):
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

    override fun onCreate(db: SQLiteDatabase?) {
        // Función vacía ya que la base de datos ya está creada
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Aquí puedes manejar la actualización de la base de datos si es necesario
    }

    fun query(query: String): List<String> {
        val result = mutableListOf<String>()
        val db = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                // Supongamos que la consulta devuelve una sola columna de tipo String
                val data = cursor.getString(0)
                result.add(data)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return result
    }
    fun copiaBD(): Boolean{
        copyDatabase()
        return checkDatabase()
    }
}