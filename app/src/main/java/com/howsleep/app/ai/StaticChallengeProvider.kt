package com.howsleep.app.ai

object StaticChallengeProvider {

    private val CHALLENGES = listOf(
        ChallengeSuggestion(
            title = "Corte a Cafeína Tarde",
            description = "A cafeína tem meia-vida de ~5h no organismo. Consumida após as 14h, ela ainda está ativa quando você tenta dormir.",
            habitToChange = "CAFFEINE",
            habitChangeInstruction = "Não consuma cafeína (café, chá preto, energético) após as 14:00.",
            durationDays = 7,
            successMetricType = "PERCEIVED_QUALITY",
            successMetricTarget = 3.5f,
            successMetricDirection = "ABOVE",
            source = "STATIC_DEFAULT",
        ),
        ChallengeSuggestion(
            title = "Detox de Telas",
            description = "A luz azul de telas suprime melatonina por até 2h após a exposição, atrasando o sono profundo.",
            habitToChange = "SCREEN_TIME",
            habitChangeInstruction = "Use telas por no máximo 30 minutos nas 2h antes de dormir.",
            durationDays = 7,
            successMetricType = "PERCEIVED_QUALITY",
            successMetricTarget = 3.5f,
            successMetricDirection = "ABOVE",
            source = "STATIC_DEFAULT",
        ),
        ChallengeSuggestion(
            title = "Horário Regular de Dormir",
            description = "O ritmo circadiano prefere horários consistentes. Variações de mais de 1h reduzem a eficiência do sono.",
            habitToChange = "SLEEP_SCHEDULE",
            habitChangeInstruction = "Deite-se e acorde no mesmo horário todos os dias (variação máxima de 30 min).",
            durationDays = 7,
            successMetricType = "ENERGY_LEVEL",
            successMetricTarget = 3.5f,
            successMetricDirection = "ABOVE",
            source = "STATIC_DEFAULT",
        ),
        ChallengeSuggestion(
            title = "Jante Mais Cedo",
            description = "Jantar perto da hora de dormir eleva a temperatura corporal e ativa a digestão, reduzindo o sono profundo.",
            habitToChange = "MEAL_TIMING",
            habitChangeInstruction = "Faça a última refeição pelo menos 3 horas antes de se deitar.",
            durationDays = 7,
            successMetricType = "PERCEIVED_QUALITY",
            successMetricTarget = 3.5f,
            successMetricDirection = "ABOVE",
            source = "STATIC_DEFAULT",
        ),
        ChallengeSuggestion(
            title = "Movimento Diário",
            description = "Exercício regular melhora a qualidade do sono profundo e reduz o tempo para adormecer.",
            habitToChange = "EXERCISE",
            habitChangeInstruction = "Pratique pelo menos 20 minutos de atividade física por dia (até 4h antes de dormir).",
            durationDays = 7,
            successMetricType = "SLEEP_DURATION",
            successMetricTarget = 7.0f,
            successMetricDirection = "ABOVE",
            source = "STATIC_DEFAULT",
        ),
        ChallengeSuggestion(
            title = "Reduza o Estresse Diário",
            description = "Estresse elevado mantém o cortisol alto à noite, dificultando o relaxamento necessário para adormecer.",
            habitToChange = "STRESS",
            habitChangeInstruction = "Pratique 10 minutos de respiração profunda ou meditação antes de dormir.",
            durationDays = 7,
            successMetricType = "MOOD_SCORE",
            successMetricTarget = 3.5f,
            successMetricDirection = "ABOVE",
            source = "STATIC_DEFAULT",
        ),
        ChallengeSuggestion(
            title = "Pausa no Álcool",
            description = "O álcool fragmenta o sono REM e aumenta despertares na segunda metade da noite, prejudicando a recuperação.",
            habitToChange = "ALCOHOL",
            habitChangeInstruction = "Evite qualquer consumo de álcool durante o desafio.",
            durationDays = 7,
            successMetricType = "ENERGY_LEVEL",
            successMetricTarget = 3.5f,
            successMetricDirection = "ABOVE",
            source = "STATIC_DEFAULT",
        ),
    )

    fun getNext(seed: Long = System.currentTimeMillis()): ChallengeSuggestion {
        val index = ((seed / 86_400_000L) % CHALLENGES.size).toInt()
        return CHALLENGES[index.coerceIn(0, CHALLENGES.lastIndex)]
    }
}
