# STATE_MACHINE — Máquina de Estados e Ciclo de Vida do Sono
## HowSleep: Monitor de Sono Inteligente e Comportamental

**Versão:** 1.0  
**Data:** 2026-06-20

---

## 1. Visão Geral

Cada "noite" no HowSleep é representada por um `sleep_epoch_day` (Long) e possui um **estado lógico** derivado da combinação de registros presentes nas tabelas `pre_sleep_log`, `sleep_session`, `post_sleep_log` e `ai_challenge`. Este estado não é armazenado como coluna — ele é **computado em tempo de query** pela camada de repositório, garantindo que nunca fique dessincronizado com os dados reais.

---

## 2. Estados de uma Noite

### 2.1 Diagrama de Transição de Estados

```
                    ┌─────────────────────────────────────────────────┐
                    │             FLUXO PRINCIPAL                     │
                    └─────────────────────────────────────────────────┘

[INÍCIO DO DIA]
      │
      ▼
┌─────────────────┐
│  PENDING_PRE    │  Nenhum dado para este sleep_epoch_day ainda.
│  _SLEEP         │  O app aguarda o usuário preencher o formulário
└────────┬────────┘  pré-sono ou a passagem do horário de corte.
         │
         │  pre_sleep_log gravado (qualquer campo obrigatório preenchido)
         ▼
┌─────────────────┐
│  PRE_SLEEP_     │  Pré-sono registrado. Sleep API está monitorando
│  LOGGED         │  passivamente. O usuário foi dormir (ou está
└────────┬────────┘  prestes a ir).
         │
         │  SleepSessionWorker grava sleep_session para este epoch_day
         ▼
┌─────────────────┐
│  SLEEP_         │  Sessão de sono detectada e gravada. O usuário
│  DETECTED       │  acordou ou está acordando. App aguarda o
└────────┬────────┘  formulário pós-sono.
         │
         │  post_sleep_log gravado (campos obrigatórios preenchidos)
         ▼
┌─────────────────┐
│  POST_SLEEP_    │  Pós-sono registrado. Dados completos para análise.
│  LOGGED         │  AiCallWorker é enfileirado.
└────────┬────────┘
         │
         │  AiCallWorker termina (independente do source: AI/LOCAL/STATIC)
         ▼
┌─────────────────┐
│  CHALLENGE_     │  Desafio gerado e persistido. Usuário tem um
│  ACTIVE         │  desafio ativo associado a esta noite (e às
└────────┬────────┘  próximas N noites do desafio).
         │
         │  valid_until_epoch_day atingido + ChallengeEvaluationWorker executa
         ▼
┌─────────────────┐
│  CHALLENGE_     │  Ciclo completo. Desafio avaliado (COMPLETED,
│  EVALUATED      │  ABANDONED ou EXPIRED).
└─────────────────┘
```

### 2.2 Estados Alternativos (Desvios do Fluxo Principal)

```
[INICIO DO DIA]
      │
      │  Usuário não preenche pré-sono até 23:59 do dia seguinte
      ▼
┌─────────────────┐
│  SKIPPED_PRE    │  Pré-sono nunca preenchido. A noite pode ainda
│  _SLEEP         │  ter sleep_session e post_sleep_log — elas são
└─────────────────┘  processadas normalmente, mas sem dados de hábitos.
                     O PromptBuilder inclui essa noite com campos pre_sleep = null.

      │
      │  Sleep API não emite evento / confiança < threshold / usuário
      │  ativou "Modo Manual" e não inseriu horários
      ▼
┌─────────────────┐
│  NO_SLEEP_      │  Nenhuma sleep_session para este epoch_day.
│  DATA           │  O formulário pós-sono pode existir mesmo assim.
└─────────────────┘  Noites NO_SLEEP_DATA com post_sleep_log são
                     incluídas no PromptBuilder (dados subjetivos
                     têm valor mesmo sem sensor).

      │
      │  Usuário toca "Pular essa noite" ou não preenche pós-sono
      │  até 23:59 do dia seguinte
      ▼
┌─────────────────┐
│  INCOMPLETE     │  Pós-sono ausente. AiCallWorker NÃO dispara.
│                 │  Noite excluída do PromptBuilder e LocalInsightEngine.
└─────────────────┘  Exibida no dashboard com badge "Noite incompleta".
```

