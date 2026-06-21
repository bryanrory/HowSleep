# DATA_CONTRACTS — Dicionário de Dados & Contratos de IA
## HowSleep: Monitor de Sono Inteligente e Comportamental

**Versão:** 1.0  
**Data:** 2026-06-20

---

## 1. Convenções Globais do Schema

### 1.1 Timestamps

Todos os timestamps são armazenados como `Long` (epoch milliseconds UTC). Nunca converter para fuso local antes de persistir.

| Sufixo de coluna | Tipo | Significado |
|---|---|---|
| `_utc_ms` | `Long` | Epoch milissegundos UTC (ex: `System.currentTimeMillis()`) |
| `_epoch_day` | `Long` | Dias desde 1970-01-01 UTC (ex: `LocalDate.now().toEpochDay()`) |
| `_local_hour` | `Int` | Hora local derivada (0–23), calculada no write, armazenada como `Int` |

### 1.2 ENUMs

Todos os ENUMs são armazenados como `String` no Room (não como `Int`). Isso garante legibilidade ao inspecionar o banco e evita bugs de migração ao reordenar valores.

### 1.3 Chave de JOIN

A chave natural que une todas as tabelas de uma mesma noite é `sleep_epoch_day: Long`. Não há FK entre as tabelas de log — o JOIN é feito por valor igual de `sleep_epoch_day`.

---

## 2. Schema Room Detalhado

### 2.1 Tabela: `sleep_session`

Representa uma sessão de sono detectada pelo Android Sleep API ou inserida manualmente.

| Campo | Tipo Kotlin | Anotação Room | Restrições | Justificativa de Negócio |
|---|---|---|---|---|
| `id` | `Long` | `@PrimaryKey(autoGenerate = true)` | AUTO PK | Identificador interno |
| `sleep_epoch_day` | `Long` | `@ColumnInfo(index = true)` | INDEX, UNIQUE | Chave de JOIN com as outras tabelas; UNIQUE porque só há uma sessão por noite |
| `sleep_start_utc_ms` | `Long` | `@ColumnInfo` | NOT NULL | Início do sono em UTC; usado para calcular `sleep_epoch_day` e duração |
| `sleep_end_utc_ms` | `Long` | `@ColumnInfo` | NOT NULL | Fim do sono em UTC; junto com `sleep_start` define a duração total |
| `timezone_id` | `String` | `@ColumnInfo` | NOT NULL | Fuso horário local no momento do evento (ex: "America/Sao_Paulo"); congela o contexto de fuso para queries de horário local |
| `total_duration_minutes` | `Int` | `@ColumnInfo` | NOT NULL | Duração total em minutos, calculada e armazenada para performance de queries (evita cálculo em tempo de query) |
| `confidence` | `Int` | `@ColumnInfo` | NOT NULL, CHECK 0–100 | Score de confiança do Sleep API (0–100); influencia exibição de aviso "dados incertos" |
| `interruption_count` | `Int` | `@ColumnInfo` | NOT NULL, DEFAULT 0 | Número de segmentos AWAKE entre segmentos ASLEEP; proxy de qualidade objetiva do sono |
| `light_sleep_minutes` | `Int?` | `@ColumnInfo` | NULLABLE | Reservado para integração futura com wearables; Sleep API nativo não fornece |
| `deep_sleep_minutes` | `Int?` | `@ColumnInfo` | NULLABLE | Idem acima |
| `source` | `String` | `@ColumnInfo` | NOT NULL, DEFAULT "SLEEP_API" | Enum: "SLEEP_API" \| "MANUAL"; distingue dado do sensor de entrada manual do usuário |
| `created_at_utc_ms` | `Long` | `@ColumnInfo` | NOT NULL | Auditoria: quando o registro foi criado no banco |

**Índices:** `sleep_epoch_day` (UNIQUE INDEX) — usado em todo JOIN e lookup.

**Conflito de upsert:** `@Insert(onConflict = OnConflictStrategy.REPLACE)` — o Sleep API pode emitir múltiplos eventos para a mesma noite; sempre consolida com os dados mais recentes.

---

### 2.2 Tabela: `pre_sleep_log`

