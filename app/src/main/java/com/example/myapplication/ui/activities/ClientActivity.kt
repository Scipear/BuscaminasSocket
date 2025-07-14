package com.example.myapplication.ui.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.network.sockets.Cliente
import kotlin.concurrent.thread

class ClientActivity : AppCompatActivity() {
  private lateinit var etIpAddress: EditText
  private lateinit var btnConnect: Button
  private lateinit var client: Cliente
  private var direccionIP: String = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_client)

    // Inicializar las vistas usando findViewById
    etIpAddress = findViewById(R.id.etIpAddress)
    btnConnect = findViewById(R.id.btnConnect)

    // Configurar el listener para el botón de conectar
    btnConnect.setOnClickListener { handleConnectButtonClick() }
  }

  private fun handleConnectButtonClick() {
    direccionIP = etIpAddress.text.toString().trim()

    // Validar que los campos no estén vacíos
    if (direccionIP.isEmpty()) {
      Toast.makeText(this, "Por favor, introduce una dirección IP", Toast.LENGTH_SHORT).show()
      etIpAddress.error = "Campo requerido" // Muestra un error en el EditText
      return
    }

    val message = "Intentando conectar a IP: $direccionIP"
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    client = Cliente(direccionIP)
    MainActivity.Sockets.clienteU = client
    client.setContext(this)
    thread { client.run() }
  }
}
