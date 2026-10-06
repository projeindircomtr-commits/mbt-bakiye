package com.projeindir.mbtbakiye

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Telefon yeniden açılınca veya uygulama güncellenince takibi tekrar başlat
class AcilisAlici : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        TakipServisi.baslat(ctx)
        KontrolWorker.planla(ctx)
    }
}
