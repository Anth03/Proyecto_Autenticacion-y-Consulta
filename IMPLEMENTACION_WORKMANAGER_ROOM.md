# Implementación de Sincronización de Datos SICENET con WorkManager y Room

## Resumen de la Implementación

Se ha implementado exitosamente un sistema completo de sincronización de datos desde el servicio web SICENET hacia una base de datos local usando **Room** y **WorkManager**, siguiendo la arquitectura Repository.

---

## 📋 Punto 0: Nuevos Endpoints SOAP Implementados

### Archivos Modificados:
- `network/SICENETWService.kt`

### Nuevos Métodos Agregados:

1. **getCargaAcademica()** - Consulta la carga académica del alumno
   - Template SOAP: `bodyCargaAcademica`
   - Endpoint: `getCargaAcademicaByAlumno`

2. **getKardexConPromedio(lineamiento)** - Consulta el kardex completo con promedio
   - Template SOAP: `bodyKardexConPromedio`
   - Endpoint: `getAllKardexConPromedioByAlumno`
   - Parámetro: `aluLineamiento` (Int)

3. **getCalifUnidades()** - Consulta calificaciones por unidad
   - Template SOAP: `bodyCalifUnidades`
   - Endpoint: `getCalifUnidadesByAlumno`

4. **getCalifFinal(modEducativo)** - Consulta calificaciones finales
   - Template SOAP: `bodyCalifFinal`
   - Endpoint: `getAllCalifFinalByAlumnos`
   - Parámetro: `bytModEducativo` (Int)

---

## 🗄️ Punto 1: Capa de Repository Local con Room

### Estructura de Base de Datos

#### Archivos Creados:

1. **`data/local/entities/SicenetEntities.kt`** - Entidades de Room
   - `ProfileStudentEntity` - Perfil del estudiante
   - `CargaAcademicaEntity` - Materias actuales
   - `KardexEntity` - Historial académico
   - `CalificacionUnidadEntity` - Calificaciones por unidad
   - `CalificacionFinalEntity` - Calificaciones finales

2. **`data/local/SicenetDao.kt`** - DAOs (Data Access Objects)
   - `ProfileStudentDao`
   - `CargaAcademicaDao`
   - `KardexDao`
   - `CalificacionUnidadDao`
   - `CalificacionFinalDao`

3. **`data/local/SicenetDatabase.kt`** - Base de datos Room
   - Versión: 1
   - Singleton pattern
   - Contiene todas las tablas y DAOs

4. **`data/LocalSNRepository.kt`** - Repositorio local
   - Métodos para insertar y consultar datos
   - Funciones de mapeo entre entidades y modelos
   - Usa Flow para observar cambios en tiempo real

5. **`model/AcademicData.kt`** - Modelos de datos
   - `CargaAcademica`
   - `Kardex`
   - `KardexConPromedio`
   - `CalificacionUnidad`
   - `CalificacionFinal`

### Características del Repositorio Local:

✅ **CRUD completo** para todas las entidades
✅ **Flow reactivo** - Los datos se actualizan automáticamente en la UI
✅ **Mapeo automático** entre Room Entities y modelos de dominio
✅ **Timestamp** en cada registro para saber cuándo se sincronizó

---

## ⚙️ Punto 2: WorkManager - Sincronización de Datos

### Arquitectura de Workers

Se implementó una **cadena de 2 workers** que se ejecutan secuencialmente:

#### Worker 1: **SicenetSyncWorker**
- **Archivo**: `workers/SicenetSyncWorker.kt`
- **Propósito**: Consultar datos del servicio web de SICENET

**Flujo de ejecución:**
1. Recibe matrícula y contraseña como entrada
2. Hace login en SICENET
3. Consulta perfil académico
4. Consulta carga académica
5. Consulta kardex (usa lineamiento del perfil)
6. Consulta calificaciones por unidad
7. Consulta calificaciones finales (usa modEducativo del perfil)
8. Retorna todos los datos como JSON en los datos de salida

**Datos de Entrada:**
```kotlin
KEY_MATRICULA: String
KEY_PASSWORD: String
```

