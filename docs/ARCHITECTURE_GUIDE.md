# ARCHITECTURE_GUIDE — Guia de Arquitetura e Padrões de Código
## HowSleep: Monitor de Sono Inteligente e Comportamental

**Versão:** 1.0  
**Data:** 2026-06-20

---

## 1. Estrutura de Pacotes

```
com.howsleep.app/
├── HowSleepApplication.kt          (@HiltAndroidApp — ponto de entrada do Hilt)
├── MainActivity.kt                  (single Activity, host do NavHost)
│
├── navigation/
│   ├── NavGraph.kt                  (NavHost + todas as rotas)
│   └── Screen.kt                    (sealed class com as rotas como strings)
│
├── data/
│   ├── db/
│   │   ├── HowSleepDatabase.kt      (Room @Database, lista todas as entities)
│   │   ├── entity/                  (5 @Entity classes)
│   │   ├── dao/                     (5 @Dao interfaces)
│   │   └── converter/
│   │       └── DateConverters.kt    (@TypeConverter Long ↔ LocalDate)
│   ├── remote/
│   │   ├── api/
│   │   │   └── AiChallengeApi.kt   (interface Retrofit)
│   │   ├── dto/
│   │   │   ├── AiRequestDto.kt
│   │   │   └── AiResponseDto.kt
│   │   └── mapper/
│   │       └── AiResponseMapper.kt  (DTO → Entity)
│   ├── repository/
│   │   ├── SleepRepository.kt       (interface + impl)
│   │   ├── HabitLogRepository.kt    (interface + impl)
│   │   └── AiChallengeRepository.kt (interface + impl)
│   └── di/
│       ├── DatabaseModule.kt
│       ├── NetworkModule.kt
│       ├── RepositoryModule.kt
│       └── SleepModule.kt
│
├── domain/
│   ├── model/
│   │   └── NightDataAggregate.kt   (data class puro — sem imports Android)
│   ├── util/
│   │   └── TimeUtils.kt            (funções puras de timezone)
│   └── usecase/                    (opcional — apenas para Fase 4)
│
├── ai/
│   ├── PromptBuilder.kt             (função pura: List<NightDataAggregate> → String JSON)
│   ├── AiResponseSanitizer.kt       (função pura: String → String limpa)
│   ├── ChallengeValidator.kt        (valida campos do JSON parseado)
│   ├── LocalInsightEngine.kt        (heurísticas locais — sem Android imports)
│   └── StaticChallengeProvider.kt   (desafios estáticos round-robin)
│
├── sleep/
│   ├── SleepEventSource.kt          (interface)
│   ├── RealSleepEventSource.kt      (ActivityRecognitionClient)
│   ├── MockSleepEventSource.kt      (para DEBUG)
│   ├── MockSleepWorker.kt           (Worker que emite sessão sintética)
│   ├── SleepApiManager.kt           (delega para SleepEventSource)
│   ├── SleepReceiver.kt             (@AndroidEntryPoint BroadcastReceiver — ULTRALEVE)
│   └── SleepSessionWorker.kt        (@HiltWorker — todo o processamento aqui)
│
├── worker/
│   ├── AiCallWorker.kt              (@HiltWorker — chama API + fallback)
│   ├── ChallengeEvaluationWorker.kt (@HiltWorker — avalia hábito + outcome diariamente)
│   ├── NightlyEvaluationWorker.kt   (@HiltWorker — avaliação final do desafio)
│   ├── PreSleepReminderWorker.kt    (@HiltWorker — envia notificação pré-sono)
│   └── di/
│       └── WorkerModule.kt          (HiltWorkerFactory binding)
│
├── notification/
│   ├── NotificationHelper.kt
│   └── ReminderScheduler.kt
│
└── ui/
    ├── theme/
    │   ├── Theme.kt
    │   ├── Color.kt
    │   └── Type.kt
    ├── presleep/
    │   ├── PreSleepScreen.kt
    │   ├── PreSleepViewModel.kt
    │   └── PreSleepUiState.kt
    ├── postsleep/
    │   ├── PostSleepScreen.kt
    │   ├── PostSleepViewModel.kt
    │   └── PostSleepUiState.kt
    ├── dashboard/
    │   ├── DashboardScreen.kt
    │   ├── DashboardViewModel.kt
    │   └── DashboardUiState.kt
    ├── challenge/
    │   ├── ChallengeScreen.kt
    │   ├── ChallengeViewModel.kt
    │   └── ChallengeUiState.kt
    ├── trends/
    │   ├── TrendsScreen.kt
    │   ├── TrendsViewModel.kt
    │   └── TrendsUiState.kt
    ├── settings/
    │   ├── SettingsScreen.kt
    │   └── SettingsViewModel.kt
    └── components/
        ├── SleepQualityCard.kt
        ├── ScoreSlider.kt
        ├── HabitChip.kt
        └── ChallengeCard.kt
```

