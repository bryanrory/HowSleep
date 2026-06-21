# HowSleep — Diretrizes do Repositório para Claude Code

> Este arquivo é lido automaticamente pelo Claude Code em toda conversa neste repositório.
> Ele define as regras arquiteturais, padrões de código e contexto do produto que devem
> ser seguidos sem exceção durante a implementação.

---

## Contexto do Produto

**HowSleep** é um Monitor de Sono Inteligente e Comportamental para Android nativo.

O diferencial não é medir o sono — é **investigar**: cruzar hábitos pré-sono (cafeína, telas, estresse, alimentação, exercício) com a qualidade objetiva do sono (Android Sleep API) e o estado subjetivo ao acordar (mood, energia), gerando desafios personalizados via IA e validando matematicamente se as mudanças funcionaram.

**Documentação de referência (leia antes de implementar qualquer feature):**
- `docs/PRD.md` — regras de negócio, jornadas do usuário, algoritmo de avaliação de desafio
- `docs/DATA_CONTRACTS.md` — schema Room detalhado, exemplos de payload LLM
- `docs/STATE_MACHINE.md` — 9 estados de uma noite, 8 cenários de conflito
- `docs/ARCHITECTURE_GUIDE.md` — padrões MVVM, Hilt, Workers, navegação, testes

**Blueprint arquitetural:** `C:\Users\bryan.ferreira\.claude\plans\claude-quero-criar-um-groovy-bubble.md`

---

## Stack Tecnológica (fixada — não alterar sem aprovação)

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitetura | MVVM + StateFlow (não MVI) |
| DI | Hilt |
| Banco local | Room + KSP |
| Background | WorkManager + Hilt WorkManager integration |
| HTTP | Retrofit + OkHttp + kotlinx.serialization |
| Estado leve | DataStore Preferences |
| Sensor de sono | Android Sleep API (ActivityRecognition) |
| Gráficos | Vico (Fase 4 apenas) |
| Módulos | Single `:app` module (não multi-módulo no MVP) |

---

## Regras Arquiteturais Estritas

### R-01: Chave de JOIN por `sleep_epoch_day`

A chave natural que une todas as tabelas de uma mesma noite é `sleep_epoch_day: Long` (epoch day UTC do dia local em que o sono **começa**). Nunca use FK nullable entre as tabelas de log.

```kotlin
// CORRETO: JOIN por valor igual
SELECT * FROM sleep_session s
LEFT JOIN pre_sleep_log pr ON s.sleep_epoch_day = pr.sleep_epoch_day
LEFT JOIN post_sleep_log po ON s.sleep_epoch_day = po.sleep_epoch_day

// ERRADO: FK nullable que exige UPDATE retroativo
pre_sleep_log.session_id: Long?  // ← nunca fazer isso
```

**Cálculo de `sleep_epoch_day`:** Sempre via `TimeUtils.resolveSleepEpochDay(utcMs, timezoneId)`. Regra: hora local ≥ 00:00 e < 04:00 → epoch day do dia anterior. Hora local ≥ 04:00 → epoch day do dia atual. Para pós-sono: horário < 12:00 → noite anterior.

### R-02: Timestamps sempre em UTC, timezone_id sempre armazenado

```kotlin
// CORRETO
val entity = PreSleepLogEntity(
    filledAtUtcMs = System.currentTimeMillis(),    // UTC
    timezoneId = TimeUtils.currentTimezoneId(),    // "America/Sao_Paulo"
    caffeineLastIntakeLocalHour = TimeUtils.localHourFromUtcMs(utcMs, timezoneId)  // derivado e armazenado
)

// ERRADO: converter para local antes de persistir
val localTime = LocalDateTime.now()  // ← nunca usar como timestamp persistido
```

Campos `_local_hour` são calculados no **momento da escrita** e armazenados como `Int`. Queries SQL consultam o valor armazenado — nunca recalculam.