**Datos de Salida:**
```kotlin
KEY_MATRICULA: String
KEY_PROFILE_JSON: String (JSON del perfil)
KEY_CARGA_JSON: String (JSON de carga académica)
KEY_KARDEX_JSON: String (JSON del kardex)
KEY_CALIF_UNIDAD_JSON: String (JSON de calificaciones por unidad)
KEY_CALIF_FINAL_JSON: String (JSON de calificaciones finales)
```

#### Worker 2: **SaveToLocalDbWorker**
- **Archivo**: `workers/SaveToLocalDbWorker.kt`
- **Propósito**: Almacenar datos en la base de datos local

**Flujo de ejecución:**
1. Recibe los JSON del Worker 1
2. Parsea cada JSON a objetos Kotlin
3. Guarda el perfil en Room
4. Guarda la carga académica (si hay datos)
5. Guarda el kardex (si hay datos)
6. Guarda las calificaciones por unidad (si hay datos)
7. Guarda las calificaciones finales (si hay datos)
8. Maneja errores gracefully (continúa aunque falle una inserción)

**Manejo de Errores:**
- Si falla el guardado de un tipo de dato, se registra un warning pero continúa con los demás
- Solo falla si no se puede guardar el perfil (dato crítico)

### Configuración de WorkManager

#### En AppContainer:

```kotlin
override fun startSicenetSync(matricula: String, password: String): Operation {
    // Constraints: Solo con internet
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    
    // Worker 1: Consulta API
    val syncWorkRequest = OneTimeWorkRequestBuilder<SicenetSyncWorker>()
        .setConstraints(constraints)
        .setInputData(inputData)
        .addTag("sicenet_sync")
        .build()
    
    // Worker 2: Guarda en BD
    val saveWorkRequest = OneTimeWorkRequestBuilder<SaveToLocalDbWorker>()
        .addTag("sicenet_save")
        .build()
    
    // Encadenar: sync -> save
    return workManager
        .beginUniqueWork("sicenet_sync_chain", ExistingWorkPolicy.REPLACE, syncWorkRequest)
        .then(saveWorkRequest)
        .enqueue()
}
```

### Características de la Sincronización:

✅ **Trabajo único** - `ExistingWorkPolicy.REPLACE` evita duplicados
✅ **Requiere internet** - Solo se ejecuta con conexión
✅ **Monitoreable** - Retorna `Operation` para observar el estado
✅ **Encadenamiento** - Los datos fluyen del Worker 1 al Worker 2
✅ **Resiliente** - Maneja errores sin romper la cadena

---

## 📦 Dependencias Agregadas

### build.gradle.kts

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.10-1.0.13"
}

