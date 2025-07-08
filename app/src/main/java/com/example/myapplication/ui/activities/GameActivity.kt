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
  private var gameConfig: ConfiguracionTablero? = null
  private var posicionesMinas: List<Pair<Int, Int>> = emptyList()
  private val CELL_SIZE_DP = 40 // Tamaño de cada celda en DP

  private lateinit var matrixGridLayout: GridLayout
  private lateinit var actionSpinner: Spinner
  private lateinit var rowEditText: EditText
  private lateinit var columnEditText: EditText
  private lateinit var sendMoveButton: Button

  private lateinit var tableroLogico: Tablero

  private lateinit var cellViews: Array<Array<TextView>>

  private var juegoActivo = true // Para saber si el juego ha terminado
  private var toastActual: Toast? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge() // Para el diseño Edge-to-Edge
    setContentView(R.layout.activity_game) // Carga el XML

    recuperarConfiguracion()

    // --- PASO 2: VALIDAR QUE LA CONFIGURACIÓN SE RECIBIÓ CORRECTAMENTE ---
    if (gameConfig == null) {
      // Si no hay configuración, no podemos iniciar el juego.
      // Mostramos un error y cerramos la actividad.
      Toast.makeText(
              this, "Error: No se pudo cargar la configuración del juego.", Toast.LENGTH_LONG)
          .show()
      finish() // Cierra GameActivity y vuelve a la pantalla anterior
      return // Detiene la ejecución de onCreate para evitar crashes
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
    setupButtonListener()
  }

  private fun recuperarConfiguracion() {
    gameConfig =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          // Método moderno y seguro para Android 13 (API 33) y superior
          intent.getSerializableExtra("GAME_CONFIG", ConfiguracionTablero::class.java)
        } else {
          // Método antiguo (obsoleto) para versiones anteriores
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


    // Log para depuración
    if (gameConfig == null) {
      Log.e("GameActivity", "¡ERROR! No se recibió la configuración del juego en el Intent.")
    }
  }

  private fun inicializarVistas() {
    matrixGridLayout = findViewById(R.id.matrixGridLayout)
    actionSpinner = findViewById(R.id.actionSpinner)
    rowEditText = findViewById(R.id.rowEditText)
    columnEditText = findViewById(R.id.columnEditText)
    sendMoveButton = findViewById(R.id.sendMoveButton)
  }

  private fun iniciarNuevoJuego() {
    val config = gameConfig!!

    // 1. Crear la instancia del MODELO
    //tableroLogico.setFilas(config.filas)
    //tableroLogico.setColumnas(config.columnas)
    //tableroLogico.setMinas(config.minas)
    //if (posicionesMinas.isNotEmpty()) {
    //  tableroLogico.setPosicionesMinas(posicionesMinas) // Esta función la debes crear en tu clase Tablero
    //}
    tableroLogico = Tablero(config.filas, config.columnas, config.minas, "Victor", posicionesMinas)
    //tableroLogico = Tablero(config.filas, config.columnas, config.minas, "Victor")
    juegoActivo = true

    // 2. Crear la VISTA inicial
    setupGameGrid() // Crea los TextViews
    actualizarVistaTablero() // Dibuja el estado inicial del tablero (todo oculto)
  }

  private fun setupGameGrid() {
    val config = gameConfig!!
    matrixGridLayout.removeAllViews() // Limpiar el tablero si se reinicia el juego
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

  private fun setupButtonListener() {
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

      // --- Le dice al MODELO qué hacer ---
      val resultadoJugada: Int =
          when (actionSpinner.selectedItemPosition) {
            0 -> tableroLogico.abrirCasilla(row, col)
            1 -> tableroLogico.marcarCasilla(row, col)
            2 -> tableroLogico.desmarcarCasilla(row, col)
            else -> 0
          }
      // --- Pide a la VISTA que se actualice ---

      actualizarVistaTablero()
      if (resultadoJugada == -1) {
        juegoActivo = false
        revelarTableroCompleto()
      }
      // --- Comprueba el resultado del juego desde el MODELO ---
      verificarEstadoDelJuego()
    }
  }

  // --- VISTA: Función clave para sincronizar la UI con el estado del Modelo ---
  private fun actualizarVistaTablero() {
    val config = gameConfig!!
    for (r in 0 until config.filas) {
      for (c in 0 until config.columnas) {
        val casillaLogica = tableroLogico.getCasilla(r, c)!!
        val cellView = cellViews[r][c]

        cellView.text = "" // Limpiar texto anterior
        cellView.setBackgroundColor(Color.DKGRAY) // Color por defecto de casilla oculta

        if (casillaLogica.isMarcada()) {
          cellView.text = "🚩" // Emoji de bandera
          cellView.setBackgroundColor(Color.CYAN)
        } else if (casillaLogica.isAbierta()) {
          // La casilla está abierta, mostrar su contenido
          cellView.setBackgroundColor(Color.LTGRAY)
          if (casillaLogica.isMina()) {
            // cellView.text = "M"
            cellView.text = "💣" // Emoji de bomba
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
  }

  private fun verificarEstadoDelJuego() {
    val resultado = tableroLogico.verificarResultado()

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
      // Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
      mostrarToast(mensaje)
      mostrarToast(tableroLogico.getJugador().toString())
      // Opcional: revelar todo el tablero al terminar
      revelarTableroCompleto()
    }
  }

  private fun mostrarToast(mensaje: String, duracion: Int = Toast.LENGTH_SHORT) {
    // Paso 2: Cancelar el Toast anterior si existe
    toastActual?.cancel()

    // Paso 3: Crear el nuevo Toast, mostrarlo y guardarlo en la variable
    toastActual = Toast.makeText(this, mensaje, duracion)
    toastActual?.show()
  }

  private fun revelarTableroCompleto() {
    val config = gameConfig!!
    for (r in 0 until config.filas) {
      for (c in 0 until config.columnas) {
        tableroLogico.getCasilla(r, c)?.abrir()
      }
    }
    actualizarVistaTablero() // Vuelve a dibujar el tablero con todo revelado
  }
}