**Regra de pacote fundamental:** Nada em `domain/` e `ai/` pode importar `android.*`, `androidx.*` ou qualquer classe do framework Android. Essas classes devem ser testáveis com JUnit puro sem Robolectric.

---

## 2. Padrão de UI Reativa (MVVM + StateFlow)

### 2.1 Estrutura do Trio UiState / ViewModel / Composable

O padrão é uniforme em todas as telas. Aqui usando `PostSleepScreen` como exemplo completo:

**`PostSleepUiState.kt`** — representa tudo o que a UI precisa para se renderizar:

```kotlin
// ui/postsleep/PostSleepUiState.kt
data class PostSleepUiState(
    val moodScore: Int = 3,
    val energyLevel: Int = 3,
    val perceivedQuality: Int = 3,
    val morningGrogginessMinutes: Int? = null,
    val dreamRecall: Boolean = false,
    val headache: Boolean = false,
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)
```

Regras para `UiState`:
- É sempre uma `data class` com valores padrão razoáveis
- Nunca usa tipos `LiveData`, `Flow` ou callbacks — é um snapshot imutável
- `isLoading` controla o estado de envio; `isSaved` dispara navegação para a próxima tela
- `errorMessage` é `null` quando não há erro — a UI só exibe o componente de erro quando não-nulo

**`PostSleepViewModel.kt`** — a única fonte de verdade do estado da tela:

```kotlin
// ui/postsleep/PostSleepViewModel.kt
@HiltViewModel
class PostSleepViewModel @Inject constructor(
    private val habitLogRepository: HabitLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostSleepUiState())
    val uiState: StateFlow<PostSleepUiState> = _uiState.asStateFlow()

    fun onMoodScoreChanged(value: Int) {
        _uiState.update { it.copy(moodScore = value) }
    }

    fun onEnergyLevelChanged(value: Int) {
        _uiState.update { it.copy(energyLevel = value) }
    }

    fun onPerceivedQualityChanged(value: Int) {
        _uiState.update { it.copy(perceivedQuality = value) }
    }

    fun onDreamRecallToggled() {
        _uiState.update { it.copy(dreamRecall = !it.dreamRecall) }
    }

    fun onNotesChanged(text: String) {
        if (text.length <= 500) {
            _uiState.update { it.copy(notes = text) }
        }
    }

    fun submitForm() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            habitLogRepository.savePostSleepLog(
                moodScore = state.moodScore,
                energyLevel = state.energyLevel,
                perceivedQuality = state.perceivedQuality,
                morningGrogginessMinutes = state.morningGrogginessMinutes,
                dreamRecall = state.dreamRecall,
                headache = state.headache,
                notes = state.notes.ifBlank { null }
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Erro ao salvar. Tente novamente."
                        )
                    }
                }
            )
        }
    }
}
```

Regras para `ViewModel`:
- **Nunca** expõe `MutableStateFlow` diretamente — sempre como `StateFlow` imutável via `.asStateFlow()`
- Funções de evento são prefixadas com `on` (ex: `onMoodScoreChanged`, `onDreamRecallToggled`)
- Todo IO (Room, Retrofit) ocorre dentro de `viewModelScope.launch`
- O ViewModel não sabe nada sobre navegação — apenas expõe `isSaved = true` e o Composable reage

**`PostSleepScreen.kt`** — o Composable que observa e reage:

