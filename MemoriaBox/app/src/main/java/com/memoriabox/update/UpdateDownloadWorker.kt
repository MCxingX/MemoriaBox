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
        if (isStopped) return Result.failure()
        // 依据本次执行的显式结果决策：被取消的任务绝不重试，避免取消后又被自动拉起。
        val outcome = UpdateManager.executeDownload(applicationContext, info)
        if (isStopped) return Result.failure()
        return when (outcome) {
            UpdateManager.DownloadOutcome.CANCELLED -> Result.failure()
            UpdateManager.DownloadOutcome.SUCCESS -> Result.success()
            // 最多重试 3 次，避免校验失败/存储满等永久错误无限重下
            UpdateManager.DownloadOutcome.FAILURE ->
                if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "memoriabox_update_download"
        private const val EXTRA_INFO = "update_info"

        fun enqueue(context: Context, info: UpdateInfo) {
            val request = androidx.work.OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
                .setInputData(workDataOf(EXTRA_INFO to UpdateInfoJson.toJson(info)))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            // 使用 KEEP：已有同版本任务在跑时不再替换，避免新旧 Worker 并发写同一个 .part 文件。
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