Hábitos e comportamentos registrados pelo usuário antes de dormir.

| Campo | Tipo Kotlin | Anotação Room | Restrições | Justificativa de Negócio |
|---|---|---|---|---|
| `id` | `Long` | `@PrimaryKey(autoGenerate = true)` | AUTO PK | Identificador interno |
| `sleep_epoch_day` | `Long` | `@ColumnInfo(index = true)` | INDEX, UNIQUE | Chave de JOIN; UNIQUE porque só há um log pré-sono por noite |
| `filled_at_utc_ms` | `Long` | `@ColumnInfo` | NOT NULL | Horário do último preenchimento (atualizado a cada REPLACE) |
| `timezone_id` | `String` | `@ColumnInfo` | NOT NULL | Fuso horário no momento do preenchimento |
| `caffeine_mg` | `Int?` | `@ColumnInfo` | NULLABLE | Quantidade estimada de cafeína em mg; null = não registrado |
| `caffeine_last_intake_utc_ms` | `Long?` | `@ColumnInfo` | NULLABLE | Timestamp UTC da última dose de cafeína; fonte de verdade para cálculos |
| `caffeine_last_intake_local_hour` | `Int?` | `@ColumnInfo` | NULLABLE, 0–23 | Hora local derivada e armazenada no write; usada diretamente em queries SQL (ex: "WHERE caffeine_last_intake_local_hour >= 14") |
| `screen_time_minutes_2h_before` | `Int?` | `@ColumnInfo` | NULLABLE, 0–120 | Minutos de tela nas 2h antes de dormir; principal fator de higiene do sono |
| `stress_level` | `Int` | `@ColumnInfo` | NOT NULL, 1–5 | Nível de estresse do dia; único campo obrigatório além de `mood_score` |
| `last_meal_utc_ms` | `Long?` | `@ColumnInfo` | NULLABLE | Timestamp UTC da última refeição; usado para calcular `hours_since_last_meal` |
| `last_meal_type` | `String?` | `@ColumnInfo` | NULLABLE | Enum: "LIGHT" \| "HEAVY" \| "SNACK"; influencia heurística de MEAL_TIMING |
| `exercise_done` | `Boolean` | `@ColumnInfo` | NOT NULL, DEFAULT false | Flag de exercício; usado na heurística EXERCISE |
| `exercise_intensity` | `String?` | `@ColumnInfo` | NULLABLE | Enum: "LOW" \| "MODERATE" \| "HIGH"; null se `exercise_done = false` |
| `exercise_minutes_before_bed` | `Int?` | `@ColumnInfo` | NULLABLE | Minutos entre fim do exercício e hora planejada de dormir |
| `alcohol_units` | `Float?` | `@ColumnInfo` | NULLABLE, 0.0–10.0 | Doses de álcool (unidades padrão de ~14g de etanol puro); null = não registrado |
| `notes` | `String?` | `@ColumnInfo` | NULLABLE, MAX 500 chars | Campo livre; **não enviado à IA** (privacidade) |

**Conflito de upsert:** `@Insert(onConflict = OnConflictStrategy.REPLACE)` — permite edição do formulário no mesmo dia.

---

### 2.3 Tabela: `post_sleep_log`

Estado subjetivo e avaliação do sono registrado pelo usuário ao acordar.