```kotlin
// ui/postsleep/PostSleepScreen.kt
@Composable
fun PostSleepScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: PostSleepViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navegação reativa: observa isSaved e navega uma única vez
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateToDashboard()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Como você acordou?", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Humor")
        ScoreSlider(
            value = uiState.moodScore,
            onValueChange = viewModel::onMoodScoreChanged
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Energia")
        ScoreSlider(
            value = uiState.energyLevel,
            onValueChange = viewModel::onEnergyLevelChanged
        )

        // ... outros campos

        // Exibe erro se existir
        uiState.errorMessage?.let { error ->
            Snackbar { Text(error) }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = viewModel::submitForm,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Registrar minha manhã")
            }
        }
    }
}
```

Regras para Composables:
- Usam `collectAsStateWithLifecycle()` (não `collectAsState()`) — para respeitar o ciclo de vida do Android e não coletar em background
- Navegação via `LaunchedEffect(key)` — garante execução única quando o estado muda para `true`
- Composables **nunca** chamam funções de repositório diretamente — apenas funções do ViewModel
- Passam callbacks de navegação como lambdas (`onNavigateToDashboard: () -> Unit`) — o Composable não conhece o `NavController`

---

## 3. Tratamento de Erros e Convenção de Logs

### 3.1 Camada de Repository

Todo método de repositório que realiza IO retorna `Result<T>` (Kotlin stdlib):

```kotlin
// data/repository/HabitLogRepository.kt
interface HabitLogRepository {
    suspend fun savePostSleepLog(...): Result<Unit>
    suspend fun getLastNNights(n: Int): Result<List<NightDataAggregate>>
}

// data/repository/HabitLogRepositoryImpl.kt
class HabitLogRepositoryImpl @Inject constructor(
    private val postSleepLogDao: PostSleepLogDao,
    private val sleepSessionDao: SleepSessionDao,
    private val timeUtils: TimeUtils      // injetado, não singleton — testável
) : HabitLogRepository {

    override suspend fun savePostSleepLog(...): Result<Unit> = runCatching {
        val epochDay = TimeUtils.epochDayForPostSleep(System.currentTimeMillis(), TimeUtils.currentTimezoneId())
        val entity = PostSleepLogEntity(
            sleepEpochDay = epochDay,
            filledAtUtcMs = System.currentTimeMillis(),
            timezoneId = TimeUtils.currentTimezoneId(),
            moodScore = moodScore,
            // ...
        )
        postSleepLogDao.upsert(entity)
    }
}
```

**Regras:**
- `runCatching { }` envolve qualquer operação que possa lançar exceção (Room, Retrofit, serialização)
- Nunca propague exceções para o ViewModel — sempre retorne `Result.failure(e)` com a exceção original encapsulada
- Nunca retorne `null` onde `Result` seria mais expressivo

### 3.2 Camada de Network (Retrofit)

```kotlin
// data/remote/api/AiChallengeApi.kt
interface AiChallengeApi {
    @POST("v1/messages")
    suspend fun generateChallenge(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: AiRequestDto
    ): AiResponseDto
}
```

O Repository que usa o Retrofit também usa `runCatching`:

```kotlin
// data/repository/AiChallengeRepositoryImpl.kt
override suspend fun generateChallenge(nights: List<NightDataAggregate>): Result<AiChallengeEntity> = runCatching {
    val requestJson = PromptBuilder.build(nights)
    val rawResponse = aiChallengeApi.generateChallenge(
        apiKey = BuildConfig.LLM_API_KEY,
        request = Json.decodeFromString(requestJson)
    )
    val sanitized = AiResponseSanitizer.sanitize(rawResponse.content.first().text)
    val parsed = Json.decodeFromString<AiChallengeResponse>(sanitized)
    ChallengeValidator.validate(parsed)   // lança IllegalArgumentException se inválido
    AiResponseMapper.toEntity(parsed, requestJson, rawResponse.toString())
}
```

**Tipos de exceção e tratamento:**

| Exceção | Causa | Tratamento no Worker |
|---|---|---|
| `IOException` | Sem internet, timeout | Fallback para `LocalInsightEngine` |
| `HttpException` (4xx/5xx) | Erro da API | Fallback para `LocalInsightEngine` |
| `SerializationException` | JSON malformado após sanitização | Fallback para `LocalInsightEngine` |
| `IllegalArgumentException` | Validação do `ChallengeValidator` falhou | Fallback para `LocalInsightEngine` |
| `SQLiteException` | Erro de banco de dados | Retorna `Result.failure`, Worker retorna `Result.retry()` (única exceção ao padrão de não-retry) |

