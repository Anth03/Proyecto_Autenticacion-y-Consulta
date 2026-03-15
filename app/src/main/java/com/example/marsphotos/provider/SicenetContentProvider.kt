package com.example.marsphotos.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.marsphotos.data.local.SicenetDatabase

class SicenetContentProvider : ContentProvider() {

    private lateinit var database: SicenetDatabase

    override fun onCreate(): Boolean {
        val appContext = context?.applicationContext ?: return false
        database = SicenetDatabase.getDatabase(appContext)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val db = database.openHelper.readableDatabase
        val (table, finalSelection, finalArgs) = buildTableAndSelection(uri, selection, selectionArgs)

        val projectionClause = projection?.joinToString(",") ?: "*"
        val sqlBuilder = StringBuilder("SELECT $projectionClause FROM $table")
        if (!finalSelection.isNullOrBlank()) {
            sqlBuilder.append(" WHERE ").append(finalSelection)
        }
        if (!sortOrder.isNullOrBlank()) {
            sqlBuilder.append(" ORDER BY ").append(sortOrder)
        }

        val args: Array<Any> = finalArgs?.map { it as Any }?.toTypedArray() ?: emptyArray()
        val cursor = if (args.isNotEmpty()) db.query(sqlBuilder.toString(), args) else db.query(sqlBuilder.toString())

        val resolver = context?.contentResolver
        if (resolver != null) {
            cursor.setNotificationUri(resolver, uri)
        }
        return cursor
    }

    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            CARGA -> "vnd.android.cursor.dir/vnd.${SicenetProviderContract.AUTHORITY}.${SicenetProviderContract.PATH_CARGA}"
            CARGA_BY_MATRICULA -> "vnd.android.cursor.item/vnd.${SicenetProviderContract.AUTHORITY}.${SicenetProviderContract.PATH_CARGA}"
            KARDEX -> "vnd.android.cursor.dir/vnd.${SicenetProviderContract.AUTHORITY}.${SicenetProviderContract.PATH_KARDEX}"
            KARDEX_BY_MATRICULA -> "vnd.android.cursor.item/vnd.${SicenetProviderContract.AUTHORITY}.${SicenetProviderContract.PATH_KARDEX}"
            else -> throw IllegalArgumentException("URI no soportada: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val safeValues = values ?: throw IllegalArgumentException("ContentValues no puede ser null")
        val db = database.openHelper.writableDatabase

        val rowId = when (uriMatcher.match(uri)) {
            CARGA -> db.insert("carga_academica", SQLiteDatabase.CONFLICT_REPLACE, safeValues)
            KARDEX -> db.insert("kardex", SQLiteDatabase.CONFLICT_REPLACE, safeValues)
            else -> throw IllegalArgumentException("URI no soportada para insert: $uri")
        }

        if (rowId == -1L) {
            throw IllegalStateException("No se pudo insertar registro para URI: $uri")
        }

        context?.contentResolver?.notifyChange(uri, null)
        return ContentUris.withAppendedId(uri, rowId)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = database.openHelper.writableDatabase
        val (table, finalSelection, finalArgs) = buildTableAndSelection(uri, selection, selectionArgs)
        val args: Array<Any> = finalArgs?.map { it as Any }?.toTypedArray() ?: emptyArray()

        val rows = db.delete(table, finalSelection, args)
        if (rows > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return rows
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val safeValues = values ?: throw IllegalArgumentException("ContentValues no puede ser null")
        val db = database.openHelper.writableDatabase
        val (table, finalSelection, finalArgs) = buildTableAndSelection(uri, selection, selectionArgs)
        val args: Array<Any> = finalArgs?.map { it as Any }?.toTypedArray() ?: emptyArray()

        val rows = db.update(table, SQLiteDatabase.CONFLICT_REPLACE, safeValues, finalSelection, args)
        if (rows > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return rows
    }

    private fun buildTableAndSelection(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Triple<String, String?, Array<String>?> {
        return when (uriMatcher.match(uri)) {
            CARGA -> Triple("carga_academica", selection, selectionArgs?.map { it }?.toTypedArray())
            KARDEX -> Triple("kardex", selection, selectionArgs?.map { it }?.toTypedArray())
            CARGA_BY_MATRICULA -> {
                val matricula = uri.lastPathSegment.orEmpty()
                Triple("carga_academica", combineSelectionWithMatricula(selection), combineArgsWithMatricula(selectionArgs, matricula))
            }
            KARDEX_BY_MATRICULA -> {
                val matricula = uri.lastPathSegment.orEmpty()
                Triple("kardex", combineSelectionWithMatricula(selection), combineArgsWithMatricula(selectionArgs, matricula))
            }
            else -> throw IllegalArgumentException("URI no soportada: $uri")
        }
    }

    private fun combineSelectionWithMatricula(selection: String?): String {
        val matriculaClause = "matricula = ?"
        return if (selection.isNullOrBlank()) matriculaClause else "$matriculaClause AND ($selection)"
    }

    private fun combineArgsWithMatricula(selectionArgs: Array<out String>?, matricula: String): Array<String> {
        val args = mutableListOf(matricula)
        if (selectionArgs != null) {
            args.addAll(selectionArgs)
        }
        return args.toTypedArray()
    }

    companion object {
        private const val CARGA = 1
        private const val CARGA_BY_MATRICULA = 2
        private const val KARDEX = 3
        private const val KARDEX_BY_MATRICULA = 4

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(SicenetProviderContract.AUTHORITY, SicenetProviderContract.PATH_CARGA, CARGA)
            addURI(SicenetProviderContract.AUTHORITY, "${SicenetProviderContract.PATH_CARGA}/*", CARGA_BY_MATRICULA)
            addURI(SicenetProviderContract.AUTHORITY, SicenetProviderContract.PATH_KARDEX, KARDEX)
            addURI(SicenetProviderContract.AUTHORITY, "${SicenetProviderContract.PATH_KARDEX}/*", KARDEX_BY_MATRICULA)
        }
    }
}