| Campo | Tipo Kotlin | Anotação Room | Restrições | Justificativa de Negócio |
|---|---|---|---|---|
| `id` | `Long` | `@PrimaryKey(autoGenerate = true)` | AUTO PK | Identificador interno |
| `sleep_epoch_day` | `Long` | `@ColumnInfo(index = true)` | INDEX, UNIQUE | Chave de JOIN com as outras tabelas |
| `filled_at_utc_ms` | `Long` | `@ColumnInfo` | NOT NULL | Horário do preenchimento; usado para verificar se está dentro da janela de corte (12:00) |
| `timezone_id` | `String` | `@ColumnInfo` | NOT NULL | Fuso no momento do preenchimento |
| `mood_score` | `Int` | `@ColumnInfo` | NOT NULL, 1–5 | Humor ao acordar; campo obrigatório; pode ser `success_metric_type` de um desafio |
| `energy_level` | `Int` | `@ColumnInfo` | NOT NULL, 1–5 | Energia ao acordar; campo obrigatório; pode ser `success_metric_type` |
| `perceived_quality` | `Int` | `@ColumnInfo` | NOT NULL, 1–5 | Avaliação subjetiva do sono; o dado mais valorizado pelo usuário |
| `perceived_duration_hours` | `Float?` | `@ColumnInfo` | NULLABLE | O que o usuário achou que dormiu; delta com `sleep_session.total_duration_minutes` é insight por si só |
| `morning_grogginess_minutes` | `Int?` | `@ColumnInfo` | NULLABLE, 0–120 | Inércia do sono; correlação com qualidade do sono profundo |
| `dream_recall` | `Boolean` | `@ColumnInfo` | NOT NULL, DEFAULT false | Recordou sonhos; indicador indireto de sono REM |
| `headache` | `Boolean` | `@ColumnInfo` | NOT NULL, DEFAULT false | Dor de cabeça ao acordar; correlação com álcool e apneia |
| `notes` | `String?` | `@ColumnInfo` | NULLABLE, MAX 500 chars | Campo livre; **não enviado à IA** |

**Conflito de upsert:** `@Insert(onConflict = OnConflictStrategy.REPLACE)` — permite correção até 23:59 do mesmo dia.

**Gatilho:** A submissão bem-sucedida deste formulário dispara o `AiCallWorker` via `HabitLogRepository`.

---

### 2.4 Tabela: `ai_challenge`

Desafio gerado pela IA, pelo `LocalInsightEngine` ou pelo `StaticChallengeProvider`.

| Campo | Tipo Kotlin | Anotação Room | Restrições | Justificativa de Negócio |
|---|---|---|---|---|
| `id` | `Long` | `@PrimaryKey(autoGenerate = true)` | AUTO PK | Identificador interno |
| `generated_at_utc_ms` | `Long` | `@ColumnInfo` | NOT NULL | Quando o desafio foi criado; usado para evitar duplicatas no mesmo dia |
| `valid_from_epoch_day` | `Long` | `@ColumnInfo` | NOT NULL | Primeiro dia do desafio (inclusive) |
| `valid_until_epoch_day` | `Long` | `@ColumnInfo` | NOT NULL | Último dia do desafio (inclusive); = valid_from + duration_days - 1 |
| `title` | `String` | `@ColumnInfo` | NOT NULL, MAX 60 chars | Título exibido ao usuário; ex: "Detox de Cafeína" |
| `description` | `String` | `@ColumnInfo` | NOT NULL, MAX 200 chars | Explicação do desafio; ex: "Evitar cafeína após as 14h pode reduzir despertares noturnos" |
| `habit_to_change` | `String` | `@ColumnInfo` | NOT NULL | Enum: "CAFFEINE" \| "SCREEN_TIME" \| "STRESS" \| "MEAL_TIMING" \| "EXERCISE" \| "ALCOHOL" \| "SLEEP_SCHEDULE" |
| `habit_change_instruction` | `String` | `@ColumnInfo` | NOT NULL, MAX 150 chars | Instrução acionável; ex: "Não tome café ou chá preto após as 14:00" |
| `duration_days` | `Int` | `@ColumnInfo` | NOT NULL, 5–14 | Duração do desafio em dias |
| `success_metric_type` | `String` | `@ColumnInfo` | NOT NULL | Enum: "SLEEP_DURATION" \| "MOOD_SCORE" \| "ENERGY_LEVEL" \| "PERCEIVED_QUALITY" |
| `success_metric_target` | `Float` | `@ColumnInfo` | NOT NULL | Valor alvo da métrica (ex: 7.0 para 7h de sono, 4.0 para mood ≥4) |
| `success_metric_direction` | `String` | `@ColumnInfo` | NOT NULL | Enum: "ABOVE" \| "BELOW"; define se o alvo é atingir acima ou abaixo do target |
| `baseline_value` | `Float` | `@ColumnInfo` | NOT NULL | Média da `success_metric_type` nas 3 noites anteriores ao desafio; snapshot imutável |
| `source` | `String` | `@ColumnInfo` | NOT NULL, DEFAULT "AI_API" | Enum: "AI_API" \| "LOCAL_ENGINE" \| "STATIC_DEFAULT"; rastreia a origem do desafio |
| `prompt_context_json` | `String?` | `@ColumnInfo` | NULLABLE | JSON do prompt enviado à IA (apenas para `source = "AI_API"`); para debug e audit |
| `raw_ai_response_json` | `String?` | `@ColumnInfo` | NULLABLE | Resposta bruta da IA pré-sanitização; para debug e audit |
| `status` | `String` | `@ColumnInfo` | NOT NULL, DEFAULT "ACTIVE" | Enum: "ACTIVE" \| "COMPLETED" \| "ABANDONED" \| "EXPIRED" |

