package model;

import java.io.Serializable;

public class VeiculoMemoria implements Serializable {
    // versao da classe
    private static final long serialVersionUID = 1L;
    public int velocidade;

    // construtor
    public VeiculoMemoria(int velocidade) {
        this.velocidade = velocidade;
    }

    // imprimir a velocidade
    @Override
    public String toString() {
        return String.valueOf(velocidade);
    }
}