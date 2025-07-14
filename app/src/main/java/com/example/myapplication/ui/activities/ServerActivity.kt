package com.example.myapplication.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.Server
import com.example.myapplication.network.sockets.ClientHandler
import com.example.myapplication.network.sockets.Cliente
import com.example.myapplication.network.sockets.Direccion
import kotlin.concurrent.thread

class ServerActivity : AppCompatActivity(), ClientHandler.ClienteConectadoListener {
  private val DELAY_MILLISECONDS: Long = 5000 // 5 segundos
  private var direccion = Direccion()
  private var server = Server(this)
  private lateinit var client: Cliente

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_server)

    // Encontrar el TextView por su ID
    val tvIpAddress: TextView = findViewById(R.id.tvIpAddress)

    // Obtener la dirección IP y mostrarla
    val ipAddress = direccion.getIPAddress()
    if (ipAddress != null) {
      tvIpAddress.text = ipAddress
    } else {
      tvIpAddress.text = "No se pudo obtener la IP. ¿Estás conectado a una red WiFi?"
    }
    client = Cliente(ipAddress)
    MainActivity.Sockets.serverU = server
    MainActivity.Sockets.clienteU = client
    thread { server.run() }
    thread { client.run() }
  }

  override fun onClientCountChanged(conexiones: Int) {
    if (conexiones == 2) {
      navigateToGameConfiguration()
    }
  }

  private fun navigateToGameConfiguration() {
    val intent = Intent(this, GameConfigurationActivity::class.java)
    startActivity(intent)
    finish()
  }
}