dependencies {
    // Room
    val room_version = "2.6.0"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    
    // WorkManager (ya existía)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

---

## 🏗️ Arquitectura Implementada

```
┌─────────────────────────────────────────────────────────┐
│                    MarsPhotosApp                         │
│                   (Jetpack Compose)                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
          ┌──────────────────────┐
          │     SNViewModel      │
          │    (ViewModel)       │
          └──────────┬───────────┘
                     │
                     ▼
          ┌──────────────────────┐
          │     AppContainer     │
          │   (DI Container)     │
          └──┬───────────────┬───┘
             │               │
             ▼               ▼
   ┌─────────────────┐  ┌─────────────────┐
   │  SNRepository   │  │ LocalSNRepository│
   │   (Network)     │  │    (Room)       │
   └────────┬────────┘  └────────┬────────┘
            │                     │
            ▼                     ▼
   ┌─────────────────┐  ┌─────────────────┐
   │ SICENETWService │  │ SicenetDatabase │
   │   (Retrofit)    │  │     (Room)      │
   └─────────────────┘  └─────────────────┘

         WorkManager Chain:
   ┌─────────────────────────┐
   │  SicenetSyncWorker     │
   │  (Consulta API)        │
   └───────────┬─────────────┘
               │ Output Data (JSON)
               ▼
   ┌─────────────────────────┐
   │  SaveToLocalDbWorker   │
   │  (Guarda en Room)      │
   └─────────────────────────┘
```

---

## 🔄 Flujo de Sincronización Completo

### Caso de Uso: Usuario hace login con internet

1. **Usuario ingresa credenciales** en `LoginScreen`
2. **SNViewModel** llama a `login(matricula, password)`
3. **AppContainer** inicia la cadena de Workers:
   ```kotlin
   container.startSicenetSync(matricula, password)
   ```
4. **SicenetSyncWorker** (Worker 1):
   - Se ejecuta solo si hay internet
   - Hace login en SICENET
   - Consulta todos los datos
   - Retorna JSON en los datos de salida
   
5. **SaveToLocalDbWorker** (Worker 2):
   - Recibe los JSON del Worker 1
   - Parsea y guarda en Room
   - Datos disponibles localmente

6. **UI se actualiza** automáticamente gracias a Flow

### Caso de Uso: Usuario sin internet

- Los Workers no se ejecutan (constraint de red)
- La app puede consultar datos de Room (LocalSNRepository)
- Los datos están disponibles offline

---

## 📊 Modelos de Datos

### ProfileStudent
- matricula, nombre, carrera, especialidad
- semActual, cdtosAcumulados, cdtosActuales
- lineamiento, modEducativo, estatus
- fechaReins, inscrito, adeudo

### CargaAcademica
- clvOficial, materia, grupo
- creditos, docente, observaciones
- estadoMateria, semestre

### Kardex
- clvOficial, materia, semestre
- creditos, calificacion, acreditacion
- periodo, observaciones

### CalificacionUnidad
- clvOficial, materia, unidad
- calificacion, fecha, observaciones

### CalificacionFinal
- clvOficial, materia, grupo
- calificacion, acreditacion, periodo
- creditos, observaciones

---

## ✅ Cumplimiento de Requerimientos

### ✅ Punto 0: Consulta de nueva información
- ✔️ Carga académica
- ✔️ Kardex con promedio
- ✔️ Calificaciones por unidad
- ✔️ Calificaciones finales

### ✅ Punto 1: Repository local con Room
- ✔️ Base de datos SQLite con Room
- ✔️ 5 tablas (perfil, carga, kardex, calif. unidad, calif. final)
- ✔️ DAOs para cada tabla
- ✔️ Repositorio local con CRUD completo
- ✔️ Flow reactivo para observar cambios

### ✅ Punto 2: Sincronización con WorkManager
- ✔️ Worker 1: Consulta SICENET
- ✔️ Worker 2: Guarda en Room
- ✔️ Encadenamiento de workers
- ✔️ Datos de salida del Worker 1 son entrada del Worker 2
- ✔️ Constraint de red (solo con internet)
- ✔️ Trabajo único (evita duplicados)
- ✔️ Monitoreable (retorna Operation)

---

## 🎯 Próximos Pasos Sugeridos

1. **Actualizar SNViewModel** para:
   - Iniciar la sincronización automáticamente al hacer login
   - Observar el estado del WorkManager
   - Consultar datos de Room cuando no hay internet

2. **Actualizar UI** para:
   - Mostrar indicador de sincronización
   - Mostrar datos de carga académica, kardex, etc.
   - Indicar si los datos son de cache o recién sincronizados

3. **Implementar lógica de actualización**:
   - Sincronizar periódicamente (PeriodicWorkRequest)
   - Botón de "Actualizar" manual
   - Política de caducidad de datos (ej: sincronizar cada 24 horas)

---

## 📝 Notas Técnicas

- **Room Database**: Versión 1, con `fallbackToDestructiveMigration()` activado
- **Serialización**: Usa Kotlin Serialization para JSON
- **Concurrencia**: Todo usa coroutines (suspend functions)
- **Reactive**: Flow para observar cambios en tiempo real
- **Error Handling**: Logs extensivos con Log.d() y Log.e()
- **Timestamps**: Cada entidad tiene `lastUpdated` para tracking

---

Fecha de implementación: 15 de Febrero de 2026
