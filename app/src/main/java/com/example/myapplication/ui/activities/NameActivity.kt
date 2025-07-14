package com.example.myapplication.ui.activities

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.R
import com.example.myapplication.game.core.Jugador

class NameActivity : AppCompatActivity() {
    companion object{
        lateinit var jugador: Jugador
    }
    lateinit var ipName: EditText
    lateinit var btnAceptar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_name)

        ipName = findViewById(R.id.nameText)
        btnAceptar = findViewById(R.id.aceptarButton)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnAceptar.setOnClickListener {
            val nombre = ipName.text.toString().trim()

            if(nombre.isEmpty()){
                Toast.makeText(this, "Por favor ingresa tu nombre", Toast.LENGTH_SHORT).show()
            } else {
                jugador = Jugador(nombre)

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("NOMBRE_JUGADOR", nombre)
                startActivity(intent)
            }
        }
    }
}