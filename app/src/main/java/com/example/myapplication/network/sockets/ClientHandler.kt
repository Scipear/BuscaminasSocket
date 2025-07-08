package com.example.myapplication.network.sockets

import com.example.myapplication.Server
import com.example.myapplication.game.core.Tablero
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class ClientHandler(sock: Socket) : Runnable {
  companion object {
    var clientes: MutableList<ClientHandler> = mutableListOf()
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
        descifrarMensaje(mensaje)
        enviarMensaje(mensaje)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  private fun descifrarMensaje(msj: String) {
    var type: String = msj.split(" ")[0]

    when(type){
      "OPEN_TILE" -> {
        val coordenadas = msj.removePrefix("OPEN_TILE ").split("_")
        val row = coordenadas[0].toInt()
        val col = coordenadas[1].toInt()
        Server.tableroServer.abrirCasilla(row, col)
      }

      "FLAG_TILE" -> {
        val coordenadas = msj.removePrefix("FLAG_TILE ").split("_")
        val row = coordenadas[0].toInt()
        val col = coordenadas[1].toInt()
        Server.tableroServer.marcarCasilla(row, col)
      }

      "UNFLAG_TILE" -> {
        val coordenadas = msj.removePrefix("UNFLAG_TILE ").split("_")
        val row = coordenadas[0].toInt()
        val col = coordenadas[1].toInt()
        Server.tableroServer.desmarcarCasilla(row, col)
      }
    }
  }

  private fun enviarMensaje(msj: String) {
    println("Desde el handler el mensaje es: $msj")
    for (cliente in clientes) {
      try {
        cliente.dos?.println(msj)
      } catch (e: Exception) {}
    }
  }

  interface ClienteConectadoListener {
    fun onClientCountChanged(conexiones: Int)
  }
}