### R-03: DAOs de log usam REPLACE, não INSERT

```kotlin
// pre_sleep_log e post_sleep_log: REPLACE para permitir re-submissão no mesmo dia
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsert(entity: PreSleepLogEntity): Long

// sleep_session: também REPLACE — Sleep API pode emitir múltiplos eventos para a mesma noite
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsert(entity: SleepSessionEntity): Long
```

### R-04: `SleepReceiver` é ultraleve — nenhum IO

```kotlin
// CORRETO: apenas captura intent e enfileira Worker
@AndroidEntryPoint
class SleepReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val workRequest = OneTimeWorkRequestBuilder<SleepSessionWorker>()
            .setInputData(workDataOf("events_json" to intent.toEventsJson()))
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

// ERRADO: qualquer DAO, repository ou operação de IO aqui
class SleepReceiver : BroadcastReceiver() {
    @Inject lateinit var sleepDao: SleepSessionDao  // ← nunca injetar DAOs no Receiver
    override fun onReceive(...) {
        sleepDao.insert(...)  // ← PROIBIDO — risco de ANR e perda de evento
    }
}
```

Todo processamento ocorre no `SleepSessionWorker` (`@HiltWorker`).

### R-05: Workers retornam `Result.success()` — nunca `Result.failure()`

```kotlin
// Workers tratam falhas internamente com fallback — nunca falham para o WorkManager
override suspend fun doWork(): Result {
    apiRepository.call()
        .onSuccess { return Result.success() }
        .onFailure { /* fallback */ }

    localEngine.analyze()
        ?.let { return Result.success() }

    staticProvider.getNext()
    return Result.success()   // sempre sucesso — falhas são tratadas com fallback

    // EXCEÇÃO: SQLiteException pode retornar Result.retry()
}
```

### R-06: `domain/` e `ai/` são zero-Android

Nenhum arquivo em `domain/` ou `ai/` pode importar `android.*` ou `androidx.*`. Estas classes são testáveis com JUnit puro sem Robolectric.

```kotlin
// CORRETO — em domain/util/TimeUtils.kt
import java.time.ZoneId          // ✓ Java stdlib
import java.time.LocalDate       // ✓ Java stdlib

// ERRADO
import android.content.Context   // ✗ proibido em domain/
import androidx.room.Entity      // ✗ proibido em domain/
```

### R-07: Repository retorna `Result<T>`, nunca lança exceções

```kotlin
// CORRETO
override suspend fun savePostSleepLog(...): Result<Unit> = runCatching {
    postSleepLogDao.upsert(buildEntity(...))
}

// ERRADO
override suspend fun savePostSleepLog(...) {
    try {
        postSleepLogDao.upsert(...)
    } catch (e: Exception) {
        throw RuntimeException("Falha ao salvar", e)  // ← nunca relançar
    }
}
```

### R-08: ViewModel nunca expõe `MutableStateFlow`

```kotlin
// CORRETO
private val _uiState = MutableStateFlow(XxxUiState())
val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()

// ERRADO
val uiState = MutableStateFlow(XxxUiState())  // ← mutável exposto
```

### R-09: Composables recebem callbacks de navegação, nunca `NavController`

```kotlin
// CORRETO
@Composable
fun PreSleepScreen(
    onNavigateToDashboard: () -> Unit,   // ← lambda de navegação
    viewModel: PreSleepViewModel = hiltViewModel()
)

// ERRADO
@Composable
fun PreSleepScreen(
    navController: NavController   // ← nunca passar NavController para Composable
)
```

### R-10: `MockSleepEventSource` apenas em `BuildConfig.DEBUG`

```kotlin
// SleepModule.kt
return if (BuildConfig.DEBUG && useMock) {
    MockSleepEventSource()
} else {
    RealSleepEventSource(context)
}
// A condição BuildConfig.DEBUG deve SEMPRE estar presente — nunca apenas useMock
```