### 3.3 Workers — Convenção de Falhas

```kotlin
// worker/AiCallWorker.kt
@HiltWorker
class AiCallWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val aiChallengeRepository: AiChallengeRepository,
    private val habitLogRepository: HabitLogRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val nights = habitLogRepository.getLastNNights(30).getOrElse {
            Log.e(TAG, "Falha ao buscar histórico de noites", it)
            return Result.success()   // Não retry — aguarda próximo ciclo
        }

        if (nights.size < 3) {
            // Sem dados suficientes para IA ou heurísticas locais → desafio estático
            val staticChallenge = StaticChallengeProvider.getNext()
            aiChallengeRepository.saveStaticChallenge(staticChallenge)
            return Result.success()
        }

        // Tenta API
        aiChallengeRepository.generateChallenge(nights)
            .onSuccess { entity ->
                aiChallengeRepository.saveChallenge(entity)
                return Result.success()
            }
            .onFailure { apiError ->
                Log.w(TAG, "API falhou (${apiError.javaClass.simpleName}), tentando análise local")
            }

        // Fallback: análise local
        val localInsight = LocalInsightEngine.analyze(nights)
        if (localInsight != null) {
            aiChallengeRepository.saveLocalInsight(localInsight)
            return Result.success()
        }

        // Fallback final: desafio estático
        val staticChallenge = StaticChallengeProvider.getNext()
        aiChallengeRepository.saveStaticChallenge(staticChallenge)
        return Result.success()
    }

    companion object {
        private const val TAG = "AiCallWorker"
    }
}
```

**Regras para Workers:**
- Workers retornam `Result.success()` em quase todos os cenários — falhas de IA são esperadas e tratadas com fallback, não com retry
- `Result.retry()` apenas para `SQLiteException` (banco inacessível temporariamente)
- `Result.failure()` nunca deve ser retornado — causaria notificação de falha do WorkManager e não agrega valor
- Todo log de erro usa `Log.e(TAG, mensagem, throwable)` com a exceção encapsulada para stack trace completo
- Workers são **stateless** — não armazenam estado entre execuções; leem do Room e escrevem no Room

---

## 4. Padrão de Injeção do Hilt

### 4.1 `DatabaseModule.kt`

```kotlin
// data/di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HowSleepDatabase =
        Room.databaseBuilder(context, HowSleepDatabase::class.java, "howsleep.db")
            .fallbackToDestructiveMigration()   // MVP: migração destrutiva aceitável
            .build()

    @Provides
    fun provideSleepSessionDao(db: HowSleepDatabase): SleepSessionDao = db.sleepSessionDao()

    @Provides
    fun providePreSleepLogDao(db: HowSleepDatabase): PreSleepLogDao = db.preSleepLogDao()

    @Provides
    fun providePostSleepLogDao(db: HowSleepDatabase): PostSleepLogDao = db.postSleepLogDao()

    @Provides
    fun provideAiChallengeDao(db: HowSleepDatabase): AiChallengeDao = db.aiChallengeDao()

    @Provides
    fun provideChallengeDayLogDao(db: HowSleepDatabase): ChallengeDayLogDao = db.challengeDayLogDao()
}
```

**Regras:**
- O `HowSleepDatabase` é `@Singleton` — uma única instância para todo o app
- Cada DAO tem seu próprio `@Provides` (não `@Singleton`) — Room já gerencia a instância via a database
- `fallbackToDestructiveMigration()` é aceitável no MVP; antes de qualquer release beta, substituir por migrações explícitas

---

### 4.2 `NetworkModule.kt`

```kotlin
// data/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)   // LLMs podem demorar
            .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true        // resiliência: campos extras da API não quebram o parse
        coerceInputValues = true        // resiliência: tipos coercíveis são convertidos
        isLenient = false               // não aceita JSON malformado após sanitização
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAiChallengeApi(retrofit: Retrofit): AiChallengeApi =
        retrofit.create(AiChallengeApi::class.java)
}
```

