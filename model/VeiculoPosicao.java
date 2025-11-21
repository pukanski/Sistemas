package model;

import java.io.Serializable;

public class VeiculoPosicao implements Serializable {
    // versao da classe
    private static final long serialVersionUID = 1L;
    public int posicao;
    public int velocidade;

    // construtoir
    public VeiculoPosicao(int posicao, int velocidade) {
        this.posicao = posicao;
        this.velocidade = velocidade;
    }

    // metodo de andar
    public void andar(int tamEstrada) {
        this.posicao = (this.posicao + this.velocidade) % tamEstrada;
    }

    // imprimir a velocidade
    @Override
    public String toString() {
        return String.valueOf(velocidade);
    }
}