---

### 2.5 Tabela: `challenge_day_log`

Registro diário de execução e outcome de cada dia de um desafio ativo.

| Campo | Tipo Kotlin | Anotação Room | Restrições | Justificativa de Negócio |
|---|---|---|---|---|
| `id` | `Long` | `@PrimaryKey(autoGenerate = true)` | AUTO PK | Identificador interno |
| `challenge_id` | `Long` | `@ColumnInfo(index = true)` | NOT NULL, FK → ai_challenge.id | Qual desafio este registro pertence |
| `sleep_epoch_day` | `Long` | `@ColumnInfo` | NOT NULL, UNIQUE per challenge_id | Qual dia do desafio este registro representa |
| `habit_followed` | `Boolean?` | `@ColumnInfo` | NULLABLE | Null = não avaliado; true/false = resultado da heurística de hábito |
| `habit_followed_evidence` | `String?` | `@ColumnInfo` | NULLABLE, MAX 300 chars | Descrição legível da evidência usada (ex: "caffeine_last_intake_local_hour=11, target<14 → true") |
| `outcome_metric_value` | `Float?` | `@ColumnInfo` | NULLABLE | Valor real da `success_metric_type` para esta noite |
| `outcome_improved` | `Boolean?` | `@ColumnInfo` | NULLABLE | Null = não avaliado; computed: `outcome_metric_value` atende ao `success_metric_target`? |

**Índice composto:** `(challenge_id, sleep_epoch_day)` UNIQUE — um registro por dia por desafio.

**Preenchimento:** Feito pelo `ChallengeEvaluationWorker` automaticamente após cada submissão de `post_sleep_log` enquanto há desafio `ACTIVE`.

---

## 3. Payloads da API do LLM

### 3.1 Exemplo de Requisição (3 noites simuladas)

Este JSON é construído pelo `PromptBuilder.kt`. Representa exatamente o que é enviado ao endpoint do LLM.