### 2.3 Definição Formal de Cada Estado

| Estado | Condição (derivada das tabelas) |
|---|---|
| `PENDING_PRE_SLEEP` | Nenhum registro em nenhuma das 3 tabelas para o epoch_day |
| `SKIPPED_PRE_SLEEP` | `sleep_session` ou `post_sleep_log` existe, mas `pre_sleep_log` não |
| `PRE_SLEEP_LOGGED` | `pre_sleep_log` existe; `sleep_session` não existe; `post_sleep_log` não existe |
| `SLEEP_DETECTED` | `sleep_session` existe; `post_sleep_log` não existe |
| `NO_SLEEP_DATA` | `pre_sleep_log` existe; `sleep_session` não existe após 12:00 do dia seguinte |
| `POST_SLEEP_LOGGED` | `post_sleep_log` existe; nenhum `ai_challenge` com `valid_from_epoch_day` ≤ epoch_day ≤ `valid_until_epoch_day` |
| `CHALLENGE_ACTIVE` | `post_sleep_log` existe; `ai_challenge` com `status = "ACTIVE"` abrange este epoch_day |
| `CHALLENGE_EVALUATED` | `ai_challenge` com `status ∈ {COMPLETED, ABANDONED, EXPIRED}` abrange este epoch_day |
| `INCOMPLETE` | Nenhum `post_sleep_log` para o epoch_day após 23:59 do dia seguinte |

### 2.4 Implementação: `NightStateComputer.kt`

```kotlin
// domain/NightStateComputer.kt — sem imports Android, puro Kotlin
enum class NightState {
    PENDING_PRE_SLEEP,
    SKIPPED_PRE_SLEEP,
    PRE_SLEEP_LOGGED,
    SLEEP_DETECTED,
    NO_SLEEP_DATA,
    POST_SLEEP_LOGGED,
    CHALLENGE_ACTIVE,
    CHALLENGE_EVALUATED,
    INCOMPLETE
}

object NightStateComputer {
    fun compute(
        hasPreSleep: Boolean,
        hasSleepSession: Boolean,
        hasPostSleep: Boolean,
        activeChallengeCoversDay: Boolean,
        evaluatedChallengeCoversDay: Boolean,
        isExpiredWithoutPostSleep: Boolean  // epoch_day > hoje E sem post_sleep
    ): NightState = when {
        isExpiredWithoutPostSleep -> NightState.INCOMPLETE
        evaluatedChallengeCoversDay -> NightState.CHALLENGE_EVALUATED
        activeChallengeCoversDay -> NightState.CHALLENGE_ACTIVE
        hasPostSleep -> NightState.POST_SLEEP_LOGGED
        hasSleepSession && !hasPostSleep -> NightState.SLEEP_DETECTED
        hasPreSleep && !hasSleepSession && !hasPostSleep -> NightState.PRE_SLEEP_LOGGED
        !hasPreSleep && (hasSleepSession || hasPostSleep) -> NightState.SKIPPED_PRE_SLEEP
        !hasPreSleep && !hasSleepSession && hasPostSleep -> NightState.SKIPPED_PRE_SLEEP
        else -> NightState.PENDING_PRE_SLEEP
    }
}
```

---

## 3. Ciclo de Vida do Desafio

### 3.1 Estados do `ai_challenge.status`

```
[AiCallWorker conclui com sucesso]
      │
      ▼
┌───────────┐
│  ACTIVE   │  Desafio criado. ChallengeEvaluationWorker avalia
│           │  diariamente e preenche challenge_day_log.
└─────┬─────┘
      │
      │  valid_until_epoch_day atingido E dados suficientes para avaliar
      ▼
┌──────────────┐    taxa_adesao >= 0.70 AND outcome_melhorado >= 10%    ┌───────────────┐
│  [Avaliação] │  ─────────────────────────────────────────────────────►│  COMPLETED    │
│              │                                                         └───────────────┘
│              │  taxa_adesao >= 0.70 AND outcome_melhorado < 10%       ┌───────────────┐
│              │  ─────────────────────────────────────────────────────►│  COMPLETED    │
│              │  (seguiu mas sem efeito detectado)                      └───────────────┘
│              │
│              │  taxa_adesao < 0.70                                     ┌───────────────┐
│              │  ─────────────────────────────────────────────────────►│  ABANDONED    │
│              │                                                         └───────────────┘
│              │  dias_avaliados < (duration_days * 0.5)                ┌───────────────┐
│              │  ─────────────────────────────────────────────────────►│  EXPIRED      │
└──────────────┘                                                         └───────────────┘
```

