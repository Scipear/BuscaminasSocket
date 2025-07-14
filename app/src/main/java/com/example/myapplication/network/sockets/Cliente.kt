package com.example.myapplication.network.sockets

import android.content.Intent
import android.content.Context
import com.example.myapplication.Server
import com.example.myapplication.ui.activities.ConfiguracionTablero
import com.example.myapplication.ui.activities.GameActivity
import com.example.myapplication.ui.activities.NameActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class Cliente(dir: String) : Runnable {
  private var direccionIP = dir
  private var socket: Socket? = null
  private var dis: BufferedReader? = null
  private var dos: PrintWriter? = null
  private var context: Context? = null

  override fun run() {
    try {
      println("Comprobando que el cliente inicio...")
      socket = Socket(direccionIP, 5200)
      dis = BufferedReader(InputStreamReader(socket!!.getInputStream()))
      dos = PrintWriter(socket!!.getOutputStream(), true)
      while (true) {
        recibirMensaje()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun descifrarMensaje(msj: String){
    var type: String = msj.split(" ")[0]

    when(type){
      "GAME_CONFIG" -> {
        val tableroRemoto = interpretarTableroRemoto(msj)
        if (tableroRemoto != null && context != null) {
          val intent = Intent(context, GameActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("GAME_CONFIG", tableroRemoto.config)
            putExtra("POS_MINAS", ArrayList(tableroRemoto.posiciones))
          }
          context!!.startActivity(intent)
        }
      }

      "OPEN_TILE" -> {
        val coordenadas = msj.removePrefix("OPEN_TILE ").split("_")
        val row = coordenadas[0].toInt()
        val col = coordenadas[1].toInt()
        val resultado = GameActivity.tableroLogico.abrirCasilla(row, col)
        GameActivity.instance?.runOnUiThread {
          if(resultado == -1){
            GameActivity.instance?.setJuegoActivo(false)
            GameActivity.instance?.revelarTableroCompleto()
          }else{
            if(GameActivity.instance?.getTurno() == true){
              for(i in 0 until resultado){
                NameActivity.jugador.aumentarPuntuacion()
              }
            }
            GameActivity.instance?.actualizarVistaTablero()
            GameActivity.instance?.enviarDatosJugador()
          }
          GameActivity.instance?.verificarEstadoDelJuego()
        }
      }

      "FLAG_TILE" -> {
        val coordenadas = msj.removePrefix("FLAG_TILE ").split("_")
        val row = coordenadas[0].toInt()
        val col = coordenadas[1].toInt()
        val resultado = GameActivity.tableroLogico.marcarCasilla(row, col)
        GameActivity.instance?.runOnUiThread {
          if(resultado == 1 && GameActivity.instance?.getTurno() == true){
            NameActivity.jugador.aumentarPuntuacion()
          }
          GameActivity.instance?.actualizarVistaTablero()
          GameActivity.instance?.enviarDatosJugador()
          GameActivity.instance?.verificarEstadoDelJuego()
        }
      }

      "UNFLAG_TILE" -> {
        val coordenadas = msj.removePrefix("UNFLAG_TILE ").split("_")
        val row = coordenadas[0].toInt()
        val col = coordenadas[1].toInt()
        val resultado = GameActivity.tableroLogico.desmarcarCasilla(row, col)
        GameActivity.instance?.runOnUiThread {
          if(resultado == -1 && GameActivity.instance?.getTurno() == true){
            NameActivity.jugador.reducirPuntuacion()
          }
          GameActivity.instance?.actualizarVistaTablero()
          GameActivity.instance?.enviarDatosJugador()
          GameActivity.instance?.verificarEstadoDelJuego()
        }
      }

      "CHANGE_TURN" -> {
        val nuevoTurno = !GameActivity.instance?.getTurno()!!
        GameActivity.instance?.setTurno(nuevoTurno)
        GameActivity.instance?.runOnUiThread {
          if(nuevoTurno){
            GameActivity.instance?.setupButtonListener()
          }
        }
      }

      "PLAYER_DATA" -> {
        val datos = msj.removePrefix("PLAYER_DATA ").split("_")
        val nombre = datos[0]
        val puntaje = datos[1].toInt()
        val nombrePropio = NameActivity.jugador.getNombre()
        val nombreJ2 = GameActivity.instance?.getNombreJ2()
        GameActivity.instance?.runOnUiThread {
          if(nombre != nombrePropio){
            println("Entro aqui con el nombre: $nombre")
            GameActivity.instance?.setNombreJ2(nombre)
            GameActivity.instance?.setPuntajeJ2(puntaje)
            GameActivity.instance?.actualizarVistaTablero()
          }
        }
      }
    }
  }

  private fun interpretarTableroRemoto(msj: String): TableroRemoto? {
    return try {
      val datos = msj.removePrefix("GAME_CONFIG ").split(";")
      val configPartes = datos[0].split("_")
      val filas = configPartes[0].toInt()
      val columnas = configPartes[1].toInt()
      val minas = configPartes[2].toInt()

      val posiciones = datos[1].split(",").map {
        val (fila, col) = it.split("-")
        Pair(fila.toInt(), col.toInt())
      }

      val config = ConfiguracionTablero(filas, columnas, minas)
      TableroRemoto(config, posiciones)
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun enviarMensaje(msj: String) {
    try {
      dos?.println(msj)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun recibirMensaje() {
    try {
      var mensajeRecibido: String
      while (socket?.isConnected == true) {
        mensajeRecibido = dis?.readLine().toString()
        descifrarMensaje(mensajeRecibido)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun setContext(contexto: Context){
    context = contexto
  }

  data class TableroRemoto(
    val config: ConfiguracionTablero,
    val posiciones: List<Pair<Int, Int>>
  )
}