```json
{
  "model": "claude-sonnet-4-6",
  "system": "You are a sleep science advisor analyzing behavioral data from a sleep tracking app. Your role is to identify the single most impactful habit the user can change to improve their sleep quality. You must respond ONLY with a valid JSON object matching the exact schema provided in the user message. Do not include explanations, markdown formatting, code blocks, or any text outside the JSON object. Focus on patterns that appear in at least 2 of the nights provided. Prioritize habits with clear correlation to poor sleep outcomes. Be specific and actionable.",
  "messages": [
    {
      "role": "user",
      "content": {
        "request": "analyze_sleep_habits",
        "nights_analyzed": 3,
        "date_range": "2026-06-17 to 2026-06-19",
        "user_context": {
          "avg_sleep_duration_hours": 6.1,
          "avg_perceived_quality": 2.3,
          "avg_mood_score": 2.7,
          "avg_energy_level": 2.5,
          "nights_with_caffeine_after_14h": 2,
          "nights_with_screen_time_over_60min": 3,
          "nights_with_exercise": 1
        },
        "sleep_data": [
          {
            "date": "2026-06-17",
            "pre_sleep": {
              "caffeine_mg": 160,
              "caffeine_last_intake_local_hour": 16,
              "screen_time_minutes_2h_before": 90,
              "stress_level": 3,
              "hours_since_last_meal": 1.5,
              "last_meal_type": "HEAVY",
              "exercise_done": false,
              "exercise_intensity": null,
              "exercise_minutes_before_bed": null,
              "alcohol_units": 0.0
            },
            "sleep": {
              "total_duration_minutes": 340,
              "total_duration_hours": 5.67,
              "confidence": 82,
              "interruption_count": 3,
              "sleep_efficiency_percent": 79.1
            },
            "post_sleep": {
              "mood_score": 2,
              "energy_level": 2,
              "perceived_quality": 2,
              "morning_grogginess_minutes": 45
            }
          },
          {
            "date": "2026-06-18",
            "pre_sleep": {
              "caffeine_mg": 80,
              "caffeine_last_intake_local_hour": 15,
              "screen_time_minutes_2h_before": 110,
              "stress_level": 4,
              "hours_since_last_meal": 2.0,
              "last_meal_type": "HEAVY",
              "exercise_done": true,
              "exercise_intensity": "MODERATE",
              "exercise_minutes_before_bed": 60,
              "alcohol_units": 1.0
            },
            "sleep": {
              "total_duration_minutes": 375,
              "total_duration_hours": 6.25,
              "confidence": 88,
              "interruption_count": 2,
              "sleep_efficiency_percent": 83.3
            },
            "post_sleep": {
              "mood_score": 3,
              "energy_level": 3,
              "perceived_quality": 2,
              "morning_grogginess_minutes": 30
            }
          },
          {
            "date": "2026-06-19",
            "pre_sleep": {
              "caffeine_mg": 240,
              "caffeine_last_intake_local_hour": 17,
              "screen_time_minutes_2h_before": 120,
              "stress_level": 5,
              "hours_since_last_meal": 1.0,
              "last_meal_type": "HEAVY",
              "exercise_done": false,
              "exercise_intensity": null,
              "exercise_minutes_before_bed": null,
              "alcohol_units": 0.0
            },
            "sleep": {
              "total_duration_minutes": 295,
              "total_duration_hours": 4.92,
              "confidence": 75,
              "interruption_count": 5,
              "sleep_efficiency_percent": 71.2
            },
            "post_sleep": {
              "mood_score": 1,
              "energy_level": 2,
              "perceived_quality": 1,
              "morning_grogginess_minutes": 60
            }
          }
        ],
        "response_schema": {
          "title": "string (max 60 chars)",
          "description": "string (max 200 chars)",
          "habit_to_change": "one of: CAFFEINE | SCREEN_TIME | STRESS | MEAL_TIMING | EXERCISE | ALCOHOL | SLEEP_SCHEDULE",
          "habit_change_instruction": "string (specific and actionable, max 150 chars)",
          "duration_days": "integer between 5 and 14",
          "success_metric_type": "one of: SLEEP_DURATION | MOOD_SCORE | ENERGY_LEVEL | PERCEIVED_QUALITY",
          "success_metric_target": "float",
          "success_metric_direction": "one of: ABOVE | BELOW",
          "reasoning": "string (max 300 chars — explain the pattern you detected)"
        }
      }
    }
  ],
  "max_tokens": 512
}
```

---

### 3.2 Exemplo de Resposta Esperada do LLM

O `AiResponseSanitizer` deve ser capaz de extrair este JSON mesmo que o LLM o envolva em markdown. Este é o JSON puro que deve resultar após a sanitização:

```json
{
  "title": "Corte a Cafeína Tarde",
  "description": "Você consumiu cafeína após as 15h em 3 das últimas 3 noites. Isso atrasa a produção de melatonina e fragmenta o sono nas primeiras horas da madrugada.",
  "habit_to_change": "CAFFEINE",
  "habit_change_instruction": "Não consuma cafeína (café, chá preto, energético) após as 14:00 por 7 dias.",
  "duration_days": 7,
  "success_metric_type": "PERCEIVED_QUALITY",
  "success_metric_target": 3.5,
  "success_metric_direction": "ABOVE",
  "reasoning": "Cafeína após 14h presente em 3/3 noites. Noites com mais cafeína tardia (16h, 17h) tiveram maior interruption_count (3 e 5) e menor perceived_quality (2 e 1) em comparação com a noite de menor consumo tardio."
}
```

### 3.3 Validações do `ChallengeValidator` sobre a Resposta

