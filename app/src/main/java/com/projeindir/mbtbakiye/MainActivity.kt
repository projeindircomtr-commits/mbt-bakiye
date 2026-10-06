package com.projeindir.mbtbakiye

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

class MainActivity : Activity() {
    private var txtToplam: TextView? = null
    private lateinit var txtBos: TextView
    private lateinit var txtDurum: TextView
    private lateinit var liste: LinearLayout
    private lateinit var kartIzin: View
    private var bekleyenIntent: Intent? = null
    private val handler = Handler(Looper.getMainLooper())
    private val periyodik = object : Runnable {
        override fun run() { yenile(); handler.postDelayed(this, 20000) }
    }
    private val pref by lazy { getSharedPreferences("mbt", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bildirim.kanalOlustur(this)
        bekleyenIntent = intent
        if (Api.sifre(this) == null) girisEkrani() else anaEkran()
    }

    // ---------- Giriş ----------
    private fun girisEkrani() {
        handler.removeCallbacks(periyodik)
        txtToplam = null
        setContentView(R.layout.activity_giris)
        val edt = findViewById<EditText>(R.id.edtSifre)
        val hata = findViewById<TextView>(R.id.txtGirisHata)
        val btn = findViewById<Button>(R.id.btnGiris)
        val gir = {
            val s = edt.text.toString().trim()
            if (s.isEmpty()) hata.text = "Şifreyi yaz."
            else {
                btn.isEnabled = false
                hata.text = "Kontrol ediliyor…"
                Thread {
                    try {
                        Api.durum(s)
                        Api.sifreKaydet(this, s)
                        runOnUiThread { anaEkran(); izinSureciniBaslat() }
                    } catch (e: SifreHatasi) {
                        runOnUiThread { hata.text = "Şifre yanlış, tekrar dene."; btn.isEnabled = true }
                    } catch (e: Exception) {
                        runOnUiThread { hata.text = "İnternete bağlanılamadı."; btn.isEnabled = true }
                    }
                }.start()
            }
        }
        btn.setOnClickListener { gir() }
        edt.setOnEditorActionListener { _, a, _ -> if (a == EditorInfo.IME_ACTION_DONE) { gir(); true } else false }
    }

    // ---------- Ana ekran ----------
    private fun anaEkran() {
        setContentView(R.layout.activity_main)
        txtToplam = findViewById(R.id.txtToplam)
        txtBos = findViewById(R.id.txtBos)
        txtDurum = findViewById(R.id.txtDurum)
        liste = findViewById(R.id.liste)
        kartIzin = findViewById(R.id.kartIzin)
        findViewById<Button>(R.id.btnYenile).setOnClickListener { yenile() }
        findViewById<Button>(R.id.btnIzin).setOnClickListener { bildirimIzniIste() }
        findViewById<View>(R.id.btnCikis).setOnClickListener {
            AlertDialog.Builder(this).setMessage("Çıkış yapılsın mı?")
                .setPositiveButton("Çıkış yap") { _, _ -> Api.sifreKaydet(this, null); girisEkrani() }
                .setNegativeButton("Vazgeç", null).show()
        }
        KontrolWorker.planla(this)
        izinKartiniGuncelle()
        handler.removeCallbacks(periyodik)
        handler.post(periyodik)
        bildirimdenGeldiyse(bekleyenIntent)
        bekleyenIntent = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (txtToplam != null) bildirimdenGeldiyse(intent) else bekleyenIntent = intent
    }

    override fun onResume() {
        super.onResume()
        if (txtToplam != null) { izinKartiniGuncelle(); handler.removeCallbacks(periyodik); handler.post(periyodik) }
    }
    override fun onPause() { super.onPause(); handler.removeCallbacks(periyodik) }

    // ---------- İzinler ----------
    private fun bildirimAcik() = NotificationManagerCompat.from(this).areNotificationsEnabled()

    private fun izinKartiniGuncelle() {
        if (::kartIzin.isInitialized) kartIzin.visibility = if (bildirimAcik()) View.GONE else View.VISIBLE
    }

    // Girişten hemen sonra: önce bildirim, sonra pil izni
    private fun izinSureciniBaslat() {
        if (!bildirimAcik()) bildirimIzniIste() else pilIzniIste()
    }

    private fun bildirimIzniIste() {
        val sayac = pref.getInt("izin_sayac", 0)
        if (Build.VERSION.SDK_INT >= 33 && sayac < 2 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            pref.edit().putInt("izin_sayac", sayac + 1).apply()
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else {
            // İzin penceresi artık çıkmıyorsa doğrudan bildirim ayarını aç
            AlertDialog.Builder(this)
                .setTitle("Bildirimleri aç")
                .setMessage("Açılan sayfada \"Bildirimlere izin ver\" düğmesini aç, sonra geri gel.")
                .setPositiveButton("Ayarı aç") { _, _ ->
                    startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                }
                .setNegativeButton("Sonra", null).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        izinKartiniGuncelle()
        if (bildirimAcik()) pilIzniIste()
    }

    private fun pilIzniIste() {
        if (pref.getBoolean("pil_soruldu", false)) return
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        pref.edit().putBoolean("pil_soruldu", true).apply()
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        } catch (_: Exception) { }
    }

    // ---------- Onay ----------
    private fun bildirimdenGeldiyse(i: Intent?) {
        val id = i?.getIntExtra(Bildirim.EXTRA_ID, -1) ?: -1
        if (id <= 0) return
        val tutar = i!!.getLongExtra(Bildirim.EXTRA_TUTAR, 0)
        i.removeExtra(Bildirim.EXTRA_ID)
        onaySor(Gelir(id, tutar))
    }

    private fun onaySor(g: Gelir) {
        AlertDialog.Builder(this)
            .setTitle("Yeni satın alma gerçekleşti")
            .setMessage("Gelen tutar: " + Api.tl(g.tutar) + "\n\nOnaylarsan genel toplam bakiyeye eklenecek.")
            .setPositiveButton("Onayla ve bakiyeye ekle") { _, _ -> onayla(g) }
            .setNegativeButton("Sonra", null)
            .show()
    }

    private fun sifreGecersiz() {
        Api.sifreKaydet(this, null)
        Toast.makeText(this, "Şifre değişmiş, tekrar giriş yap", Toast.LENGTH_LONG).show()
        girisEkrani()
    }

    private fun yenile() {
        val s = Api.sifre(this) ?: return
        txtDurum.text = "Güncelleniyor…"
        Thread {
            try {
                val d = Api.durum(s)
                runOnUiThread { if (txtToplam != null) { goster(d); txtDurum.text = "Güncel" } }
            } catch (e: SifreHatasi) {
                runOnUiThread { sifreGecersiz() }
            } catch (e: Exception) {
                runOnUiThread { if (txtToplam != null) txtDurum.text = "Bağlanılamadı: interneti kontrol et" }
            }
        }.start()
    }

    private fun onayla(g: Gelir) {
        val s = Api.sifre(this) ?: return
        Thread {
            try {
                val d = Api.onayla(s, g.id)
                runOnUiThread {
                    Bildirim.kaldir(this, g.id)
                    goster(d)
                    Toast.makeText(this, Api.tl(g.tutar) + " bakiyeye eklendi", Toast.LENGTH_LONG).show()
                }
            } catch (e: SifreHatasi) {
                runOnUiThread { sifreGecersiz() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Onaylanamadı: " + e.message, Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun goster(d: Durum) {
        txtToplam?.text = Api.tl(d.toplam)
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
