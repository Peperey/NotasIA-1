package com.notasia.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import org.json.JSONArray
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    private lateinit var notes: JSONArray
    private val archivo get() = File(filesDir, "notes.json")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val recycler = RecyclerView(this).apply {
layoutManager = LinearLayoutManager(this@MainActivity)
        }
        val fab = Button(this).apply {
            text = "➕ Nueva nota"
            setBackgroundColor(Color.parseColor("#F5B705"))
            setTextColor(Color.WHITE)
        }
        fab.setOnClickListener {
            startActivity(Intent(this, EditorActivity::class.java).putExtra("pos", -1))
        }
        root.addView(recycler, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(fab, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(32, 8, 32, 32)
        })
        setContentView(root)

        adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, t: Int) =
                object : RecyclerView.ViewHolder(TextView(p.context)) {}
            override fun getItemCount() = notes.length()
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                val tv = h.itemView as TextView
                val nota = notes.getJSONObject(pos)
                tv.text = "📝 " + nota.optString("title", "Sin título") +
                        "\n" + nota.optString("body", "").take(60)
                tv.textSize = 16f
                tv.setPadding(48, 40, 48, 40)
                tv.setOnClickListener { v ->
                    v.context.startActivity(Intent(v.context, EditorActivity::class.java)
                        .putExtra("pos", pos))
                }
                // Mantener presionado para borrar
                tv.setOnLongClickListener {
                    AlertDialog.Builder(it.context)
                        .setTitle("¿Borrar nota?")
                        .setMessage("Esta acción no se puede deshacer.")
                        .setPositiveButton("Borrar") { _, _ ->
                            val nueva = JSONArray()
                            for (i in 0 until notes.length())
                                if (i != pos) nueva.put(notes.getJSONObject(i))
                            archivo.writeText(nueva.toString())
                            loadNotes()
                        }
                        .setNegativeButton("Cancelar", null).show()
                    true
                }
            }
        }
        recycler.adapter = adapter
    }

    override fun onResume() { super.onResume(); loadNotes() }

    private fun loadNotes() {
        notes = if (archivo.exists()) JSONArray(archivo.readText()) else JSONArray()
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
    }
}
