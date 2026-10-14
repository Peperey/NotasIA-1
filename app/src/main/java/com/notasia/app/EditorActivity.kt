package com.notasia.app

import android.app.Activity
import android.graphics.Color
import android.os.*
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class EditorActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ⚠️ CAMBIA AQUÍ: tu API key y el proveedor
    private val API_KEY = BuildConfig.NOTAS_API_KEY
 
    private val API_URL = "https://api.groq.com/openai/v1/chat/completions" // gratis
    private val MODEL = "llama-3.1-8b-instant"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        val title = EditText(this).apply { hint = "Título" ; textSize = 20f }
        val body = EditText(this).apply { hint = "Escribe tu nota..."; minLines = 8 ; textSize = 16f }
        val fix = Button(this).apply {
            text = "🤖 Corregir con IA"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
        }
        val save = Button(this).apply {
            text = "💾 Guardar"
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
        }
        layout.addView(title)
        layout.addView(body, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout.addView(fix)
        layout.addView(save)
        setContentView(layout)

        val pos = intent.getIntExtra("pos", -1)
        val f = File(filesDir, "notes.json")
        val notes = if (f.exists()) JSONArray(f.readText()) else JSONArray()
        if (pos >= 0 && pos < notes.length()) {
            val nota = notes.getJSONObject(pos)
            title.setText(nota.optString("title"))
            body.setText(nota.optString("body"))
        }

        fix.setOnClickListener {
            val original = body.text.toString().trim()
            if (original.isEmpty()) {
                Toast.makeText(this, "Escribe algo primero 🙂", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            fix.isEnabled = false; fix.text = "⏳ Corrigiendo..."
            Thread {
                var resultado = ""
                var error = ""
                try {
                    val json = JSONObject().apply {
                        put("model", MODEL)
                        put("messages", JSONArray()
                            .put(JSONObject().put("role", "system").put("content",
                                "Eres un corrector. Corrige gramática y ortografía del texto en español. Devuelve ÚNICAMENTE el texto corregido, sin explicaciones ni comillas."))
                            .put(JSONObject().put("role", "user").put("content", original)))
                    }
                    val req = Request.Builder()
                        .url(API_URL)
                        .header("Authorization", "Bearer $API_KEY")
                        .post(RequestBody.create("application/json".toMediaType(), json.toString()))
                        .build()
                    client.newCall(req).execute().use { r ->
                        val resp = JSONObject(r.body!!.string())
                        if (r.isSuccessful) {
                            resultado = resp.getJSONArray("choices")
                                .getJSONObject(0).getJSONObject("message")
                                .getString("content").trim()
                        } else {
                            error = "API r.code:{r.code}:r.code:{resp.optString("error", "error")}"
                        }
                    }
                } catch (e: Exception) { error = e.message ?: "Error desconocido" }
                runOnUiThread {
                    fix.isEnabled = true; fix.text = "🤖 Corregir con IA"
                    if (error.isNotEmpty()) Toast.makeText(this, "❌ $error", Toast.LENGTH_LONG).show()
                    else if (resultado.isNotEmpty()) {
                        body.setText(resultado)
                        Toast.makeText(this, "✅ Texto corregido", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }

        save.setOnClickListener {
            val obj = JSONObject().apply {
                put("title", title.text.toString().ifEmpty { "Sin título" })
                put("body", body.text.toString())
                put("updated", System.currentTimeMillis())
            }
            if (pos >= 0 && pos < notes.length()) notes.put(pos, obj) else notes.put(obj)
            f.writeText(notes.toString(2))
            setResult(Activity.RESULT_OK)
            Toast.makeText(this, "💾 Guardada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
