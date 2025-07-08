package com.example.myapplication.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import com.example.myapplication.R
import java.io.Serializable

data class ConfiguracionTablero(val filas: Int, val columnas: Int, val minas: Int) : Serializable

class GameConfigurationActivity : AppCompatActivity() {

  private lateinit var rgDifficulty: RadioGroup
  private lateinit var groupCustomConfig: Group
  private lateinit var etRows: EditText
  private lateinit var etColumns: EditText
  private lateinit var etMines: EditText
  private lateinit var btnStartGame: Button
  val server = MainActivity.Sockets.serverU
  val cliente = MainActivity.Sockets.clienteU

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_game_configuration)

    // Inicializar vistas
    rgDifficulty = findViewById(R.id.rgDifficulty)
    groupCustomConfig = findViewById(R.id.groupCustomConfig)
    etRows = findViewById(R.id.etRows)
    etColumns = findViewById(R.id.etColumns)
    etMines = findViewById(R.id.etMines)
    btnStartGame = findViewById(R.id.btnStartGame)

    setupListeners()
  }

  private fun setupListeners() {
    // Listener para mostrar/ocultar los campos personalizados
    rgDifficulty.setOnCheckedChangeListener { _, checkedId ->
      if (checkedId == R.id.rbCustom) {
        groupCustomConfig.visibility = View.VISIBLE
      } else {
        groupCustomConfig.visibility = View.GONE
      }
    }

    // Listener para el botón de iniciar partida
    btnStartGame.setOnClickListener { onStartGameClick() }
  }

  private fun onStartGameClick() {
    val selectedId = rgDifficulty.checkedRadioButtonId

    val config: ConfiguracionTablero? =
        when (selectedId) {
          R.id.rbEasy -> ConfiguracionTablero(4, 4, 4)
          R.id.rbMedium -> ConfiguracionTablero(6, 6, 10)
          R.id.rbHard -> ConfiguracionTablero(8, 8, 12)
          R.id.rbCustom -> getCustomConfiguration()
          else -> null
        }

    if (config != null) {
      // Configuración válida, podemos iniciar el juego
      Toast.makeText(
              this,
              "Iniciando partida: ${config.filas}x${config.columnas}, ${config.minas} minas",
              Toast.LENGTH_LONG)
          .show()
      cliente?.setContext(this)
      server?.iniciarTablero(config.filas, config.columnas, config.minas, "Victor")
      val mensaje = server?.getMensaje()
      Thread {
        if (mensaje != null) {
          cliente?.enviarMensaje(mensaje)
        }
      }.start()
    } else if (selectedId == R.id.rbCustom) {
      Toast.makeText(
              this,
              "Por favor, corrige los errores en la configuración personalizada.",
              Toast.LENGTH_SHORT)
          .show()
    } else {

      Toast.makeText(this, "Por favor, selecciona una dificultad.", Toast.LENGTH_SHORT).show()
    }
  }

  fun ConfiguracionTablero.toMessage(): String {
    return "GAME_CONFIG ${filas}_${columnas}_${minas}"
  }

  private fun getCustomConfiguration(): ConfiguracionTablero? {
    // Limpiar errores previos
    etRows.error = null
    etColumns.error = null
    etMines.error = null

    val rowsStr = etRows.text.toString()
    val colsStr = etColumns.text.toString()
    val minesStr = etMines.text.toString()

    val rows = rowsStr.toIntOrNull()
    if (rows == null || rows <= 0) {
      etRows.error = "Debe ser un número positivo"
      return null
    }

    val cols = colsStr.toIntOrNull()
    if (cols == null || cols <= 0) {
      etColumns.error = "Debe ser un número positivo"
      return null
    }

    val mines = minesStr.toIntOrNull()
    if (mines == null || mines <= 0) {
      etMines.error = "Debe ser un número positivo"
      return null
    }

    val totalCells = rows * cols
    if (mines >= totalCells) {
      etMines.error = "Las minas ($mines) deben ser menos que las casillas ($totalCells)"
      return null
    }

    return ConfiguracionTablero(rows, cols, mines)
  }
}