**Regras:**
- `readTimeout` de 60s para LLMs — respostas podem demorar mais que o padrão de 30s
- `ignoreUnknownKeys = true` — futuras versões da API podem adicionar campos sem quebrar o app
- Log de body apenas em DEBUG — nunca logar payloads com dados do usuário em produção

---

### 4.3 `SleepModule.kt`

```kotlin
// data/di/SleepModule.kt
@Module
@InstallIn(SingletonComponent::class)
object SleepModule {

    @Provides
    @Singleton
    fun provideSleepEventSource(
        @ApplicationContext context: Context,
        dataStore: DataStore<Preferences>
    ): SleepEventSource {
        val useMock = runBlocking {
            dataStore.data.first()[PreferencesKeys.IS_MOCK_SLEEP_ENABLED] ?: false
        }
        return if (BuildConfig.DEBUG && useMock) {
            MockSleepEventSource()
        } else {
            RealSleepEventSource(context)
        }
    }
}
```

**Regra:** `MockSleepEventSource` só pode ser ativado em `BuildConfig.DEBUG`. A condição `BuildConfig.DEBUG &&` garante que, mesmo que o DataStore tenha `IS_MOCK_SLEEP_ENABLED = true` em builds de release (por algum bug), o mock nunca é ativado em produção.

---

### 4.4 `RepositoryModule.kt`

```kotlin
// data/di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSleepRepository(impl: SleepRepositoryImpl): SleepRepository

    @Binds
    @Singleton
    abstract fun bindHabitLogRepository(impl: HabitLogRepositoryImpl): HabitLogRepository

    @Binds
    @Singleton
    abstract fun bindAiChallengeRepository(impl: AiChallengeRepositoryImpl): AiChallengeRepository
}
```

**Regra:** Use `@Binds` (abstract class) para interfaces com implementação única — é mais eficiente que `@Provides` (object) porque não gera código de factory extra.

---

### 4.5 `WorkerModule.kt` e a Convenção `@HiltWorker`

Workers que precisam de injeção de dependência usam `@HiltWorker` + `@AssistedInject`:

```kotlin
// worker/di/WorkerModule.kt
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    fun provideWorkManagerFactory(
        assistedFactory: HiltWorkerFactory
    ): WorkerFactory = assistedFactory
}
```

```kotlin
// HowSleepApplication.kt
@HiltAndroidApp
class HowSleepApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

**Padrão obrigatório para todo Worker com DI:**

```kotlin
@HiltWorker
class ExemploWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    // Dependências injetadas normalmente abaixo:
    private val algumRepository: AlgumRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // ...
        return Result.success()
    }
}
```

**Regras:**
- `@Assisted` é obrigatório em `context` e `workerParams` — são os parâmetros injetados pelo WorkManager
- Nunca use `@Inject constructor` em Workers — sempre `@AssistedInject` com `@HiltWorker`
- Workers não devem ser `@Singleton` — o WorkManager cria novas instâncias a cada execução

---

### 4.6 Regras Gerais de Nomenclatura Hilt

| Tipo | Convenção de Nome | Escopo |
|---|---|---|
| Módulo object (provides) | `XxxModule.kt` | `@InstallIn(SingletonComponent::class)` para singletons de app |
| Módulo abstract (binds) | `XxxModule.kt` | Mesma regra de escopo |
| ViewModel | `@HiltViewModel` | Criado/destruído com o ViewModel scope — não é singleton |
| BroadcastReceiver | `@AndroidEntryPoint` | Sem escopo — Android gerencia o ciclo de vida |
| Activity/Fragment | `@AndroidEntryPoint` | Android gerencia o ciclo de vida |
| Worker | `@HiltWorker` + `@AssistedInject` | WorkManager gerencia o ciclo de vida |

**`@AndroidEntryPoint` em `SleepReceiver` é obrigatório** — sem ele, o Hilt não injeta dependências no BroadcastReceiver. Porém, como a regra do SleepReceiver é "ser ultraleve", o único ponto de injeção deve ser `WorkManager.getInstance(context)`, que não requer DI direta.

---

## 5. Convenção de Navegação

### 5.1 `Screen.kt`

```kotlin
// navigation/Screen.kt
sealed class Screen(val route: String) {
    object Onboarding    : Screen("onboarding")
    object Dashboard     : Screen("dashboard")
    object PreSleep      : Screen("pre_sleep")
    object PostSleep     : Screen("post_sleep")
    object Challenge     : Screen("challenge")
    object Trends        : Screen("trends")
    object History       : Screen("history")
    object Settings      : Screen("settings")
}
```

### 5.2 `NavGraph.kt`

```kotlin
// navigation/NavGraph.kt
@Composable
fun HowSleepNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToPreSleep = { navController.navigate(Screen.PreSleep.route) },
                onNavigateToChallenge = { navController.navigate(Screen.Challenge.route) }
            )
        }

        composable(Screen.PreSleep.route) {
            PreSleepScreen(
                onNavigateToDashboard = {
                    navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                }
            )
        }

        composable(Screen.PostSleep.route) {
            PostSleepScreen(
                onNavigateToDashboard = {
                    navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                }
            )
        }

        // ... demais rotas
    }
}
```

**Regras:**
- O `NavController` nunca entra em Composables como parâmetro — apenas lambdas de navegação
- `popBackStack` em vez de `navigate` para voltar ao Dashboard, evitando stack infinita
- O `startDestination` é determinado em `MainActivity` com base no DataStore (`ONBOARDING_COMPLETED`)

---

## 6. Padrão de Queries Room com Flow

DAOs que alimentam a UI expõem `Flow<T>` (não `suspend fun`):

```kotlin
// data/db/dao/SleepSessionDao.kt
@Dao
interface SleepSessionDao {