| Campo | Regra de Validação | Ação em Falha |
|---|---|---|
| `title` | não-nulo, não-vazio, ≤ 60 chars | Fallback para LocalInsightEngine |
| `description` | não-nulo, não-vazio, ≤ 200 chars | Truncar para 200 chars (soft validation) |
| `habit_to_change` | deve ser um dos 7 valores do ENUM | Fallback para LocalInsightEngine |
| `habit_change_instruction` | não-nulo, não-vazio, ≤ 150 chars | Truncar para 150 chars |
| `duration_days` | inteiro entre 5 e 14 | Fallback para LocalInsightEngine |
| `success_metric_type` | deve ser um dos 4 valores do ENUM | Fallback para LocalInsightEngine |
| `success_metric_target` | Float positivo > 0 | Fallback para LocalInsightEngine |
| `success_metric_direction` | "ABOVE" ou "BELOW" | Fallback para LocalInsightEngine |
| `reasoning` | opcional; truncar se > 300 chars | Aceita sem o campo |

---

### 3.4 Contrato do `NightDataAggregate`

Data class que o `PromptBuilder` recebe. Resultado de um JOIN das 3 tabelas por `sleep_epoch_day`:

```kotlin
data class NightDataAggregate(
    val sleepEpochDay: Long,
    val date: LocalDate,              // derivado de sleepEpochDay
    // De sleep_session (nullable — pode não ter sessão detectada ainda)
    val totalDurationMinutes: Int?,
    val confidence: Int?,
    val interruptionCount: Int?,
    val sleepEfficiencyPercent: Float?,
    // De pre_sleep_log (nullable — usuário pode ter esquecido)
    val caffeineMg: Int?,
    val caffeineLastIntakeLocalHour: Int?,
    val screenTimeMinutes2hBefore: Int?,
    val stressLevel: Int?,
    val hoursSinceLastMeal: Float?,   // calculado: (sleepStartUtcMs - lastMealUtcMs) / 3_600_000f
    val lastMealType: String?,
    val exerciseDone: Boolean?,
    val exerciseIntensity: String?,
    val exerciseMinutesBeforeBed: Int?,
    val alcoholUnits: Float?,
    // De post_sleep_log (nullable — trigger do Worker, mas pode estar ausente)
    val moodScore: Int?,
    val energyLevel: Int?,
    val perceivedQuality: Int?,
    val morningGrogginessMinutes: Int?
)
```

---

## 4. Contratos de Workers (InputData / OutputData)

### 4.1 `SleepSessionWorker`

**Input (via `WorkData`):**

| Chave | Tipo | Descrição |
|---|---|---|
| `"events_json"` | `String` | JSON serializado da lista de `SleepSegmentEvent` extraída do Intent do `SleepReceiver` |

**Output:** Nenhum (grava diretamente no Room).

**Trigger pós-execução:** Nenhum — o `AiCallWorker` é disparado pelo repositório após a submissão do `post_sleep_log`, não pelo `SleepSessionWorker`.

---

### 4.2 `AiCallWorker`

**Input:** Nenhum (lê do Room internamente).

**Output:** Nenhum (grava `AiChallengeEntity` diretamente no Room).

**Estados de resultado do Worker:**

| Cenário | `Result` do Worker | Registro no DB |
|---|---|---|
| API chama com sucesso + JSON válido | `Result.success()` | `source = "AI_API"` |
| API falha (IOException) + LocalEngine com dados | `Result.success()` | `source = "LOCAL_ENGINE"` |
| API falha + LocalEngine sem dados | `Result.success()` | `source = "STATIC_DEFAULT"` |
| Validação falha em todos os paths | `Result.success()` | Nenhum (sem desafio gerado) |

O Worker retorna `Result.success()` mesmo em fallback — falhas de IA não devem fazer o WorkManager tentar novamente automaticamente (o retry é gerenciado pelo ciclo diário).

---

### 4.3 `ChallengeEvaluationWorker`

**Input:** Nenhum (lê do Room internamente: desafio `ACTIVE` + `pre_sleep_log` + `post_sleep_log` + `sleep_session`).

**Output:** Nenhum (atualiza `challenge_day_log` e eventualmente `ai_challenge.status`).
