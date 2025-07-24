package com.example.digi_diary.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BaseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            Log.d("BaseWorker", "Worker is running")
            Result.success()
        } catch (e: Exception) {
            Log.e("BaseWorker", "Error in worker", e)
            Result.failure()
        }
    }
}
