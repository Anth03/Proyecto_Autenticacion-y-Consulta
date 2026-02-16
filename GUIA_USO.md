# 🚀 Guía Rápida: Uso de WorkManager y Room en SICENET App

## 📚 Tabla de Contenidos
1. [Cómo Usar la Sincronización](#cómo-usar-la-sincronización)
2. [Cómo Consultar Datos Locales](#cómo-consultar-datos-locales)
3. [Ejemplos de Código](#ejemplos-de-código)
4. [Estructura de Archivos](#estructura-de-archivos)

---

## 🎯 Cómo Usar la Sincronización

### Iniciar Sincronización Manual

En tu `SNViewModel` o donde necesites sincronizar:

```kotlin
// Importar en SNViewModel
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SNViewModel(private val container: AppContainer) : ViewModel() {
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus
    
    fun syncData(matricula: String, password: String) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            
            // Iniciar la sincronización
            val operation = container.startSicenetSync(matricula, password)
            
            // Observar el estado del primer worker (consulta API)
            container.workManager
                .getWorkInfosByTagFlow("sicenet_sync")
                .collect { workInfos ->
                    workInfos.firstOrNull()?.let { workInfo ->
                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                _syncStatus.value = SyncStatus.Success
                                Log.d("Sync", "Sincronización completada")
                            }
                            WorkInfo.State.FAILED -> {
                                _syncStatus.value = SyncStatus.Error("Sincronización fallida")
                                Log.e("Sync", "Error en sincronización")
                            }
                            WorkInfo.State.RUNNING -> {
                                _syncStatus.value = SyncStatus.Syncing
                                Log.d("Sync", "Sincronizando...")
                            }
                            else -> {}
                        }
                    }
                }
        }
    }
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}
```

---

## 📂 Cómo Consultar Datos Locales

### 1. Consultar Perfil del Estudiante

```kotlin
class SNViewModel(private val container: AppContainer) : ViewModel() {
    
    // Flow que emite el perfil cada vez que cambia en la BD
    fun getProfileFlow(matricula: String): Flow<ProfileStudent?> {
        return container.localSNRepository.getProfile(matricula)
    }
    
    // En tu Composable:
    @Composable
    fun ProfileScreen(viewModel: SNViewModel) {
        val profile by viewModel.getProfileFlow("S21120184")
            .collectAsState(initial = null)
        
        profile?.let {
            Text("Nombre: ${it.nombre}")
            Text("Carrera: ${it.carrera}")
            Text("Semestre: ${it.semestre}")
            Text("Promedio: ${it.creditosAcumulados}")
        } ?: Text("Cargando perfil...")
    }
}
```

### 2. Consultar Carga Académica

```kotlin
// En el ViewModel
fun getCargaAcademicaFlow(matricula: String): Flow<List<CargaAcademica>> {
    return container.localSNRepository.getCargaAcademica(matricula)
}

// En el Composable
@Composable
fun CargaAcademicaScreen(viewModel: SNViewModel) {
    val cargaAcademica by viewModel.getCargaAcademicaFlow("S21120184")
        .collectAsState(initial = emptyList())
    
    LazyColumn {
        items(cargaAcademica) { materia ->
            Card {
                Column {
                    Text("Materia: ${materia.materia}")
                    Text("Grupo: ${materia.grupo}")
                    Text("Docente: ${materia.docente}")
                    Text("Créditos: ${materia.creditos}")
                }
            }
        }
    }
}
```

### 3. Consultar Kardex

```kotlin
// En el ViewModel
fun getKardexFlow(matricula: String): Flow<List<Kardex>> {
    return container.localSNRepository.getKardex(matricula)
}

// En el Composable
@Composable
fun KardexScreen(viewModel: SNViewModel) {
    val kardex by viewModel.getKardexFlow("S21120184")
        .collectAsState(initial = emptyList())
    
    LazyColumn {
        items(kardex) { materia ->
            Card {
                Column {
                    Text("${materia.materia}")
                    Text("Semestre: ${materia.semestre}")
                    Text("Calificación: ${materia.calificacion}")
                    Text("Créditos: ${materia.creditos}")
                    Text("Período: ${materia.periodo}")
                }
            }
        }
    }
}
```

### 4. Consultar Calificaciones Finales

```kotlin
// En el ViewModel
fun getCalificacionesFinalesFlow(matricula: String): Flow<List<CalificacionFinal>> {
    return container.localSNRepository.getCalificacionesFinales(matricula)
}

// En el Composable
@Composable
fun CalificacionesFinalesScreen(viewModel: SNViewModel) {
    val calificaciones by viewModel.getCalificacionesFinalesFlow("S21120184")
        .collectAsState(initial = emptyList())
    
    LazyColumn {
        items(calificaciones) { calif ->
            Card {
                Column {
                    Text("${calif.materia}")
                    Text("Grupo: ${calif.grupo}")
                    Text("Calificación: ${calif.calificacion}")
                    Text("Acreditación: ${calif.acreditacion}")
                    Text("Período: ${calif.periodo}")
                }
            }
        }
    }
}
```

---

## 💡 Ejemplos de Código Completo

### Ejemplo 1: Login con Sincronización Automática

```kotlin
class SNViewModel(private val container: AppContainer) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.NotLoggedIn)
    val uiState: StateFlow<UiState> = _uiState
    
    fun login(matricula: String, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                // 1. Hacer login (esto guarda la cookie)
                val result = container.snRepository.accesoObjeto(matricula, password)
                
                if (result.acceso) {
                    // 2. Iniciar sincronización en background
                    container.startSicenetSync(matricula, password)
                    
                    // 3. Observar datos locales
                    container.localSNRepository.getProfile(matricula)
                        .collect { profile ->
                            if (profile != null) {
                                _uiState.value = UiState.Success(profile)
                            }
                        }
                } else {
                    _uiState.value = UiState.Error("Credenciales incorrectas")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class UiState {
    object NotLoggedIn : UiState()
    object Loading : UiState()
    data class Success(val profile: ProfileStudent) : UiState()
    data class Error(val message: String) : UiState()
}
```

### Ejemplo 2: Pantalla con Indicador de Sincronización

```kotlin
@Composable
fun ProfileScreenWithSync(
    viewModel: SNViewModel,
    matricula: String
) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    val profile by viewModel.getProfileFlow(matricula).collectAsState(initial = null)
    
    Column {
        // Indicador de sincronización
        when (syncStatus) {
            is SyncStatus.Syncing -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Sincronizando datos...")
            }
            is SyncStatus.Success -> {
                Text("✓ Datos actualizados", color = Color.Green)
            }
            is SyncStatus.Error -> {
                Text(
                    "⚠ Error: ${(syncStatus as SyncStatus.Error).message}",
                    color = Color.Red
                )
            }
            else -> {}
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mostrar perfil
        profile?.let { ProfileContent(it) }
            ?: Text("Cargando perfil...")
        
        // Botón de sincronización manual
        Button(
            onClick = { viewModel.syncData(matricula, "password") }
        ) {
            Text("Actualizar datos")
        }
    }
}
```

### Ejemplo 3: Sincronización Periódica (Opcional)

```kotlin
// En AppContainer, agregar método para sincronización periódica:
fun startPeriodicSync(matricula: String, password: String) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    
    val inputData = workDataOf(
        SicenetSyncWorker.KEY_MATRICULA to matricula,
        SicenetSyncWorker.KEY_PASSWORD to password
    )
    
    val periodicWork = PeriodicWorkRequestBuilder<SicenetSyncWorker>(
        24, TimeUnit.HOURS // Cada 24 horas
    )
        .setConstraints(constraints)
        .setInputData(inputData)
        .build()
    
    workManager.enqueueUniquePeriodicWork(
        "sicenet_periodic_sync",
        ExistingPeriodicWorkPolicy.KEEP,
        periodicWork
    )
}
```

---

## 📁 Estructura de Archivos Creados/Modificados

```
app/src/main/java/com/example/marsphotos/
│
├── data/
│   ├── AppContainer.kt                    [MODIFICADO]
│   │   ├── localSNRepository: LocalSNRepository
│   │   ├── workManager: WorkManager
│   │   └── startSicenetSync()
│   │
│   ├── SNRepository.kt                    [MODIFICADO]
│   │   ├── getCargaAcademica()
│   │   ├── getKardexConPromedio()
│   │   ├── getCalifUnidades()
│   │   └── getCalifFinal()
│   │
│   ├── LocalSNRepository.kt               [NUEVO]
│   │   ├── getProfile()
│   │   ├── getCargaAcademica()
│   │   ├── getKardex()
│   │   ├── getCalificacionesUnidad()
│   │   └── getCalificacionesFinales()
│   │
│   └── local/
│       ├── SicenetDatabase.kt             [NUEVO]
│       ├── SicenetDao.kt                  [NUEVO]
│       └── entities/
│           └── SicenetEntities.kt         [NUEVO]
│
├── model/
│   └── AcademicData.kt                    [NUEVO]
│       ├── CargaAcademica
│       ├── Kardex
│       ├── CalificacionUnidad
│       └── CalificacionFinal
│
├── network/
│   └── SICENETWService.kt                 [MODIFICADO]
│       ├── getCargaAcademica()
│       ├── getKardexConPromedio()
│       ├── getCalifUnidades()
│       └── getCalifFinal()
│
└── workers/                               [NUEVO]
    ├── SicenetSyncWorker.kt
    └── SaveToLocalDbWorker.kt
```

---

## ⚙️ Configuración Necesaria

### 1. Permisos en AndroidManifest.xml

Ya deberían estar, pero verifica:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 2. WorkManager se inicializa automáticamente

No necesitas inicializarlo manualmente.

---

## 🔍 Debugging

### Ver logs de sincronización:

```bash
# Filtrar por tags
adb logcat -s SicenetSyncWorker SaveToLocalDbWorker SICENET
```

### Inspeccionar base de datos:

```kotlin
// En Android Studio:
// View > Tool Windows > App Inspection > Database Inspector
// Selecciona tu app y verás todas las tablas Room
```

### Consultar estado de Workers:

```kotlin
viewModelScope.launch {
    val workInfos = container.workManager
        .getWorkInfosByTag("sicenet_sync")
        .await()
    
    workInfos.forEach { info ->
        Log.d("Worker", "Estado: ${info.state}")
        Log.d("Worker", "Output: ${info.outputData}")
    }
}
```

---

## ✅ Checklist de Implementación

- [x] Room database creada
- [x] DAOs implementados
- [x] Repositorio local creado
- [x] Workers de sincronización creados
- [x] AppContainer actualizado
- [x] Nuevos endpoints SOAP agregados
- [x] Modelos de datos creados
- [ ] SNViewModel actualizado (próximo paso)
- [ ] UI actualizada para mostrar datos (próximo paso)
- [ ] Indicadores de sincronización en UI (próximo paso)

---

## 🎓 Conceptos Clave

### Room
- **Entity**: Clase que representa una tabla de base de datos
- **DAO**: Interface con métodos para acceder a los datos
- **Database**: Clase abstracta que contiene todos los DAOs

### WorkManager
- **Worker**: Clase que ejecuta trabajo en background
- **WorkRequest**: Solicitud para ejecutar un Worker
- **Chain**: Secuencia de Workers que se ejecutan en orden

### Repository Pattern
- **Remote Repository**: Consulta datos de la red
- **Local Repository**: Consulta datos de la base de datos local
- **ViewModel**: Decide de dónde obtener los datos (remoto o local)

---

¡Todo listo para integrar con la UI! 🎉