---

## Padrão de UI Reativa (MVVM + StateFlow)

### Trio obrigatório por tela: UiState + ViewModel + Composable

```kotlin
// 1. UiState — data class com valores padrão
data class XxxUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,       // dispara navegação quando true
    val errorMessage: String? = null    // null = sem erro
)

// 2. ViewModel
@HiltViewModel
class XxxViewModel @Inject constructor(
    private val repository: XxxRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(XxxUiState())
    val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()

    fun onXxxChanged(value: Xxx) = _uiState.update { it.copy(xxx = value) }

    fun submit() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.save(...)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isSaved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = "Erro. Tente novamente.") } }
        }
    }
}

// 3. Composable
@Composable
fun XxxScreen(onNavigateNext: () -> Unit, viewModel: XxxViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()  // ← sempre WithLifecycle

    LaunchedEffect(uiState.isSaved) {      // ← navegação reativa, executa uma vez
        if (uiState.isSaved) onNavigateNext()
    }
    // ...
}
```

### Regras de `Flow` em DAOs

- Queries que alimentam a UI → `Flow<T>` no DAO (Room notifica automaticamente)
- Queries em Workers/use-cases → `suspend fun` que retorna o valor diretamente
- ViewModel converte `Flow` de Room em `StateFlow` via `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())`

---

## Padrão de Injeção Hilt

### Nomenclatura de módulos

| Arquivo | `@InstallIn` | Conteúdo |
|---|---|---|
| `DatabaseModule.kt` | `SingletonComponent` | `HowSleepDatabase` + 5 DAOs |
| `NetworkModule.kt` | `SingletonComponent` | `OkHttpClient`, `Json`, `Retrofit`, `AiChallengeApi` |
| `SleepModule.kt` | `SingletonComponent` | `SleepEventSource` (real ou mock) |
| `RepositoryModule.kt` | `SingletonComponent` | `@Binds` para as 3 interfaces de Repository |
| `WorkerModule.kt` | `SingletonComponent` | `HiltWorkerFactory` binding |

### Padrão obrigatório para Workers com DI

```kotlin
@HiltWorker
class NomeWorker @AssistedInject constructor(
    @Assisted context: Context,             // ← @Assisted obrigatório
    @Assisted workerParams: WorkerParameters,  // ← @Assisted obrigatório
    private val repository: AlgumRepository    // ← injeção normal
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result { ... }
}
```

### `HowSleepApplication.kt` implementa `Configuration.Provider`

```kotlin
@HiltAndroidApp
class HowSleepApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    override fun getWorkManagerConfiguration() =
        Configuration.Builder().setWorkerFactory(workerFactory).build()
}
```

---

## Estrutura de Pacotes

```
com.howsleep.app/
├── HowSleepApplication.kt
├── MainActivity.kt
├── navigation/          NavGraph.kt, Screen.kt
├── data/
│   ├── db/             HowSleepDatabase.kt, entity/, dao/, converter/
│   ├── remote/         api/, dto/, mapper/
│   ├── repository/     interfaces + impl
│   └── di/             DatabaseModule, NetworkModule, SleepModule, RepositoryModule
├── domain/             model/, util/TimeUtils.kt    ← zero imports Android
├── ai/                 PromptBuilder, AiResponseSanitizer, ChallengeValidator,
│                       LocalInsightEngine, StaticChallengeProvider   ← zero imports Android
├── sleep/              SleepEventSource (interface), Real/MockImpl, SleepApiManager,
│                       SleepReceiver (ultraleve), SleepSessionWorker
├── worker/             AiCallWorker, ChallengeEvaluationWorker, NightlyEvaluationWorker,
│                       PreSleepReminderWorker, di/WorkerModule
├── notification/       NotificationHelper, ReminderScheduler
└── ui/
    ├── theme/
    ├── presleep/       Screen + ViewModel + UiState
    ├── postsleep/
    ├── dashboard/
    ├── challenge/
    ├── trends/
    ├── settings/
    └── components/     SleepQualityCard, ScoreSlider, HabitChip, ChallengeCard
```

