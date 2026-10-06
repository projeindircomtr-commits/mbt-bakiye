package com.projeindir.mbtbakiye

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var txtToplam: TextView
    private lateinit var txtBos: TextView
    private lateinit var txtDurum: TextView
    private lateinit var liste: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val periyodik = object : Runnable {
        override fun run() { yenile(); handler.postDelayed(this, 20000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        txtToplam = findViewById(R.id.txtToplam)
        txtBos = findViewById(R.id.txtBos)
        txtDurum = findViewById(R.id.txtDurum)
        liste = findViewById(R.id.liste)
        findViewById<Button>(R.id.btnYenile).setOnClickListener { yenile() }

        Bildirim.kanalOlustur(this)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        KontrolWorker.planla(this)
        bildirimdenGeldiyse(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        bildirimdenGeldiyse(intent)
    }

    override fun onResume() { super.onResume(); handler.post(periyodik) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(periyodik) }

    private fun bildirimdenGeldiyse(i: Intent?) {
        val id = i?.getIntExtra(Bildirim.EXTRA_ID, -1) ?: -1
        if (id <= 0) return
        val tutar = i!!.getLongExtra(Bildirim.EXTRA_TUTAR, 0)
        i.removeExtra(Bildirim.EXTRA_ID)
        onaySor(Gelir(id, tutar))
    }

    private fun onaySor(g: Gelir) {
        AlertDialog.Builder(this)
            .setTitle("Gelen para")
            .setMessage(Api.tl(g.tutar) + "\n\nOnaylarsan genel toplam bakiyeye eklenecek.")
            .setPositiveButton("Onayla ve ekle") { _, _ -> onayla(g) }
            .setNegativeButton("Sonra", null)
            .show()
    }

    private fun yenile() {
        txtDurum.text = "Güncelleniyor…"
        Thread {
            try {
                val d = Api.durum()
                runOnUiThread { goster(d); txtDurum.text = "Güncel" }
            } catch (e: Exception) {
                runOnUiThread { txtDurum.text = "Bağlanılamadı: internet bağlantını kontrol et" }
            }
        }.start()
    }

    private fun onayla(g: Gelir) {
        Thread {
            try {
                val d = Api.onayla(g.id)
                runOnUiThread {
                    Bildirim.kaldir(this, g.id)
                    goster(d)
                    Toast.makeText(this, Api.tl(g.tutar) + " bakiyeye eklendi", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Onaylanamadı: " + e.message, Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun goster(d: Durum) {
        txtToplam.text = Api.tl(d.toplam)
        liste.removeAllViews()
        txtBos.visibility = if (d.bekleyen.isEmpty()) View.VISIBLE else View.GONE
        val dp = resources.displayMetrics.density
        for (g in d.bekleyen.reversed()) {
            val satir = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.kart_satir)
                setPadding((16 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = (8 * dp).toInt() }
            }
            val tutar = TextView(this).apply {
                text = Api.tl(g.tutar)
                textSize = 18f
                setTextColor(getColor(R.color.lacivert))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            val btn = Button(this).apply {
                text = "Onayla"
                isAllCaps = false
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundResource(R.drawable.buton)
                setOnClickListener { onaySor(g) }
            }
            satir.addView(tutar)
            satir.addView(btn)
            liste.addView(satir)
        }
    }
}
