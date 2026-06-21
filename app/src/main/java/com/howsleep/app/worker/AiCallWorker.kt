package com.howsleep.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.howsleep.app.ai.LocalInsightEngine
import com.howsleep.app.ai.StaticChallengeProvider
import com.howsleep.app.data.repository.AiChallengeRepository
import com.howsleep.app.data.repository.HabitLogRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AiCallWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val aiChallengeRepository: AiChallengeRepository,
    private val habitLogRepository: HabitLogRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Não gera novo desafio se já existe um ativo ou se já gerou hoje
        val activeChallenge = aiChallengeRepository.getActiveChallengeOnce()
        if (activeChallenge != null) return Result.success()
        if (aiChallengeRepository.hasChallengeGeneratedToday()) return Result.success()

        val nights = habitLogRepository.getLastNNights(30).getOrElse {
            Log.e(TAG, "Falha ao buscar histórico de noites", it)
            return Result.success()
        }

        val completeNights = nights.filter { it.moodScore != null && it.perceivedQuality != null }

        if (completeNights.size < 3) {
            val static = StaticChallengeProvider.getNext(System.currentTimeMillis())
            aiChallengeRepository.saveSuggestion(static, nights).onFailure {
                Log.e(TAG, "Falha ao salvar desafio estático (dados insuficientes)", it)
            }
            return Result.success()
        }

        // Tenta a API de IA
        aiChallengeRepository.generateChallenge(completeNights)
            .onSuccess { entity ->
                aiChallengeRepository.saveChallenge(entity).onFailure {
                    Log.e(TAG, "Falha ao persistir desafio da IA", it)
                }
                return Result.success()
            }
            .onFailure { Log.w(TAG, "API de IA falhou (${it.javaClass.simpleName}), tentando análise local") }

        // Fallback: LocalInsightEngine
        val localSuggestion = LocalInsightEngine.analyze(completeNights)
        if (localSuggestion != null) {
            aiChallengeRepository.saveSuggestion(localSuggestion, completeNights).onFailure {
                Log.e(TAG, "Falha ao salvar insight local", it)
            }
            return Result.success()
        }

        // Fallback final: desafio estático
        val static = StaticChallengeProvider.getNext(System.currentTimeMillis())
        aiChallengeRepository.saveSuggestion(static, completeNights).onFailure {
            Log.e(TAG, "Falha ao salvar desafio estático (fallback final)", it)
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "AiCallWorker"
    }
}