---

## Fases de Implementação

### Fase 1 — Foundation ✅ CONCLUÍDA
Objetivo: app compila, navega, Room existe com todas as 5 entities, Hilt fiado, mock de sleep disponível.

**Commit:** `15b5c63` + `2930c0c` + `b81febb`

### Fase 2 — Core Loop ✅ CONCLUÍDA
SleepReceiver → SleepSessionWorker → formulários Pré/Pós-Sono → Dashboard 7 noites.

**Commits:** `f5a0f87`, `ca5cad9`, `3d84e6c`, `0b6e0a6`

### Fase 3 — AI Integration ✅ CONCLUÍDA
PromptBuilder → AiCallWorker → LocalInsightEngine → StaticChallengeProvider → ChallengeScreen.

**Commit:** `aa8f098`

**O que foi implementado:**
- `domain/NightStateComputer.kt` — 9 estados + máquina de estados pura
- `ai/ChallengeSuggestion.kt`, `AiChallengeResponse.kt` — data classes bridge e serialização
- `ai/PromptBuilder.kt` — gera prompt user-content a partir de NightDataAggregate (zero Android)
- `ai/AiResponseSanitizer.kt` — extrai JSON limpo da resposta do LLM
- `ai/ChallengeValidator.kt` — valida campos com `require()` + trunca strings longas
- `ai/LocalInsightEngine.kt` — engine de correlação hábito×sono (zero Android)
- `ai/StaticChallengeProvider.kt` — 7 desafios hardcoded, seleção por `seed % 7`
- `data/remote/api/AiChallengeApi.kt` — interface Retrofit `@POST v1/messages`
- `data/remote/dto/` — `AiRequestDto`, `AiResponseDto`, `AiMessageDto`, `AiContentBlockDto`
- `data/remote/mapper/AiResponseMapper.kt` — DTO → Entity com cálculo de `validUntilEpochDay`
- `worker/AiCallWorker.kt` — fallback chain: API → LocalInsightEngine → StaticProvider
- `worker/ChallengeEvaluationWorker.kt` — avalia hábito + outcome; finaliza desafio no último dia
- `ui/challenge/` — `ChallengeUiState` + `ChallengeViewModel` + `ChallengeScreen` (badge de origem, progresso, dialog de abandono)
- `navigation/NavGraph.kt` — rota Challenge conectada ao ícone ⭐ no Dashboard

### Fase 4 — Polish ✅ CONCLUÍDA
Mergeada em `main`. Branch: `feat/fase-4-polish` — Commits: `48d4402`…`0b70fc0`

