package com.example.sicenetclient

import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ProviderClientScreen(
                    onQueryCarga = { matricula -> queryByMatricula(ProviderContract.CARGA_URI, matricula) },
                    onQueryKardex = { matricula -> queryByMatricula(ProviderContract.KARDEX_URI, matricula) },
                    onInsertDemo = { matricula -> insertDemoCarga(matricula) }
                )
            }
        }
    }

    private fun queryByMatricula(baseUri: android.net.Uri, matricula: String): List<String> {
        val targetUri = android.net.Uri.withAppendedPath(baseUri, matricula)
        val rows = mutableListOf<String>()

        return try {
            val cursor = contentResolver.query(targetUri, null, null, null, "materia ASC")
            rows.addAll(cursor.toDisplayRows())
            cursor?.close()
            if (rows.isEmpty()) listOf("Sin datos para $matricula") else rows
        } catch (security: SecurityException) {
            listOf("Sin permiso: ${security.message}")
        } catch (e: Exception) {
            listOf("Error: ${e.message}")
        }
    }

    private fun insertDemoCarga(matricula: String): String {
        return try {
            val values = ContentValues().apply {
                put("matricula", matricula)
                put("clvOficial", "CPTEST")
                put("materia", "PRUEBA CONTENT PROVIDER")
                put("grupo", "A")
                put("creditos", 0)
                put("docente", "N/A")
                put("observaciones", "Insertado por cliente")
                put("estadoMateria", 0)
                put("semestre", 0)
                put("lastUpdated", System.currentTimeMillis())
            }
            val uri = contentResolver.insert(ProviderContract.CARGA_URI, values)
            if (uri != null) "Insert OK: $uri" else "No se insertó registro"
        } catch (security: SecurityException) {
            "Sin permiso de escritura: ${security.message}"
        } catch (e: Exception) {
            "Error al insertar: ${e.message}"
        }
    }
}

@Composable
private fun ProviderClientScreen(
    onQueryCarga: (String) -> List<String>,
    onQueryKardex: (String) -> List<String>,
    onInsertDemo: (String) -> String
) {
    var matricula by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Listo") }
    var rows by remember { mutableStateOf(emptyList<String>()) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = matricula,
                onValueChange = { matricula = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Matricula") },
                singleLine = true
            )

            Button(
                onClick = {
                    rows = onQueryCarga(matricula)
                    status = "Consulta de carga academica completada"
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = matricula.isNotBlank()
            ) {
                Text("Consultar carga academica")
            }

            Button(
                onClick = {
                    rows = onQueryKardex(matricula)
                    status = "Consulta de kardex completada"
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = matricula.isNotBlank()
            ) {
                Text("Consultar kardex")
            }

            Button(
                onClick = {
                    status = onInsertDemo(matricula)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = matricula.isNotBlank()
            ) {
                Text("Insertar registro de prueba")
            }

            Text(text = status)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(rows) { line ->
                    Text(text = line, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun Cursor?.toDisplayRows(): List<String> {
    if (this == null) return emptyList()

    val lines = mutableListOf<String>()
    val materiaIndex = getColumnIndex("materia")
    val clvIndex = getColumnIndex("clvOficial")
    val califIndex = getColumnIndex("calificacion")
    val periodoIndex = getColumnIndex("periodo")

    while (moveToNext()) {
        val materia = if (materiaIndex >= 0) getString(materiaIndex) else ""
        val clave = if (clvIndex >= 0) getString(clvIndex) else ""
        val calif = if (califIndex >= 0) getString(califIndex) else ""
        val periodo = if (periodoIndex >= 0) getString(periodoIndex) else ""
        val extra = if (calif.isNotBlank()) " | Calif: $calif | Periodo: $periodo" else ""
        lines += "$clave - $materia$extra"
    }
    return lines
}


