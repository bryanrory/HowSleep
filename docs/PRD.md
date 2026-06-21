# PRD — Product Requirements Document & Regras de Negócio
## HowSleep: Monitor de Sono Inteligente e Comportamental

**Versão:** 1.0  
**Data:** 2026-06-20  
**Status:** Aprovado para implementação

---

## 1. Visão do Produto

O HowSleep é um aplicativo Android que atua como um **investigador de sono**: em vez de apenas medir quanto o usuário dormiu, o app cruza **hábitos comportamentais** (cafeína, telas, estresse, alimentação, exercício) com a **qualidade objetiva do sono** (via Android Sleep API) e o **estado subjetivo ao acordar** (mood, energia), gerando desafios personalizados via IA e validando matematicamente se as mudanças funcionaram.

### Proposta de Valor Central

| Problema | Solução |
|---|---|
| Apps de sono medem, mas não explicam | HowSleep investiga correlações entre hábitos e qualidade do sono |
| Dicas genéricas de "higiene do sono" | Desafios personalizados baseados nos dados reais do usuário |
| Usuário não sabe se mudou algo | Validação automática com comparação baseline vs. resultado |

---

## 2. Jornada Completa do Usuário

### 2.1 Onboarding (única vez)

```
[Tela de Boas-Vindas]
    ↓
[Explicação do Conceito — 3 slides]
    "O HowSleep aprende seus hábitos e descobre o que rouba seu sono"
    ↓
[Solicitação de Permissão: ACTIVITY_RECOGNITION]
    → Concedida → Registro no Sleep API → [Tela Principal]
    → Negada   → Tela de aviso + botão "Entender por que precisamos"
                 → Opção de usar "Modo Manual" (inserção manual de horários)
    ↓
[Configuração Inicial]
    - Horário estimado de dormir (usado para lembrete pré-sono)
    - Horário estimado de acordar (usado para lembrete pós-sono)
    - Fuso horário: capturado automaticamente via ZoneId.systemDefault()
    ↓
[Tela Principal — Dashboard]
```

**Regra de negócio OB-01:** O onboarding só é exibido uma vez. A flag `ONBOARDING_COMPLETED` é persistida no DataStore após o usuário configurar o horário. Se o app for reinstalado, o onboarding é exibido novamente (banco de dados é apagado com a reinstalação).

**Regra de negócio OB-02:** A permissão `ACTIVITY_RECOGNITION` é obrigatória para o funcionamento principal. O app não deve ser bloqueado sem ela — o "Modo Manual" deve ser oferecido como alternativa viável, onde o usuário insere os horários de início e fim do sono manualmente.

---

### 2.2 Ciclo Diário — Noite Típica

O ciclo se repete toda noite. Cada ciclo é identificado por um `sleep_epoch_day` (o epoch day da data local em que o sono **começa**).

```
┌─────────────────────────────────────────────────────────┐
│  20:00–23:00  Janela de Formulário PRÉ-SONO             │
│               Usuário preenche hábitos do dia           │
├─────────────────────────────────────────────────────────┤
│  23:00–07:00  SONO — Android Sleep API monitora         │
│               (ou MockSleepWorker em modo DEBUG)        │
├─────────────────────────────────────────────────────────┤
│  07:00–12:00  Janela de Formulário PÓS-SONO             │
│               Usuário preenche humor/energia ao acordar │
├─────────────────────────────────────────────────────────┤
│  Após submissão do Pós-Sono                             │
│               AiCallWorker verifica condições           │
│               → Chama LLM API OU LocalInsightEngine     │
│               → Persiste AiChallenge                    │
└─────────────────────────────────────────────────────────┘
```

---

### 2.3 Formulário Pré-Sono

**Quando exibir:** Notificação enviada `BEDTIME_REMINDER_OFFSET_MINUTES` (padrão: 30 min) antes do horário de dormir configurado. O formulário fica disponível a qualquer momento via botão "Registrar minha noite" na tela principal.

**Campos obrigatórios:**
- Nível de estresse do dia (1–5)
- Exercício realizado hoje? (Sim/Não)

