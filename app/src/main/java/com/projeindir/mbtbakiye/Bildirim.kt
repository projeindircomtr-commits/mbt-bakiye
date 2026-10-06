package com.projeindir.mbtbakiye

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Bildirim {
    const val KANAL = "gelirler"
    const val EXTRA_ID = "gelir_id"
    const val EXTRA_TUTAR = "gelir_tutar"

    fun kanalOlustur(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val k = NotificationChannel(KANAL, "Gelen ödemeler", NotificationManager.IMPORTANCE_HIGH)
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(k)
        }
    }

    // Yeni bekleyen gelirler için bildirim at (her gelir için 1 kez)
    fun yenileriBildir(ctx: Context, bekleyen: List<Gelir>) {
        val pref = ctx.getSharedPreferences("mbt", Context.MODE_PRIVATE)
        val bildirilen = pref.getStringSet("bildirilen", emptySet())!!.toMutableSet()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        kanalOlustur(ctx)
        for (g in bekleyen) {
            if (g.id.toString() in bildirilen) continue
            val intent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_ID, g.id)
                putExtra(EXTRA_TUTAR, g.tutar)
            }
            val pi = PendingIntent.getActivity(
                ctx, g.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(ctx, KANAL)
                .setSmallIcon(R.drawable.ic_bildirim)
                .setContentTitle("Yeni gelir geldi")
                .setContentText("Görmek ve onaylamak için dokun")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            NotificationManagerCompat.from(ctx).notify(g.id, n)
            bildirilen.add(g.id.toString())
        }
        pref.edit().putStringSet("bildirilen", bildirilen).apply()
    }

    fun kaldir(ctx: Context, id: Int) = NotificationManagerCompat.from(ctx).cancel(id)
}