**Transição manual:** O usuário pode "Abandonar desafio" explicitamente via Settings. Isso seta `status = "ABANDONED"` imediatamente e permite que o próximo `AiCallWorker` gere um novo desafio.

---

## 4. Matriz de Resolução de Conflitos

Esta seção define o comportamento do app em cenários de borda onde eventos ocorrem fora da ordem esperada ou simultaneamente.

---

### Cenário C-01: Pós-Sono submetido antes do `SleepSessionWorker` terminar

**Situação:** O usuário acorda, abre o app e preenche o formulário pós-sono antes que o Android Sleep API tenha emitido o evento de fim de sono (ou antes que o `SleepSessionWorker` tenha terminado de processar).

```
Timeline:
  07:00 — Usuário acorda
  07:05 — Usuário preenche pós-sono → post_sleep_log gravado para sleep_epoch_day X
  07:10 — Sleep API emite SleepSegmentEvent → SleepSessionWorker enfileirado
  07:11 — SleepSessionWorker executa → sleep_session gravada para sleep_epoch_day X
```

**Resolução:**

O `post_sleep_log` é gravado com `sleep_epoch_day` calculado pela regra de corte (ver PRD seção 3.1). Não há problema — as tabelas são independentes e se unem via `sleep_epoch_day`.

Quando o `SleepSessionWorker` termina 6 minutos depois, ele grava a `sleep_session` com o **mesmo** `sleep_epoch_day`. O JOIN nas DAOs passa a retornar dados completos automaticamente.

O `AiCallWorker` já pode ter sido enfileirado com a submissão do pós-sono. Ele executará após o pós-sono ser gravado. Se a `sleep_session` não existir ainda quando o `AiCallWorker` verificar as condições, ele inclui a noite com `sleep = null` no `NightDataAggregate`. A IA ou o `LocalInsightEngine` analisarão os dados de hábitos e humor sem a duração objetiva — aceitável.

**Regra de conciliação C-01:** Nenhuma ação especial necessária. O JOIN por `sleep_epoch_day` é eventual — os dados se unem automaticamente quando ambos os registros existirem. A UI atualiza via `Flow` do Room.

---

### Cenário C-02: Sleep API emite evento para a noite errada (sleep_epoch_day incorreto)

**Situação:** O usuário dormiu na madrugada de segunda para terça. O Sleep API emite o evento às 01:30 da terça. O `SleepSessionWorker` calcula o `sleep_epoch_day` como o epoch day de **segunda** (regra de corte: hora local < 04:00 → dia anterior). Mas o pré-sono foi preenchido às 23:30 de segunda com `sleep_epoch_day` = epoch day de segunda.

**Resolução:** Os dois registros usam a mesma regra de cálculo de `sleep_epoch_day` (via `TimeUtils.resolveSleepEpochDay()`), então chegam naturalmente ao mesmo valor — epoch day de segunda. O JOIN funciona.

**Regra de conciliação C-02:** A consistência depende de **sempre usar `TimeUtils.resolveSleepEpochDay()`** para calcular o `sleep_epoch_day`, nunca hardcodando lógica de data em pontos diferentes do código.

---

### Cenário C-03: Dois eventos de sono para o mesmo `sleep_epoch_day`

**Situação:** O Sleep API emite um evento de atualização incremental — primeiro emite `ASLEEP` às 23:00, depois uma atualização consolidada com `AWAKE` às 07:00. O `SleepReceiver` dispara `SleepSessionWorker` duas vezes para o mesmo `sleep_epoch_day`.

