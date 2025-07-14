package com.example.myapplication.game.core

import kotlin.random.Random

class Tablero(
    private var filas: Int,
    private var columnas: Int,
    private var numeroMinas: Int,
    private var nombre: String
) {
    private var jugadas: Int = 0
    private val tablero: Array<Array<Casilla>>
    private var jugador: Jugador = Jugador(nombre)

    init {
        if (numeroMinas > filas * columnas) {
            throw IllegalArgumentException("El número de minas no puede exceder el total de casillas.")
        }
        if (numeroMinas < 0 || filas <= 0 || columnas <= 0) {
            throw IllegalArgumentException("Dimensiones y número de minas deben ser positivos.")
        }

        tablero = Array(filas) { r ->
            Array(columnas) { c ->
                Casilla(r, c)
            }
        }
        asignarMinas()
        actualizarMinasAlrededor()
    }

    constructor(
        filas: Int,
        columnas: Int,
        numeroMinas: Int,
        nombre: String,
        posicionesPredefinidas: List<Pair<Int, Int>>
    ) : this(filas, columnas, 0, nombre) {
        setPosicionesMinas(posicionesPredefinidas, numeroMinas)
    }

    private fun asignarMinas() {
        var minasGeneradas = 0
        while (minasGeneradas < numeroMinas) {
            val i = Random.nextInt(filas)
            val j = Random.nextInt(columnas)

            if (!tablero[i][j].isMina()) {
                tablero[i][j].setMina(true)
                minasGeneradas++
            }
        }
    }

    private fun actualizarMinasAlrededor() {
        for (r in 0 until filas) {
            for (c in 0 until columnas) {
                if (tablero[r][c].isMina()) {
                    aumentarContadorAdyacentes(r, c)
                }
            }
        }
    }

    private fun aumentarContadorAdyacentes(filaMina: Int, columnaMina: Int) {
        for (i in (filaMina - 1)..(filaMina + 1)) {
            for (j in (columnaMina - 1)..(columnaMina + 1)) {
                if (estaDentroDeLimites(i, j)
                    && (i != filaMina || j != columnaMina) && !tablero[i][j].isMina()) {
                    tablero[i][j].incrementarMinasAlrededor()
                }
            }
        }
    }

    private fun estaDentroDeLimites(fila: Int, columna: Int): Boolean {
        return fila >= 0 && fila < filas && columna >= 0 && columna < columnas
    }

    fun abrirCasilla(fila: Int, columna: Int): Int {
        var puntos: Int = 1
        if (!estaDentroDeLimites(fila, columna)) {
            return 0
        }
        val casilla = tablero[fila][columna]
        jugadas++
        casilla.abrir()

        if (casilla.isMina()) {
            return -1
        }

        if (casilla.getMinasAlrededor() == 0) {
            puntos += abrirAlrededorRecursivo(casilla)
        }

        return puntos
    }

    private fun abrirAlrededorRecursivo(casillaOriginal: Casilla): Int{
        var puntos: Int = 0
        if (casillaOriginal.getMinasAlrededor() == 0) {
            val casillasAdyacentes = obtenerCasillasAdyacentesParaAbrir(casillaOriginal)
            for (adyacente in casillasAdyacentes) {
                if (!adyacente.isAbierta() && !adyacente.isMarcada()) {
                    adyacente.abrir()
                    puntos++
                    if (adyacente.getMinasAlrededor()== 0) {
                        abrirAlrededorRecursivo(adyacente)
                    }
                }
            }
        }
        return puntos
    }
    private fun obtenerCasillasAdyacentesParaAbrir(casillaBase: Casilla): List<Casilla> {
        val alrededor = mutableListOf<Casilla>()
        val filaBase = casillaBase.getX()
        val columnaBase = casillaBase.getY()

        for (i in (filaBase - 1)..(filaBase + 1)) {
            for (j in (columnaBase - 1)..(columnaBase + 1)) {
                if (estaDentroDeLimites(i, j) && (i != filaBase || j != columnaBase)) {
                    val casillaAdyacente = tablero[i][j]
                    if (!casillaAdyacente.isMina() ) {
                        alrededor.add(casillaAdyacente)
                    }
                }
            }
        }
        return alrededor
    }

    fun marcarCasilla(fila: Int, columna: Int): Int {
        val casilla = tablero[fila][columna]
        if (!casilla.isAbierta()) {
            casilla.marcar()
        }

        if(casilla.isMina()){
            return 1
        }

        return -1
    }

    fun desmarcarCasilla(fila: Int, columna: Int): Int {
        val casilla = tablero[fila][columna]
        if (casilla.isMarcada()) {
            casilla.desmarcar()
        }

        if(casilla.isMina()){
            //jugador.reducirPuntuacion()
            return -1
        }

        return 1
    }

    fun getCasilla(fila: Int, columna: Int): Casilla? {
        if (fila in 0 until filas && columna in 0 until columnas) {
            return tablero[fila][columna]
        }
        return null
    }

    fun getPosicionesMinas(): List<Pair<Int, Int>> {
        val posiciones = mutableListOf<Pair<Int, Int>>()
        for (fila in 0 until filas) {
            for (col in 0 until columnas) {
                if (tablero[fila][col].isMina()) {
                    posiciones.add(Pair(fila, col))
                }
            }
        }
        return posiciones
    }

    fun setPosicionesMinas(posiciones: List<Pair<Int, Int>>, numMinas: Int) {
        this.numeroMinas = numMinas
        // Limpia minas anteriores si las hay
        for (fila in 0 until filas) {
            for (col in 0 until columnas) {
                tablero[fila][col].setMina(false)
            }
        }

        // Asigna minas nuevas
        for ((fila, col) in posiciones) {
            tablero[fila][col].setMina(true)
        }

        // Recalcular minas alrededor
        actualizarMinasAlrededor()
    }

    fun verificarResultado(): Int {
        var casillasAbiertas = 0
        var minasMarcadasCorrectamente = 0

        for (r in 0 until filas) {
            for (c in 0 until columnas) {
                val casilla = tablero[r][c]

                // Verificar si se ha abierto una mina.
                if (casilla.isAbierta() && casilla.isMina()) {
                    return 0 // Resultado 0 mina explotada
                }

                // Contar casillas abiertas
                if (casilla.isAbierta()) {
                    casillasAbiertas++
                }

                // Contar minas correctamente marcadas
                if (casilla.isMarcada() && casilla.isMina()) {
                    minasMarcadasCorrectamente++
                }
            }
        }

        // Condición de victoria: Todas las minas están correctamente marcadas.
        if (minasMarcadasCorrectamente == numeroMinas) {
            println("Como es esto minasmarcadas: ${minasMarcadasCorrectamente} y numeroMinas: ${numeroMinas}")
            return 1 // Resultado 1: Ganó (marcó todas las minas)
        }

        // Condición de victoria Todas las casillas seguras (sin minas) están abiertas.
        val totalCasillasSeguras = (filas * columnas) - numeroMinas
        if (casillasAbiertas == totalCasillasSeguras) {
            return 2 // Resultado 2
        }

        // Si ninguna de las condiciones anteriores se cumple.
        return 3 // Resultado 3 Partida en progreso
    }
}
