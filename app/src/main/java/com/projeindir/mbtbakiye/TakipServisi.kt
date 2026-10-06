package com.projeindir.mbtbakiye

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat

// Arka planda 30 saniyede bir yeni satın alma var mı diye bakar
class TakipServisi : Service() {
    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler
    private val kontrol = object : Runnable {
        override fun run() {
            val s = Api.sifre(applicationContext)
            if (s == null) { stopSelf(); return }
            try {
                val d = Api.durum(s)
                Bildirim.yenileriBildir(applicationContext, d.bekleyen)
            } catch (e: SifreHatasi) {
                stopSelf(); return
            } catch (_: Exception) { }
            handler.postDelayed(this, ARALIK_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(KANAL_SABIT, "Takip (sessiz)", NotificationManager.IMPORTANCE_MIN)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, KANAL_SABIT)
            .setSmallIcon(R.drawable.ic_bildirim)
            .setContentTitle("MBT Bakiye")
            .setContentText("Yeni satın almalar takip ediliyor")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(1, n)

        thread = HandlerThread("mbt-takip").also { it.start() }
        handler = Handler(thread.looper)
        handler.post(kontrol)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(kontrol)
        thread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val KANAL_SABIT = "takip"
        private const val ARALIK_MS = 30_000L

        fun baslat(ctx: Context) {
            if (Api.sifre(ctx) == null) return
            try {
                ctx.startForegroundService(Intent(ctx, TakipServisi::class.java))
            } catch (_: Exception) { }
        }

        fun durdur(ctx: Context) = ctx.stopService(Intent(ctx, TakipServisi::class.java))
    }
}