**Campos opcionais (mas recomendados):**
- Cafeína: quantidade (0mg / ~80mg / ~160mg / ~240mg+) e horário da última dose
- Tempo de tela nas 2h antes de dormir (em minutos)
- Última refeição: tipo (Leve/Pesada/Lanche) e horário
- Se exercício: intensidade (Baixa/Moderada/Alta) e minutos antes de dormir
- Álcool: doses padrão consumidas
- Notas livres (máx. 500 caracteres)

**Regra de negócio PS-01:** O formulário pré-sono pode ser preenchido e **editado** quantas vezes o usuário quiser até o horário de corte da noite (definido como 04:00 local do dia seguinte). A DAO usa `REPLACE` por `sleep_epoch_day` — a última submissão vence.

**Regra de negócio PS-02:** O campo `sleep_epoch_day` para o pré-sono é sempre o epoch day do **dia atual local** no momento do preenchimento, exceto se preenchido entre 00:00 e 04:00, quando pertence ao dia anterior (a noite que acabou de terminar). Ver seção 3.1 para o algoritmo completo.

---

### 2.4 Monitoramento do Sono (Sleep API)

O usuário não precisa fazer nada. O Android Sleep API monitora passivamente e emite broadcasts de `SleepSegmentEvent` quando detecta transições ASLEEP/AWAKE.

**Fluxo técnico:**
1. `SleepApiManager.registerForUpdates()` — chamado no onboarding e a cada boot (via `RECEIVE_BOOT_COMPLETED`)
2. `SleepReceiver.onReceive()` — captura o broadcast, serializa eventos para JSON, enfileira `SleepSessionWorker`
3. `SleepSessionWorker` — processa eventos, calcula duração, grava `SleepSessionEntity`

**Regra de negócio SO-01:** O Sleep API pode emitir múltiplos eventos para a mesma noite (atualização incremental). O `SleepSessionWorker` deve fazer `REPLACE` na `sleep_session` pelo `sleep_epoch_day`, consolidando sempre com os dados mais recentes.

**Regra de negócio SO-02:** Se o `confidence` do Sleep API for inferior a 50, a sessão é gravada mas marcada com flag `low_confidence = true`. A UI exibe um aviso "Dados de sono incertos" para essa noite.

---

### 2.5 Formulário Pós-Sono

**Quando exibir:** Notificação enviada `WAKEUP_REMINDER_OFFSET_MINUTES` (padrão: 15 min) após o horário de acordar configurado.

**Campos obrigatórios:**
- Humor ao acordar (1–5)
- Nível de energia (1–5)
- Qualidade percebida do sono (1–5)

**Campos opcionais:**
- Estimativa de horas dormidas (para comparar com o dado do sensor)
- Tempo para se sentir desperto (minutos)
- Recordou sonhos? (Sim/Não)
- Acordou com dor de cabeça? (Sim/Não)
- Notas livres (máx. 500 caracteres)

**Regra de negócio PO-01 — Janela de Corte (12:00):** O formulário pós-sono submetido até as **12:00 local** é associado à noite anterior (`sleep_epoch_day = epoch day de ontem`). Submetido após as 12:00, pertence à noite atual (incomum, mas possível para quem trabalha à noite). Ver seção 3.1.

**Regra de negócio PO-02:** O formulário pós-sono também aceita re-submissão via `REPLACE`. O usuário pode corrigir uma avaliação equivocada até o final do dia (23:59 local).

---

### 2.6 Tela de Insights e Desafios

Após a submissão do pós-sono, o app exibe:

1. **Resumo da Noite:** duração detectada, qualidade percebida, mood, energia
2. **Insight do Dia:** gerado pela IA ou LocalInsightEngine (com badge de origem se não for IA)
3. **Desafio Ativo:** card com título, instrução, progresso (X de Y dias), e a métrica que será medida
4. **Histórico:** últimas 7 noites em formato de cards compactos

---

## 3. Regras de Janela de Tempo e Limites

### 3.1 Algoritmo de Atribuição de `sleep_epoch_day`

Este é o algoritmo executado em `TimeUtils.resolveSleepEpochDay()` a cada escrita no banco:

```
Dado: timestamp UTC do evento (preenchimento do formulário ou início do sono)
Dado: timezone_id do dispositivo

Passo 1: Converter timestamp para hora local
    localDateTime = Instant.ofEpochMilli(utcMs).atZone(ZoneId.of(timezoneId))
    localHour = localDateTime.hour
    localDate = localDateTime.toLocalDate()

Passo 2: Aplicar regra de corte
    SE localHour >= 0 E localHour < 4:
        → sleep_epoch_day = (localDate - 1 dia).toEpochDay()   [pertence à noite anterior]
    SENÃO:
        → sleep_epoch_day = localDate.toEpochDay()              [pertence à noite atual]

Exceção para Pós-Sono (regra das 12:00):
    SE formulário é post_sleep_log E localHour >= 12:
        → sleep_epoch_day = localDate.toEpochDay()              [noite atual — caso raro, turno noturno]
    SE formulário é post_sleep_log E localHour < 12:
        → sleep_epoch_day = (localDate - 1 dia).toEpochDay()   [noite anterior — caso mais comum]
```

**Casos concretos:**

| Cenário | Horário Local | sleep_epoch_day |
|---|---|---|
| Usuário preenche pré-sono às 22:30 de segunda | 22:30 seg. | epoch day de segunda |
| Usuário preenche pré-sono à meia-noite de segunda (00:15 de terça) | 00:15 ter. | epoch day de segunda |
| Usuário acorda às 07:30 de terça e preenche pós-sono | 07:30 ter. | epoch day de segunda |
| Usuário acorda às 13:00 de terça (soneca longa) e preenche pós-sono | 13:00 ter. | epoch day de terça |
| Sleep API emite evento de fim de sono às 06:45 de terça | 06:45 ter. | epoch day de segunda |

---

### 3.2 Tratamento de Logs Esquecidos

**Cenário: Usuário esqueceu o pré-sono**

- O app não bloqueia o ciclo. O `pre_sleep_log` para aquela noite simplesmente não existe.
- O `sleep_session` e o `post_sleep_log` são gravados normalmente.
- Na query para o `PromptBuilder`, noites sem `pre_sleep_log` são incluídas com os campos de hábito como `null`.
- A IA e o `LocalInsightEngine` ignoram campos `null` nas heurísticas — uma noite sem pré-sono contribui apenas com os dados de sono e pós-sono.
- **Regra de negócio LF-01:** O app exibe um lembrete gentil (não-bloqueante) na tela de pós-sono: "Você não registrou seus hábitos de ontem. Deseja preenchê-los agora? (os dados de hábitos ainda podem ser úteis)". O usuário pode preencher retroativamente até 24h após o `sleep_epoch_day`.

**Cenário: Usuário esqueceu o pós-sono**

- O `AiCallWorker` **não é disparado** sem o pós-sono. Os dados de humor/energia são essenciais para a análise.
- O app exibe notificação de follow-up às 14:00 do mesmo dia: "Você esqueceu de avaliar sua noite de ontem."
- Se o pós-sono não for preenchido até as 23:59 do mesmo dia, a noite é marcada como `INCOMPLETE` no estado da noite (ver State Machine).
- **Regra de negócio LF-02:** Noites `INCOMPLETE` não entram no cálculo do `PromptBuilder` nem nas heurísticas do `LocalInsightEngine`. Entram apenas no dashboard visual (com indicador de "noite incompleta").

**Cenário: Usuário não dormiu ou dormiu fora de casa**

- O usuário pode tocar "Pular essa noite" no lembrete de pós-sono.
- O estado da noite é marcado como `SKIPPED`.
- Noites `SKIPPED` não entram nos cálculos de IA ou heurísticas.

---

### 3.3 Limites e Validações de Formulário

| Campo | Limite/Validação | Mensagem de Erro |
|---|---|---|
| `stress_level` | 1–5, obrigatório | "Selecione seu nível de estresse" |
| `caffeine_mg` | 0, 80, 160, 240+ (presets) | — (seleção por chips) |
| `caffeine_last_intake_local_hour` | 0–23, opcional | — |
| `screen_time_minutes_2h_before` | 0–120 minutos (slider) | — |
| `alcohol_units` | 0.0–10.0 (step 0.5) | — |
| `notes` | máx. 500 caracteres | "Máximo de 500 caracteres atingido" |
| `mood_score` | 1–5, obrigatório | "Selecione seu humor" |
| `energy_level` | 1–5, obrigatório | "Selecione seu nível de energia" |
| `perceived_quality` | 1–5, obrigatório | "Avalie a qualidade do seu sono" |