**O que foi implementado:**
- `notification/NotificationHelper.kt` — canal `howsleep_reminders` + `sendPreSleepReminder()` + `sendPostSleepFollowUpReminder()`
- `notification/ReminderScheduler.kt` — `schedulePreSleepReminder(h, m)` + `scheduleNightlyEvaluation()` às 06h + `schedulePostSleepFollowUp()` às 14h
- `worker/PreSleepReminderWorker.kt` — @HiltWorker, envia notificação pré-sono
- `worker/NightlyEvaluationWorker.kt` — @HiltWorker, finaliza desafios expirados sem avaliação
- `worker/PostSleepFollowUpWorker.kt` — @HiltWorker, verifica se pós-sono de ontem foi preenchido; envia notificação se não (PRD LF-02)
- `HowSleepApplication.kt` — injeta `NotificationHelper` e cria canais no `onCreate()`
- `MainActivity.kt` — injeta `ReminderScheduler`; solicita `POST_NOTIFICATIONS` em runtime (Android 13+); agenda avaliação noturna e follow-up pós-sono
- `ui/settings/` — time picker Material3 para horário do lembrete (SettingsUiState + ViewModel + Screen)
- `ui/trends/` — TrendsUiState + TrendsViewModel + TrendsScreen com gráficos Vico (duração + qualidade, 7/30 dias)
- `ui/challenge/ChallengeHistoryScreen.kt` — lista desafios COMPLETED/ABANDONED/EXPIRED com status chip + seção de delta (Base X → Resultado Y, ±Z%)
- `data/db/entity/AiChallengeEntity.kt` — campos `outcomeAverage: Float?` e `outcomeDeltaPercent: Float?` para veredicto final
- `data/db/HowSleepDatabase.kt` — versão 2 com `MIGRATION_1_2` explícita (2 colunas via ALTER TABLE)
- `data/db/dao/AiChallengeDao.kt` — query `getHistory()` para não-ACTIVE
- `data/repository/AiChallengeRepository.kt` — `getChallengeHistory(): Flow<List<AiChallengeEntity>>`
- `worker/ChallengeEvaluationWorker.kt` — `finalizeChallenge()` calcula `outcomeAverage` e `outcomeDeltaPercent` e persiste (PRD 4.4)
- `navigation/` — rotas `ChallengeHistory` e `Trends` adicionadas; ícones ShowChart e History no Dashboard TopAppBar
- `libs.versions.toml` + `build.gradle.kts` — Vico 2.0.1 habilitado + `material-icons-extended`

**Notas de implementação:**
- `ReminderScheduler` é injetado na `MainActivity` (não na `Application`) para evitar dependência circular com `WorkManager.getInstance()` antes do `workerFactory` estar disponível
- `NotificationHelper` não depende de WorkManager — pode ser injetado diretamente na `Application`
- `outcomeDeltaPercent` é positivo quando o resultado melhora na direção esperada (tanto ABOVE quanto BELOW)
- `DatabaseModule` agora usa `addMigrations(MIGRATION_1_2)` em vez de `fallbackToDestructiveMigration()`

---

## Comandos Gradle

```bash
# Build completo (debug)
./gradlew assembleDebug

# Build de release
./gradlew assembleRelease

# Rodar todos os testes unitários (JVM — sem dispositivo)
./gradlew test

# Rodar testes de instrumentação (requer dispositivo/emulador conectado)
./gradlew connectedAndroidTest

# Rodar apenas os testes de uma classe específica
./gradlew test --tests "com.howsleep.app.ai.PromptBuilderTest"

# Verificar se o Room compila as entities corretamente (KSP)
./gradlew kspDebugKotlin

# Verificar Lint
./gradlew lintDebug

# Limpar build cache
./gradlew clean

# Build + testes em sequência
./gradlew clean assembleDebug test
```

**Nota Windows:** Use `gradlew.bat` no lugar de `./gradlew` se executando fora do Git Bash/WSL.

---

## Tratamento de Erros — Convenção Resumida

| Camada | Padrão | Notas |
|---|---|---|
| Repository | `runCatching { }` → `Result<T>` | Nunca relança exceções |
| ViewModel | `.fold(onSuccess, onFailure)` | Atualiza `uiState.errorMessage` |
| Worker | Fallback chain dentro de `doWork()` | Retorna `Result.success()` sempre (exceto `SQLiteException` → `.retry()`) |
| DAO | `@Insert(onConflict = REPLACE)` para logs | Nunca deixar o Room lançar `SQLiteConstraintException` por conflito esperado |
| Retrofit | `HttpException` e `IOException` tratados no Repository | Nunca propagar para ViewModel |

---

## Variáveis de Ambiente e Segredos

