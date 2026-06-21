# HowSleep

> **Não é mais um app de sono. É um investigador de hábitos.**

HowSleep é um monitor de sono inteligente e comportamental para Android. Em vez de apenas medir quanto você dormiu, o app cruza seus hábitos pré-sono — cafeína, telas, estresse, alimentação — com a qualidade objetiva do sono detectada nativamente pelo dispositivo, gera desafios personalizados via IA e valida matematicamente se as mudanças funcionaram.

---

## Como funciona

```
Hábitos do dia  →  Sono detectado  →  Estado ao acordar  →  Análise por IA  →  Desafio validado
```

1. **Registre seus hábitos** antes de dormir: cafeína, tempo de tela, estresse, exercício, alimentação
2. **Durma normalmente** — a Android Sleep API detecta início, fim e interrupções do sono de forma passiva
3. **Avalie sua manhã** ao acordar: humor, energia, qualidade percebida
4. **Receba um desafio** gerado por IA com base no seu padrão real dos últimos dias
5. **Veja o resultado** — o app compara sua métrica antes e depois do desafio e diz se funcionou

---

## Funcionalidades

- Detecção automática de sono via Android Sleep API (sem interação do usuário)
- Formulário pré-sono com registro de hábitos comportamentais
- Avaliação pós-sono de humor, energia e qualidade percebida
- Análise diária por LLM com janela móvel de até 30 noites
- Fallback local: motor de heurísticas analisa padrões sem internet
- Desafios com prazo, métrica de sucesso e validação automática de conclusão
- Dashboard com histórico de 7 noites e tendências de longo prazo
- Modo de desenvolvimento com simulação de sono (sem precisar dormir para testar)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitetura | MVVM + StateFlow |
| Banco de dados | Room (SQLite) |
| Injeção de dependência | Hilt |
| Background | WorkManager |
| Sensor de sono | Android Sleep API (ActivityRecognition) |
| HTTP / IA | Retrofit + kotlinx.serialization + LLM API |
| Estado persistente | DataStore Preferences |

---

## Estrutura do projeto

```
app/src/main/java/com/howsleep/app/
├── data/           Room entities, DAOs, repositórios, cliente HTTP
├── domain/         Modelos e utilitários puros (sem dependência Android)
├── ai/             PromptBuilder, sanitizador de resposta, motor de heurísticas locais
├── sleep/          Integração com Sleep API, BroadcastReceiver, Workers
├── worker/         Workers do WorkManager (IA, avaliação de desafio, notificações)
├── navigation/     NavGraph e rotas
└── ui/             Telas, ViewModels e componentes Compose
```

---

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/PRD.md`](docs/PRD.md) | Regras de negócio, jornadas do usuário, algoritmo de avaliação de desafio |
| [`docs/DATA_CONTRACTS.md`](docs/DATA_CONTRACTS.md) | Schema do banco de dados e exemplos de payload da API de IA |
| [`docs/STATE_MACHINE.md`](docs/STATE_MACHINE.md) | Estados de uma noite, ciclo de vida do desafio e resolução de conflitos |
| [`docs/ARCHITECTURE_GUIDE.md`](docs/ARCHITECTURE_GUIDE.md) | Padrões de código: MVVM, Hilt, Workers, navegação e testes |
| [`CLAUDE.md`](CLAUDE.md) | Diretrizes do repositório para desenvolvimento assistido por IA |

---

## Requisitos

- Android 7.0+ (API 24)
- Google Play Services (necessário para a Android Sleep API)
- Permissão `ACTIVITY_RECOGNITION` (solicitada no onboarding)

---

## Status do desenvolvimento

| Fase | Descrição | Status |
|---|---|---|
| Fase 1 | Foundation — Room, Hilt, navegação, mock do Sleep API | ⏳ Pendente |
| Fase 2 | Core Loop — formulários, detecção de sono, dashboard | ⏳ Pendente |
| Fase 3 | AI Integration — PromptBuilder, LLM API, fallback local | ⏳ Pendente |
| Fase 4 | Polish — notificações, tendências, histórico de desafios | ⏳ Pendente |

---

## Licença

Distribuído sob a licença MIT.