    // Alimenta o Dashboard — reativo, atualiza automaticamente quando o banco muda
    @Query("SELECT * FROM sleep_session ORDER BY sleep_epoch_day DESC LIMIT 7")
    fun getLast7Nights(): Flow<List<SleepSessionEntity>>

    // Busca única — não precisa ser Flow
    @Query("SELECT * FROM sleep_session WHERE sleep_epoch_day = :epochDay")
    suspend fun getByEpochDay(epochDay: Long): SleepSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SleepSessionEntity): Long

    @Query("SELECT COUNT(*) FROM sleep_session s INNER JOIN post_sleep_log p ON s.sleep_epoch_day = p.sleep_epoch_day WHERE s.sleep_epoch_day >= :fromEpochDay")
    suspend fun countCompleteNightsSince(fromEpochDay: Long): Int
}
```

**Regras:**
- Queries que alimentam `StateFlow` do ViewModel usam `Flow<T>` no DAO
- Queries pontuais (ex: verificar condição no Worker) usam `suspend fun` que retorna diretamente
- O ViewModel converte `Flow` de Room para `StateFlow` via `.stateIn(viewModelScope, ...)`:

```kotlin
// Dentro do ViewModel:
val last7Nights: StateFlow<List<SleepSessionEntity>> = sleepRepository
    .getLast7Nights()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),  // 5s de grace period para rotação de tela
        initialValue = emptyList()
    )
```

---

## 7. Convenções de Teste

| Camada | Framework | O que testar |
|---|---|---|
| `domain/` e `ai/` | JUnit 4 puro | `PromptBuilder`, `AiResponseSanitizer`, `ChallengeValidator`, `LocalInsightEngine`, `TimeUtils`, `NightStateComputer` |
| DAOs (Room) | `@RunWith(AndroidJUnit4::class)` + Room in-memory | Queries SQL, conflitos de REPLACE, JOINs por epoch_day |
| ViewModels | JUnit 4 + `TestCoroutineDispatcher` + fake repositories | Transições de `UiState`, propagação de erros |
| Workers | `WorkManagerTestInitHelper` + Room in-memory | Lógica de fallback do `AiCallWorker`, avaliação do `ChallengeEvaluationWorker` |
| Sleep API | Teste manual em dispositivo físico | `SleepApiManager` + `SleepReceiver` (não testável por JUnit) |
| UI (Compose) | `ComposeTestRule` | Flows de tela críticos (onboarding, submit de formulário) |

**Regra de ouro para testabilidade:** Se uma classe importa `android.*` ou `androidx.*`, ela exige Android test. Mantenha a lógica de negócio em `domain/` e `ai/` para maximizar a cobertura com testes JUnit puros.
