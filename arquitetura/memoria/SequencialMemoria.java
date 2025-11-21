package arquitetura.memoria;

import core.Config;
import core.ISimulacao;
import model.VeiculoMemoria;

import java.util.*;

public class SequencialMemoria implements ISimulacao {
    private VeiculoMemoria[] estradaAtual;
    private VeiculoMemoria[] proximaEstrada;
    private Random random;

    // nome pra tabela
    @Override
    public String getNome() {
        return "Sequencial (Memoria)";
    }

    // monta a estrada
    @Override
    public void inicializar() {
        this.estradaAtual = new VeiculoMemoria[Config.L];
        this.proximaEstrada = new VeiculoMemoria[Config.L];
        this.random = new Random();
        inicializarEstrada(this.estradaAtual);
    }

    // roda a simulacao
    @Override
    public void executar() {
        for (int step = 0; step < Config.STEPS; step++) {

            // anda no array estrada inteiro
            for (int i = 0; i < Config.L; i++) {
                VeiculoMemoria veiculo = estradaAtual[i];

                if (veiculo != null) {
                    int v = veiculo.velocidade;

                    // fase 1: acelera
                    if (v < Config.V_MAX) v++;

                    // calcula a distancia pro proximo carro
                    int dist = 0;
                    for (int k = 1; k < Config.L; k++) {
                        dist++;
                        if (estradaAtual[(i + k) % Config.L] != null) break;
                    }

                    // fase 2: desacelera
                    v = Math.min(v, dist - 1);

                    // fase 3: randomiza
                    if (random.nextDouble() < Config.PROBABILIDADE && v > 0) {
                        v--;
                    }

                    // fase 4: define a velocidade
                    veiculo.velocidade = v;

                    // escreve o movimento na proxima estrada
                    proximaEstrada[(i + v) % Config.L] = veiculo;
                }
            }

            // depois do calculo de todos os movimentos e trocada a estrada atual pela proxima
            VeiculoMemoria[] temp = estradaAtual;
            estradaAtual = proximaEstrada;
            proximaEstrada = temp;

            // limpa o array proximaEstrada
            Arrays.fill(proximaEstrada, null);
        }
    }

    // monta e popula a estrada
    public static void inicializarEstrada(VeiculoMemoria[] estrada) {
        Random r = new Random();
        Set<Integer> posicoes = new HashSet<>();
        while (posicoes.size() < Config.NUM_VEICULOS) {
            posicoes.add(r.nextInt(Config.L));
        }
        for (int pos : posicoes) {
            estrada[pos] = new VeiculoMemoria(r.nextInt(Config.V_MAX + 1));
        }
    }
}