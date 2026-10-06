package com.projeindir.mbtbakiye

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

data class Gelir(val id: Int, val tutar: Long)
data class Durum(val toplam: Long, val bekleyen: List<Gelir>)
class SifreHatasi : Exception("Şifre yanlış")

object Api {
    private const val ADRES = "https://projeindir.com.tr/mbt/api.php"

    fun sifre(ctx: Context): String? =
        ctx.getSharedPreferences("mbt", Context.MODE_PRIVATE).getString("sifre", null)

    fun sifreKaydet(ctx: Context, s: String?) =
        ctx.getSharedPreferences("mbt", Context.MODE_PRIVATE).edit().putString("sifre", s).apply()

    private fun istek(sifre: String, islem: String, ek: Map<String, String> = emptyMap()): JSONObject {
        val c = URL("$ADRES?islem=$islem").openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 15000
        c.setRequestProperty("User-Agent", "MBTBakiye/1.1")
        c.requestMethod = "POST"
        c.doOutput = true
        val govde = (ek + ("sifre" to sifre)).entries.joinToString("&") {
            it.key + "=" + URLEncoder.encode(it.value, "UTF-8")
        }
        c.outputStream.use { it.write(govde.toByteArray()) }
        if (c.responseCode == 403) { c.disconnect(); throw SifreHatasi() }
        val metin = c.inputStream.bufferedReader().use { it.readText() }
        c.disconnect()
        val j = JSONObject(metin)
        if (!j.optBoolean("ok")) throw Exception(j.optString("hata", "Sunucu hatası"))
        return j
    }

    private fun coz(j: JSONObject): Durum {
        val arr = j.getJSONArray("bekleyen")
        val liste = (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Gelir(o.getInt("id"), o.getLong("tutar"))
        }
        return Durum(j.getLong("toplam"), liste)
    }

    fun durum(sifre: String): Durum = coz(istek(sifre, "durum"))
    fun onayla(sifre: String, id: Int): Durum = coz(istek(sifre, "onayla", mapOf("id" to id.toString())))

    fun tl(kurus: Long): String {
        val f = NumberFormat.getNumberInstance(Locale("tr", "TR"))
        f.minimumFractionDigits = 2
        f.maximumFractionDigits = 2
        return f.format(kurus / 100.0) + " TL"
    }
}