---

## 4. Algoritmo de Avaliação do Desafio

### 4.1 Visão Geral

A avaliação é executada pelo `ChallengeEvaluationWorker` diariamente (disparado após a submissão do pós-sono enquanto há um desafio ativo) e no último dia do desafio para o veredicto final.

### 4.2 Cálculo de Adesão ao Hábito (≥70%)

**Definição:** Para cada dia do desafio que possui um `pre_sleep_log` associado, verifica se o usuário seguiu a instrução do desafio.

```
Para cada challenge_day_log do desafio:

    1. Buscar pre_sleep_log pelo sleep_epoch_day
    2. Se pre_sleep_log == null → habit_followed = null (dia ignorado — sem dados)
    3. Se pre_sleep_log != null → avaliar por habit_to_change:

CAFFEINE:
    habit_followed = (caffeine_last_intake_local_hour == null)  [sem cafeína]
                  OR (caffeine_last_intake_local_hour < 14)      [antes das 14h]

SCREEN_TIME:
    habit_followed = (screen_time_minutes_2h_before == null)    [não registrado → benefit of doubt = false]
                  OR (screen_time_minutes_2h_before <= 30)

STRESS:
    habit_followed = (stress_level <= 2)

MEAL_TIMING:
    hours_since_last_meal = (sleep_start_utc_ms - last_meal_utc_ms) / 3_600_000.0
    habit_followed = (last_meal_utc_ms == null) ? null : (hours_since_last_meal >= 3.0)

EXERCISE:
    habit_followed = exercise_done

ALCOHOL:
    habit_followed = (alcohol_units == null OR alcohol_units == 0.0)

SLEEP_SCHEDULE:
    Não é inferível do pre_sleep_log → habit_followed = null (requer confirmação manual do usuário)
    [A UI exibe um toggle "Cumpri o horário de dormir hoje?" que o usuário preenche]
```

**Cálculo da taxa de adesão:**

```
dias_avaliados = count(challenge_day_log WHERE habit_followed IS NOT NULL)
dias_seguidos  = count(challenge_day_log WHERE habit_followed = true)

SE dias_avaliados == 0 → desafio não pode ser avaliado → status = EXPIRED
taxa_adesao = dias_seguidos / dias_avaliados

adesao_aprovada = (taxa_adesao >= 0.70)
```

**Importante:** Dias sem `pre_sleep_log` (habit_followed = null) são **excluídos do denominador**, não contados como falha. Isso evita penalizar o usuário por esquecer o formulário.

### 4.3 Cálculo de Melhora do Outcome (≥10%)

**Definição:** Compara a média da métrica de outcome durante o desafio com o `baseline_value` gravado no momento da criação do desafio.

```
baseline_value: média da success_metric_type nas últimas 3 noites ANTES do desafio iniciar
                (gravado em ai_challenge no momento da criação — snapshot imutável)

Para cada challenge_day_log com sleep_session e post_sleep_log associados:
    outcome_metric_value = valor da success_metric_type para aquela noite:

    SLEEP_DURATION    → sleep_session.total_duration_minutes / 60.0
    MOOD_SCORE        → post_sleep_log.mood_score
    ENERGY_LEVEL      → post_sleep_log.energy_level
    PERCEIVED_QUALITY → post_sleep_log.perceived_quality

media_outcome_desafio = AVG(outcome_metric_value) para dias com dados completos

Cálculo de variação:
    SE success_metric_direction == "ABOVE":
        delta_percentual = (media_outcome_desafio - baseline_value) / baseline_value * 100
        outcome_aprovado = (delta_percentual >= 10.0)

    SE success_metric_direction == "BELOW":
        delta_percentual = (baseline_value - media_outcome_desafio) / baseline_value * 100
        outcome_aprovado = (delta_percentual >= 10.0)
```

