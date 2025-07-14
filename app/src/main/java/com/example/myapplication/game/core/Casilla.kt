package com.example.myapplication.game.core

class Casilla(coordenadaX: Int, coordenadaY: Int) { // Constructor primario

    // Propiedades privadas, inicializadas por el constructor o con valores por defecto
    private var x: Int = coordenadaX
    private var y: Int = coordenadaY
    private var minas_alrededor: Int = 0

    private var mina: Boolean = false       // Valor por defecto
    private var abierta: Boolean = false    // Valor por defecto
    private var marcada: Boolean = false    // Valor por defecto
    private var disponible: Boolean = true  // Valor por defecto

    fun getX(): Int {
        return x
    }

    fun getY(): Int {
        return y
    }

    fun getMinasAlrededor(): Int {
        return minas_alrededor
    }

    fun isMina(): Boolean {
        return mina
    }

    fun isAbierta(): Boolean {
        return abierta
    }

    fun isMarcada(): Boolean {
        return marcada
    }

    fun setMina(esMina: Boolean) {
        this.mina = esMina
    }

    override fun toString(): String {
        return "Casilla(x=$x, y=$y, minas_alrededor=$minas_alrededor, mina=$mina, abierta=$abierta, marcada=$marcada, disponible=$disponible)"
    }

    fun incrementarMinasAlrededor() {
        if (!mina) { // Solo incrementa si no es una mina en sí misma
            minas_alrededor++
        }
    }

    fun abrir() {
        this.abierta = true
        this.disponible = false
        }

    fun marcar() {
        this.marcada = true
        this.disponible = false
    }

    fun desmarcar(){
        this.marcada = false
        this.disponible = true
    }
}
