package com.howsleep.app.ai

import com.howsleep.app.domain.model.NightDataAggregate

object LocalInsightEngine {

    fun analyze(nights: List<NightDataAggregate>): ChallengeSuggestion? {
        if (nights.size < 3) return null

        val scores = listOf(
            scoreCaffeine(nights),
            scoreScreenTime(nights),
            scoreStress(nights),
            scoreMealTiming(nights),
            scoreAlcohol(nights),
            scoreExercise(nights),
        )

        val best = scores.maxByOrNull { it?.score ?: -1f } ?: return null
        return best?.suggestion
    }

    private data class HabitScore(val score: Float, val suggestion: ChallengeSuggestion)

    private fun avgOutcome(nights: List<NightDataAggregate>): Float? {
        val values = nights.mapNotNull { n ->
            val scores = listOfNotNull(n.perceivedQuality, n.moodScore, n.energyLevel)
            if (scores.isEmpty()) null else scores.average().toFloat()
        }
        return if (values.isEmpty()) null else values.average().toFloat()
    }

    private fun scoreCaffeine(nights: List<NightDataAggregate>): HabitScore? {
        val bad = nights.filter { (it.caffeineMg ?: 0) > 0 && (it.caffeineLastIntakeLocalHour ?: 0) >= 14 }
        if (bad.size < 2) return null
        val badAvg = avgOutcome(bad) ?: return null
        val goodAvg = avgOutcome(nights.filter { !bad.contains(it) }) ?: (badAvg + 0.5f)
        val impact = goodAvg - badAvg
        if (impact <= 0) return null
        return HabitScore(
            score = impact * bad.size,
            suggestion = ChallengeSuggestion(
                title = "Corte a Cafeína Tarde",
                description = "Você consumiu cafeína após as 14h em ${bad.size} das últimas ${nights.size} noites. Isso atrasa a produção de melatonina e fragmenta o sono.",
                habitToChange = "CAFFEINE",
                habitChangeInstruction = "Não consuma cafeína (café, chá preto, energético) após as 14:00.",
                durationDays = 7,
                successMetricType = "PERCEIVED_QUALITY",
                successMetricTarget = (badAvg + 1f).coerceAtMost(5f),
                successMetricDirection = "ABOVE",
                source = "LOCAL_ENGINE",
            )
        )
    }

    private fun scoreScreenTime(nights: List<NightDataAggregate>): HabitScore? {
        val bad = nights.filter { (it.screenTimeMinutes2hBefore ?: 0) > 60 }
        if (bad.size < 2) return null
        val badAvg = avgOutcome(bad) ?: return null
        val goodAvg = avgOutcome(nights.filter { !bad.contains(it) }) ?: (badAvg + 0.5f)
        val impact = goodAvg - badAvg
        if (impact <= 0) return null
        return HabitScore(
            score = impact * bad.size,
            suggestion = ChallengeSuggestion(
                title = "Menos Telas Antes de Dormir",
                description = "Em ${bad.size} das ${nights.size} noites você usou telas por mais de 1h antes de dormir. A luz azul suprime melatonina e dificulta o adormecer.",
                habitToChange = "SCREEN_TIME",
                habitChangeInstruction = "Limite o uso de telas a no máximo 30 minutos nas 2h antes de dormir.",
                durationDays = 7,
                successMetricType = "PERCEIVED_QUALITY",
                successMetricTarget = (badAvg + 1f).coerceAtMost(5f),
                successMetricDirection = "ABOVE",
                source = "LOCAL_ENGINE",
            )
        )
    }