### 4.4 Veredicto Final

```
SE adesao_aprovada AND outcome_aprovado:
    → status = "COMPLETED"
    → Mensagem: "Você completou o desafio E seu sono melhorou! 🎉"

SE adesao_aprovada AND NOT outcome_aprovado:
    → status = "COMPLETED" (o usuário seguiu o desafio mas o efeito não foi detectado)
    → Mensagem: "Você manteve o hábito! O impacto no seu sono foi pequeno desta vez."

SE NOT adesao_aprovada AND outcome_aprovado:
    → status = "ABANDONED"
    → Mensagem: "Seu sono melhorou, mas o hábito não foi consistente."

SE NOT adesao_aprovada AND NOT outcome_aprovado:
    → status = "ABANDONED"
    → Mensagem: "Vamos tentar novamente. Amanhã é um novo começo."

SE dias_avaliados < (duration_days * 0.5):
    → status = "EXPIRED" (dados insuficientes para avaliar)
    → Mensagem: "Poucos dados para avaliar. Tente registrar seus hábitos com mais consistência."
```

### 4.5 Cálculo do Baseline

O `baseline_value` é calculado no momento da criação do `AiChallengeEntity` e **nunca muda** — é um snapshot do estado antes do desafio.

```
baseline_nights = últimas 3 noites ANTES de valid_from_date com dados completos
                  (sleep_session + post_sleep_log presentes)

baseline_value = AVG(success_metric_type) para essas noites

SE menos de 2 noites completas disponíveis:
    → Usar a média histórica global (todas as noites disponíveis)
    → Marcar ai_challenge.baseline_confidence = "LOW"
```

---

## 5. Regras de Privacidade e Dados

**PD-01:** Todos os dados são armazenados exclusivamente no dispositivo do usuário (Room Database local). Nenhum dado pessoal é enviado para servidores próprios.

**PD-02:** Os dados enviados para a API do LLM não incluem notas de texto livres, informações de identificação pessoal ou dados de localização — apenas os campos estruturados definidos no `PromptBuilder`.

**PD-03:** A chave da API do LLM é armazenada localmente em `BuildConfig` no MVP. Antes de qualquer release público, deve ser migrada para um proxy backend sob controle do desenvolvedor.

**PD-04:** O usuário pode deletar todos os dados locais via "Apagar todos os dados" nas Configurações. Esta ação é irreversível e exibe um diálogo de confirmação explícito.

---

## 6. Critérios de Aceite por Fase

### Fase 1 (Foundation)
- [ ] App instala sem crash em dispositivo físico com Android 7.0+
- [ ] Onboarding exibido apenas uma vez
- [ ] `ACTIVITY_RECOGNITION` solicitada com explicação clara
- [ ] Room Database compilando com todas as 5 entities via KSP
- [ ] Toggle de modo mock visível apenas em builds DEBUG

### Fase 2 (Core Loop)
- [ ] Formulário pré-sono salva dados corretamente com `sleep_epoch_day` calculado pelo timezone local
- [ ] Re-submissão do formulário no mesmo dia substitui sem criar duplicata
- [ ] `SleepSessionWorker` grava sessão de sono com duração e interruptions corretos
- [ ] Formulário pós-sono associado à noite correta via `sleep_epoch_day`
- [ ] Dashboard exibe últimas 7 noites com duração e qualidade percebida

### Fase 3 (AI Integration)
- [ ] `AiCallWorker` não dispara com menos de 3 noites completas
- [ ] JSON da IA com markdown é sanitizado e parseado sem erro
- [ ] Fallback para `LocalInsightEngine` quando sem internet
- [ ] Fallback para desafio estático quando sem dados suficientes
- [ ] Badge de origem exibido corretamente na `ChallengeScreen`

### Fase 4 (Polish)
- [ ] Notificação de lembrete pré-sono enviada no horário configurado
- [ ] `challenge_day_log` preenchido automaticamente sem ação do usuário
- [ ] Tela de tendências com gráfico de pelo menos 7 dias
- [ ] Veredicto de desafio exibe comparativo baseline vs. resultado com delta percentual