**Resolução:** A DAO usa `@Insert(onConflict = OnConflictStrategy.REPLACE)` para `sleep_session`. A segunda execução do `SleepSessionWorker` sobrescreve a primeira com os dados consolidados e mais completos.

**Regra de conciliação C-03:** A ordenação não importa — REPLACE garante que o estado final seja sempre o mais recente processado. O `SleepSessionWorker` deve processar a lista completa de `SleepSegmentEvent` recebida no `inputData` e calcular os valores consolidados (duração total, interruption_count) a partir de todos os segmentos disponíveis.

---

### Cenário C-04: `AiCallWorker` enfileirado mas Sleep API ainda não entregou a sessão

**Situação:** O usuário preenche pós-sono às 07:30 → `AiCallWorker` enfileirado. A `sleep_session` só é gravada às 08:00 (após o `SleepSessionWorker` processar). O `AiCallWorker` executa às 07:45 sem `sleep_session`.

**Resolução:**

O `AiCallWorker` inclui a noite atual no `NightDataAggregate` com `sleep = null`. O `PromptBuilder` inclui a noite parcial (apenas pré-sono e pós-sono sem dados de sensor). A contagem de "noites completas" (condição mínima de 3 noites) verifica se `sleep_session` existe — se a noite atual não tem sessão ainda, ela não conta para o mínimo mas **pode** ser incluída como dado parcial se as outras 3 noites estiverem completas.

A IA recebe contexto parcial para a noite mais recente, o que é aceitável — ela tem histórico completo das noites anteriores.

**Regra de conciliação C-04:** O `AiCallWorker` não deve falhar por ausência de `sleep_session` na noite mais recente. `sleep` é `null` no `NightDataAggregate` — o `PromptBuilder` trata campos `null` omitindo-os do JSON ou marcando como `"not_available"`.

---

### Cenário C-05: Re-submissão do formulário pós-sono após o `AiCallWorker` já ter executado

**Situação:** O usuário preenche pós-sono às 08:00 → `AiCallWorker` executa às 08:05 e gera um desafio. Às 09:00 o usuário "se arrependeu" e muda a avaliação de humor de 2 para 4.

**Resolução:**

O REPLACE no `post_sleep_log` atualiza o registro. O desafio gerado **não é recriado** — o `AiCallWorker` verifica se já há um desafio `ACTIVE` gerado hoje antes de executar. Se sim, não gera um novo.

O `ChallengeEvaluationWorker` usa os dados mais recentes do `post_sleep_log` ao avaliar diariamente — então a correção do usuário será considerada na próxima avaliação diária.

**Regra de conciliação C-05:** Uma re-submissão do pós-sono não recria o desafio do dia. O desafio gerado no primeiro `AiCallWorker` permanece. Os dados corrigidos entram na avaliação diária do `ChallengeEvaluationWorker`.

---

### Cenário C-06: Usuário viaja para outro fuso horário durante o desafio

**Situação:** Desafio ativo de 7 dias. No dia 4, o usuário viaja de Brasília (UTC-3) para São Paulo (mesmo fuso, ok). No dia 5, viaja para Lisboa (UTC+1). O `ZoneId.systemDefault()` muda.

**Resolução:**

Cada registro é gravado com o `timezone_id` do momento do preenchimento — este valor é imutável. O `sleep_epoch_day` de cada noite é calculado com o timezone correto do momento do evento. Noites em Lisboa terão `timezone_id = "Europe/Lisbon"`.

Os campos `_local_hour` (ex: `caffeine_last_intake_local_hour`) são calculados no write com o timezone local correto de cada noite. Queries de "cafeína após 14h local" funcionam corretamente porque comparam o valor em hora local já armazenado.

**Regra de conciliação C-06:** Nenhuma ação especial. O design de "timezone no momento do evento" é exatamente para este cenário — cada noite carrega seu próprio contexto de fuso.

---

### Cenário C-07: Permissão `ACTIVITY_RECOGNITION` revogada durante desafio ativo

**Situação:** O usuário revoga a permissão nas configurações do Android enquanto há um desafio em curso.

**Resolução:**

