package com.projeindir.mbtbakiye

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

data class Gelir(val id: Int, val tutar: Long)
data class Durum(val toplam: Long, val bekleyen: List<Gelir>)

object Api {
    private const val ADRES = "https://projeindir.com.tr/mbt/api.php"
    private const val ANAHTAR = "18d448a5719ff107b855d8437da96c94" // panel config.php ile aynı

    private fun istek(islem: String, post: Map<String, String> = emptyMap()): JSONObject {
        val url = URL("$ADRES?islem=$islem&key=$ANAHTAR")
        val c = url.openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 15000
        c.setRequestProperty("User-Agent", "MBTBakiye/1.0")
        if (post.isNotEmpty()) {
            c.requestMethod = "POST"
            c.doOutput = true
            val govde = (post + ("key" to ANAHTAR)).entries.joinToString("&") {
                it.key + "=" + URLEncoder.encode(it.value, "UTF-8")
            }
            c.outputStream.use { it.write(govde.toByteArray()) }
        }
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

    fun durum(): Durum = coz(istek("durum"))
    fun onayla(id: Int): Durum = coz(istek("onayla", mapOf("id" to id.toString())))

    fun tl(kurus: Long): String {
        val f = NumberFormat.getNumberInstance(Locale("tr", "TR"))
        f.minimumFractionDigits = 2
        f.maximumFractionDigits = 2
        return f.format(kurus / 100.0) + " TL"
    }
}
