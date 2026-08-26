package com.memoriabox.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class UpdateDownloadWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val info = inputData.getString(EXTRA_INFO)?.let(UpdateInfoJson::fromJson) ?: return Result.failure()
        UpdateManager.executeDownload(applicationContext, info)
        // 最多重试 3 次，避免校验失败/存储满等永久错误无限重下
        return when {
            UpdateManager.state.value is UpdateState.Error && runAttemptCount < 3 -> Result.retry()
            UpdateManager.state.value is UpdateState.Error -> Result.failure()
            else -> Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "memoriabox_update_download"
        private const val EXTRA_INFO = "update_info"

        fun enqueue(context: Context, info: UpdateInfo) {
            val request = androidx.work.OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
                .setInputData(workDataOf(EXTRA_INFO to UpdateInfoJson.toJson(info)))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
