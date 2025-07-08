package com.example.myapplication.network.sockets

import com.example.myapplication.game.core.Tablero
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class ClientHandler(sock: Socket) : Runnable {
  companion object {
    var clientes: MutableList<ClientHandler> = mutableListOf()

    fun enviarMensaje(msj: String) {
      println("Desde el handler el mensaje es: $msj")
      for (cliente in clientes) {
        try {
          cliente.dos?.println(msj)
          println("✅ Mensaje enviado a cliente")
        } catch (e: Exception) {}
      }
    }
  }

  private var socket = sock
  private var clientSocket: Socket? = null
  private var dis: BufferedReader? = null
  private var dos: PrintWriter? = null

  override fun run() {
    clientSocket = socket
    dis = BufferedReader(InputStreamReader(socket!!.getInputStream()))
    dos = PrintWriter(socket!!.getOutputStream(), true)

    //clientes.add(this)

    while (socket!!.isConnected) {
      try {
        var mensaje: String = dis!!.readLine()
        enviarMensaje(mensaje)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  interface ClienteConectadoListener {
    fun onClientCountChanged(conexiones: Int)
  }
}
