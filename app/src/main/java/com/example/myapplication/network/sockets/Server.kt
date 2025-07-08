package com.example.myapplication


import com.example.myapplication.game.core.Tablero
import com.example.myapplication.network.sockets.ClientHandler
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class Server(private val listener: ClientHandler.ClienteConectadoListener) : Runnable {
  companion object {
    lateinit var tableroServer: Tablero
  }
  private val port: Int = 5200
  private var serverSocket: ServerSocket? = null
  private lateinit var msj: String

  override fun run() {
    serverSocket = ServerSocket(port)
    println("Esperando por clientes...")
    try {
      while (true) {
        val socket: Socket = serverSocket!!.accept()
        println("Cliente conectado: ${socket.inetAddress.hostAddress}")
        val cliente = ClientHandler(socket)
        ClientHandler.clientes.add(cliente)

        listener.onClientCountChanged(ClientHandler.clientes.size)

        thread { cliente.run() }
          println("Cantidad de clientes conectados: ${ClientHandler.clientes.size}")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun cerrarServidor() {
    try {
      serverSocket?.close()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun getPort(): Int {
    return port
  }

  fun iniciarTablero(filas:Int, columnas:Int, minas:Int, nombre:String){
    tableroServer = Tablero(filas, columnas, minas, nombre)
    val posiciones = tableroServer.getPosicionesMinas()
    msj = buildString {
      append("GAME_CONFIG ${filas}_${columnas}_${minas};")
      append(posiciones.joinToString(",") { "${it.first}-${it.second}" })
    }
  }

  fun getMensaje(): String{
    return msj
  }
}