    private fun scoreStress(nights: List<NightDataAggregate>): HabitScore? {
        val bad = nights.filter { (it.stressLevel ?: 0) >= 4 }
        if (bad.size < 2) return null
        val badAvg = avgOutcome(bad) ?: return null
        val goodAvg = avgOutcome(nights.filter { !bad.contains(it) }) ?: (badAvg + 0.5f)
        val impact = goodAvg - badAvg
        if (impact <= 0) return null
        return HabitScore(
            score = impact * bad.size,
            suggestion = ChallengeSuggestion(
                title = "Reduza o Estresse Diário",
                description = "Em ${bad.size} noites seu estresse estava acima de 3/5. Estresse elevado aumenta o cortisol e dificulta a entrada no sono profundo.",
                habitToChange = "STRESS",
                habitChangeInstruction = "Pratique 10 minutos de respiração profunda ou meditação antes de dormir.",
                durationDays = 7,
                successMetricType = "MOOD_SCORE",
                successMetricTarget = (badAvg + 1f).coerceAtMost(5f),
                successMetricDirection = "ABOVE",
                source = "LOCAL_ENGINE",
            )
        )
    }

    private fun scoreMealTiming(nights: List<NightDataAggregate>): HabitScore? {
        val bad = nights.filter { n ->
            val hours = n.hoursSinceLastMeal ?: return@filter false
            hours < 3.0f
        }
        if (bad.size < 2) return null
        val badAvg = avgOutcome(bad) ?: return null
        val goodAvg = avgOutcome(nights.filter { !bad.contains(it) }) ?: (badAvg + 0.5f)
        val impact = goodAvg - badAvg
        if (impact <= 0) return null
        return HabitScore(
            score = impact * bad.size,
            suggestion = ChallengeSuggestion(
                title = "Jante Mais Cedo",
                description = "Em ${bad.size} noites você jantou menos de 3h antes de dormir. A digestão ativa interfere na qualidade do sono profundo.",
                habitToChange = "MEAL_TIMING",
                habitChangeInstruction = "Faça a última refeição pelo menos 3 horas antes de se deitar.",
                durationDays = 7,
                successMetricType = "PERCEIVED_QUALITY",
                successMetricTarget = (badAvg + 1f).coerceAtMost(5f),
                successMetricDirection = "ABOVE",
                source = "LOCAL_ENGINE",
            )
        )
    }

    private fun scoreAlcohol(nights: List<NightDataAggregate>): HabitScore? {
        val bad = nights.filter { (it.alcoholUnits ?: 0f) > 0f }
        if (bad.size < 2) return null
        val badAvg = avgOutcome(bad) ?: return null
        val goodAvg = avgOutcome(nights.filter { !bad.contains(it) }) ?: (badAvg + 0.5f)
        val impact = goodAvg - badAvg
        if (impact <= 0) return null
        return HabitScore(
            score = impact * bad.size,
            suggestion = ChallengeSuggestion(
                title = "Pausa no Álcool",
                description = "Em ${bad.size} noites você consumiu álcool. O álcool fragmenta o sono REM e aumenta despertares noturnos.",
                habitToChange = "ALCOHOL",
                habitChangeInstruction = "Evite álcool completamente nas próximas noites do desafio.",
                durationDays = 7,
                successMetricType = "ENERGY_LEVEL",
                successMetricTarget = (badAvg + 1f).coerceAtMost(5f),
                successMetricDirection = "ABOVE",
                source = "LOCAL_ENGINE",
            )
        )
    }

    private fun scoreExercise(nights: List<NightDataAggregate>): HabitScore? {
        val withExercise = nights.filter { it.exerciseDone == true }
        val withoutExercise = nights.filter { it.exerciseDone == false }
        if (withoutExercise.size < 2) return null
        val withAvg = avgOutcome(withExercise) ?: return null
        val withoutAvg = avgOutcome(withoutExercise) ?: return null
        val impact = withAvg - withoutAvg
        if (impact <= 0.3f) return null
        return HabitScore(
            score = impact * withoutExercise.size,
            suggestion = ChallengeSuggestion(
                title = "Movimento Diário",
                description = "Noites em que você se exercitou tiveram melhor qualidade de sono. Exercício regular aprofunda o sono e reduz o tempo para adormecer.",
                habitToChange = "EXERCISE",
                habitChangeInstruction = "Pratique pelo menos 20 minutos de atividade física por dia (até 4h antes de dormir).",
                durationDays = 7,
                successMetricType = "SLEEP_DURATION",
                successMetricTarget = 7.0f,
                successMetricDirection = "ABOVE",
                source = "LOCAL_ENGINE",
            )
        )
    }
}
