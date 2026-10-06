package com.projeindir.mbtbakiye

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class KontrolWorker(ctx: Context, p: WorkerParameters) : Worker(ctx, p) {
    override fun doWork(): Result = try {
        val d = Api.durum()
        Bildirim.yenileriBildir(applicationContext, d.bekleyen)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        fun planla(ctx: Context) {
            val kisit = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val is_ = PeriodicWorkRequestBuilder<KontrolWorker>(15, TimeUnit.MINUTES)
                .setConstraints(kisit).build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork("mbt_kontrol", ExistingPeriodicWorkPolicy.KEEP, is_)
        }
    }
}
