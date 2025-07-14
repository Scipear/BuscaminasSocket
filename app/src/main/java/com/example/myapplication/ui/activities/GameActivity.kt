package com.example.myapplication.ui.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.gridlayout.widget.GridLayout
import com.example.myapplication.R
import com.example.myapplication.game.core.Tablero

class GameActivity : AppCompatActivity() {
  companion object {
    lateinit var tableroLogico: Tablero
    var instance: GameActivity? = null
  }
  private var gameConfig: ConfiguracionTablero? = null
  private var posicionesMinas: List<Pair<Int, Int>> = emptyList()
  private val CELL_SIZE_DP = 40

  private lateinit var matrixGridLayout: GridLayout
  private lateinit var actionSpinner: Spinner
  private lateinit var rowEditText: EditText
  private lateinit var columnEditText: EditText
  private lateinit var sendMoveButton: Button
  private lateinit var jugador1: TextView
  private lateinit var puntaje1: TextView
  private lateinit var jugador2: TextView
  private lateinit var puntaje2: TextView

    private lateinit var cellViews: Array<Array<TextView>>

  private var juegoActivo = true
  private var turno: Boolean = false
  private var toastActual: Toast? = null
  private val cliente = MainActivity.Sockets.clienteU
  private val server = MainActivity.Sockets.serverU
  private val jugador = NameActivity.jugador
  private var j2Nombre: String = ""
  private var j2Puntaje: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_game)
    instance = this

    recuperarConfiguracion()

    if (gameConfig == null) {
      Toast.makeText(
              this, "Error: No se pudo cargar la configuración del juego.", Toast.LENGTH_LONG)
          .show()
      finish()
      return
    }

    if(server != null){
      turno = true
    }

    val mainContainer = findViewById<View>(R.id.main_container)
    ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }

    inicializarVistas()
    iniciarNuevoJuego()
    setupSpinner()
    if(turno){
      setupButtonListener()
    }
  }

  private fun recuperarConfiguracion() {
    gameConfig =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getSerializableExtra("GAME_CONFIG", ConfiguracionTablero::class.java)
        } else {
          @Suppress("DEPRECATION")
          intent.getSerializableExtra("GAME_CONFIG") as? ConfiguracionTablero
        }

    posicionesMinas =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getSerializableExtra("POS_MINAS", ArrayList::class.java) as? ArrayList<Pair<Int, Int>>
      } else {
        @Suppress("DEPRECATION")
        intent.getSerializableExtra("POS_MINAS") as? ArrayList<Pair<Int, Int>>
      } ?: emptyList()
  }

  private fun inicializarVistas() {
    matrixGridLayout = findViewById(R.id.matrixGridLayout)
    actionSpinner = findViewById(R.id.actionSpinner)
    rowEditText = findViewById(R.id.rowEditText)
    columnEditText = findViewById(R.id.columnEditText)
    sendMoveButton = findViewById(R.id.sendMoveButton)
    jugador1 = findViewById(R.id.jugador1)
    jugador1.text = "${jugador.getNombre()}:"
    puntaje1 = findViewById(R.id.puntaje1)
    jugador2 = findViewById(R.id.jugador2)
    puntaje2 = findViewById(R.id.puntaje2)
    if(!turno){
      sendMoveButton.isEnabled = false
    }
  }

  private fun iniciarNuevoJuego() {
    val config = gameConfig!!
    tableroLogico = Tablero(config.filas, config.columnas, config.minas, jugador.getNombre(), posicionesMinas)
    juegoActivo = true
    enviarDatosJugador()

    //Crear la VISTA inicial
    setupGameGrid()
    actualizarVistaTablero()
  }

  private fun setupGameGrid() {
    val config = gameConfig!!
    matrixGridLayout.removeAllViews() //Limpiar el tablero si se reinicia el juego
    matrixGridLayout.rowCount = config.filas
    matrixGridLayout.columnCount = config.columnas
    cellViews = Array(config.filas) { Array(config.columnas) { TextView(this) } }
    val cellSizePx = (CELL_SIZE_DP * resources.displayMetrics.density).toInt()

    for (row in 0 until config.filas) {
      for (col in 0 until config.columnas) {
        val cellView =
            TextView(this).apply {
              layoutParams =
                  GridLayout.LayoutParams().apply {
                    width = cellSizePx
                    height = cellSizePx
                    rowSpec = GridLayout.spec(row, 1f)
                    columnSpec = GridLayout.spec(col, 1f)
                    setMargins(2, 2, 2, 2)
                  }
              gravity = Gravity.CENTER
              textSize = 18f
              setOnClickListener {
                rowEditText.setText(row.toString())
                columnEditText.setText(col.toString())
              }
            }
        cellViews[row][col] = cellView
        matrixGridLayout.addView(cellView)
      }
    }
  }

  private fun setupSpinner() {
    ArrayAdapter.createFromResource(
            this, R.array.move_actions, android.R.layout.simple_spinner_item)
        .also { adapter ->
          adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
          actionSpinner.adapter = adapter
        }
  }

  fun setupButtonListener() {
    val config = gameConfig!!
    sendMoveButton.setOnClickListener {
      if (!juegoActivo) {
        Toast.makeText(this, "El juego ha terminado. Inicia uno nuevo.", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }

      val row = rowEditText.text.toString().toIntOrNull()
      val col = columnEditText.text.toString().toIntOrNull()

      if (row == null ||
          col == null ||
          row !in 0 until config.filas ||
          col !in 0 until config.columnas) {
        Toast.makeText(this, "Coordenadas inválidas.", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }

      val msj: String
      var resultadoJugada: Int = -1
      when(actionSpinner.selectedItemPosition) {
        0 -> {
          msj = "OPEN_TILE ${row}_${col}"
          Thread { cliente?.enviarMensaje(msj) }.start()
          resultadoJugada = 1
        }
        1 -> {
          msj = "FLAG_TILE ${row}_${col}"
          Thread { cliente?.enviarMensaje(msj) }.start()
          resultadoJugada = 1
        }
        2 -> {
          msj = "UNFLAG_TILE ${row}_${col}"
          Thread { cliente?.enviarMensaje(msj) }.start()
          resultadoJugada = 1
        }
      }

      Thread {
        Thread.sleep(1000)
        cliente?.enviarMensaje("CHANGE_TURN")
      }.start()
    }
  }

  fun actualizarVistaTablero() {
    val config = gameConfig!!
    for (r in 0 until config.filas) {
      for (c in 0 until config.columnas) {
        val casillaLogica = tableroLogico.getCasilla(r, c)!!
        val cellView = cellViews[r][c]

        cellView.text = "" // Limpiar texto anterior
        cellView.setBackgroundColor(Color.DKGRAY) // Color por defecto de casilla oculta

        if (casillaLogica.isMarcada()) {
          cellView.text = "F"
          cellView.setBackgroundColor(Color.GREEN)
        } else if (casillaLogica.isAbierta()) {
          // La casilla está abierta, mostrar su contenido
          cellView.setBackgroundColor(Color.LTGRAY)
          if (casillaLogica.isMina()) {
            cellView.text = "B"
            cellView.setBackgroundColor(Color.RED)
          } else if (casillaLogica.getMinasAlrededor() > 0) {
            cellView.text = casillaLogica.getMinasAlrededor().toString()
          } else {
            // Casilla vacía y abierta, no mostrar nada
            cellView.text = ""
          }
        }
      }
    }
    actualizarPuntaje()
  }

  fun verificarEstadoDelJuego() {
    val resultado = tableroLogico.verificarResultado()
    println("Resultado del juego es: ${resultado}")

    if (resultado != 3) { // 3 es "Partida en progreso"
      juegoActivo = false
      sendMoveButton.isEnabled = false // Desactivar el botón

      val mensaje =
          when (resultado) {
            0 -> "¡Boom! Has perdido."
            1,
            2 -> "¡Felicidades! ¡Has ganado!"
            else -> ""
          }
      mostrarToast(mensaje)
      //mostrarToast(tableroLogico.getJugador().toString())
      revelarTableroCompleto()
    }
  }

  private fun mostrarToast(mensaje: String, duracion: Int = Toast.LENGTH_SHORT) {
    toastActual?.cancel()
    toastActual = Toast.makeText(this, mensaje, duracion)
    toastActual?.show()
  }

  fun revelarTableroCompleto() {
    val config = gameConfig!!
    for (r in 0 until config.filas) {
      for (c in 0 until config.columnas) {
        tableroLogico.getCasilla(r, c)?.abrir()
      }
    }
    actualizarVistaTablero()
  }

  private fun actualizarPuntaje() {
    puntaje1.text = "${NameActivity.jugador.getPuntaje()}"
    puntaje2.text = "${j2Puntaje}"
  }

  fun enviarDatosJugador(){
    val msj = "PLAYER_DATA ${jugador.getNombre()}_${jugador.getPuntaje()}"
    Thread { cliente?.enviarMensaje(msj) }.start()
  }

  fun setTurno(turn: Boolean){
    this.turno = turn
    runOnUiThread {
      sendMoveButton.isEnabled = turn
    }
  }

  fun getTurno(): Boolean{
    return turno
  }

  fun setJuegoActivo(game: Boolean){
    this.juegoActivo = game
  }

  fun setNombreJ2(name: String){
    this.j2Nombre = name
    jugador2.text = "${j2Nombre}:"
  }

  fun getNombreJ2(): String{
    return j2Nombre
  }

  fun setPuntajeJ2(points: Int){
    this.j2Puntaje = points
  }
}