- Chave da API do LLM: definida em `local.properties` como `LLM_API_KEY=sk-...`
- Exposta via `BuildConfig.LLM_API_KEY` no `app/build.gradle.kts`
- `local.properties` está no `.gitignore` — **nunca commitar segredos**
- Para produção: migrar para proxy backend antes de qualquer release público

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "LLM_API_KEY", "\"${localProperties["LLM_API_KEY"]}\"")
    }
}
```

---

## ENUMs do Domínio (valores fixos — não alterar sem migração)

```
habit_to_change:       CAFFEINE | SCREEN_TIME | STRESS | MEAL_TIMING | EXERCISE | ALCOHOL | SLEEP_SCHEDULE
success_metric_type:   SLEEP_DURATION | MOOD_SCORE | ENERGY_LEVEL | PERCEIVED_QUALITY
success_metric_direction: ABOVE | BELOW
ai_challenge.source:   AI_API | LOCAL_ENGINE | STATIC_DEFAULT
ai_challenge.status:   ACTIVE | COMPLETED | ABANDONED | EXPIRED
sleep_session.source:  SLEEP_API | MANUAL
last_meal_type:        LIGHT | HEAVY | SNACK
exercise_intensity:    LOW | MODERATE | HIGH
```

Todos armazenados como `String` no Room (não como `Int`) para legibilidade e segurança em migrações.

---

## Convenções de Git

### Branches
- Todo desenvolvimento ativo ocorre em branch separada — **nunca commitar código diretamente em `main`**
- Nomenclatura: `feat/fase-1-foundation`, `feat/fase-2-core-loop`, `fix/nome-do-bug`
- `main` recebe apenas merges de fases concluídas ou correções críticas

### Granularidade de commits — uma mudança lógica por commit
- Nova tela criada + bug corrigido em outra → **dois commits separados**
- Três entities Room criadas juntas → um único commit (mesma unidade lógica)
- Nunca misturar feature + refactor + bugfix no mesmo commit

### Prefixos obrigatórios nas mensagens
| Prefixo | Quando usar |
|---|---|
| `feat:` | nova funcionalidade |
| `fix:` | correção de bug |
| `chore:` | configuração, dependências, build |
| `docs:` | atualização de documentação |
| `refactor:` | refatoração sem mudança de comportamento |
| `test:` | adição ou correção de testes |

**Nunca** adicionar `Co-Authored-By:` ou qualquer metadado de ferramenta nas mensagens.

### Push
- Ao final de cada sessão de trabalho ou feature concluída
- Nunca `--force` em `main`

---

## Documentação como fonte de verdade

Antes de implementar qualquer coisa, ler os documentos relevantes:
- `CLAUDE.md` — regras arquiteturais e checklist
- `docs/PRD.md` — regras de negócio e jornadas
- `docs/DATA_CONTRACTS.md` — schema do banco e payloads de IA
- `docs/STATE_MACHINE.md` — estados de uma noite e cenários de conflito
- `docs/ARCHITECTURE_GUIDE.md` — padrões de código

**Se a implementação divergir do que está documentado → atualizar o doc no mesmo commit ou no imediatamente seguinte. Nunca deixar dívida de documentação acumulando.**

---

## Checklist de Revisão antes de cada Commit

- [ ] Nenhum arquivo em `domain/` ou `ai/` importa `android.*` ou `androidx.*`
- [ ] Todo timestamp novo usa `System.currentTimeMillis()` (UTC) com `timezoneId` salvo junto
- [ ] `sleep_epoch_day` calculado via `TimeUtils.resolveSleepEpochDay()` — nunca inline
- [ ] DAOs de log usam `@Insert(onConflict = OnConflictStrategy.REPLACE)`
- [ ] `SleepReceiver.onReceive()` não contém nenhuma operação de IO
- [ ] Workers retornam `Result.success()` (exceto retry para SQLiteException)
- [ ] `MockSleepEventSource` está sob `BuildConfig.DEBUG &&` no módulo Hilt
- [ ] Nenhum `NavController` passado como parâmetro para Composables
- [ ] Composables usam `collectAsStateWithLifecycle()` (não `collectAsState()`)
- [ ] Nenhuma chave de API hardcoded no código-fonte