`SleepApiManager.registerForUpdates()` falha silenciosamente ao tentar registrar (chama `unregisterFromUpdates()` como precaução). O app detecta a ausência da permissão no próximo `onResume()` do `MainActivity` via `PermissionState`.

O desafio continua ativo. Noites sem `sleep_session` (por falta de sensor) são marcadas como `NO_SLEEP_DATA` — se o usuário preencher pré-sono e pós-sono, os dados subjetivos são capturados e o desafio pode ser avaliado com base neles (sem dados de duração objetiva).

A `ChallengeScreen` exibe um banner: "O monitoramento de sono está desativado. Seus hábitos ainda estão sendo rastreados."

**Regra de conciliação C-07:** A avaliação do desafio não depende exclusivamente do sensor — `SLEEP_DURATION` como `success_metric_type` ficará com `outcome_metric_value = null` para noites sem sessão, mas `MOOD_SCORE`, `ENERGY_LEVEL` e `PERCEIVED_QUALITY` funcionam normalmente.

---

### Cenário C-08: Banco de dados com menos de 3 noites completas (estado inicial)

**Situação:** App instalado há 2 dias. O `AiCallWorker` executa mas a condição "≥3 noites completas" não é atendida.

**Resolução:**

O `AiCallWorker` verifica a condição antes de qualquer chamada. Com < 3 noites:
- Não chama a API do LLM (janela insuficiente para análise de padrões)
- Não chama o `LocalInsightEngine` (mínimo de 3 noites para heurísticas)
- Ativa o `StaticChallengeProvider` imediatamente, gerando um desafio genérico seguro

O desafio estático é marcado com `source = "STATIC_DEFAULT"` e badge "Dica Geral" na UI.

**Regra de conciliação C-08:** O app nunca fica sem conteúdo para o usuário, mesmo no primeiro dia. O `StaticChallengeProvider` garante que sempre haja um desafio exibido.

---

## 5. Diagrama de Sequência: Ciclo Completo de uma Noite Típica

```
Usuário       App UI       Repository      WorkManager      Room DB     LLM API
   │             │              │               │               │           │
   │ preenche    │              │               │               │           │
   │ pré-sono    │              │               │               │           │
   │────────────►│              │               │               │           │
   │             │ upsert()     │               │               │           │
   │             │─────────────►│               │               │           │
   │             │              │ INSERT/REPLACE│               │           │
   │             │              │──────────────────────────────►│           │
   │             │              │               │               │           │
   │ [dorme]     │              │               │               │           │
   │             │              │               │               │           │
   │             │   [Sleep API emite SleepSegmentEvent]        │           │
   │             │              │         enqueue()             │           │
   │             │              │◄──────────────│               │           │
   │             │              │        SleepSessionWorker     │           │
   │             │              │               │ processa      │           │
   │             │              │               │ INSERT/REPLACE│           │
   │             │              │               │──────────────►│           │
   │             │              │               │               │           │
   │ [acorda]    │              │               │               │           │
   │             │              │               │               │           │
   │ preenche    │              │               │               │           │
   │ pós-sono    │              │               │               │           │
   │────────────►│              │               │               │           │
   │             │ upsert()     │               │               │           │
   │             │─────────────►│               │               │           │
   │             │              │ INSERT/REPLACE│               │           │
   │             │              │──────────────────────────────►│           │
   │             │              │               │               │           │
   │             │              │ enqueue()     │               │           │
   │             │              │──────────────►│               │           │
   │             │              │          AiCallWorker         │           │
   │             │              │               │ lê histórico  │           │
   │             │              │               │◄─────────────►│           │
   │             │              │               │ chama API     │           │
   │             │              │               │──────────────────────────►│
   │             │              │               │               │  JSON     │
   │             │              │               │◄──────────────────────────│
   │             │              │               │ sanitiza+valida           │
   │             │              │               │ INSERT challenge          │
   │             │              │               │──────────────►│           │
   │             │              │               │               │           │
   │             │ Flow emite   │               │               │           │
   │             │◄─────────────────────────────────────────────│           │
   │ vê desafio  │              │               │               │           │
   │◄────────────│              │               │               │           │
```
