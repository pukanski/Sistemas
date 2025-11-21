package arquitetura.posicao;

import core.Config;
import core.ISimulacao;
import model.VeiculoPosicao;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;

public class ParaleloPosicaoStream implements ISimulacao {
    private VeiculoPosicao[] estrada;
    private List<VeiculoPosicao> veiculosList;
    private ForkJoinPool customPool;

    // nome pra tabela
    @Override
    public String getNome() {
        return "Paralelo Stream (Posicao)";
    }

    // monta a estrada
    @Override
    public void inicializar() {
        this.estrada = new VeiculoPosicao[Config.L];
        this.veiculosList = SequencialPosicao.gerarListaInicial(this.estrada);
        this.customPool = new ForkJoinPool(Config.NUM_THREADS);
    }

    // roda a simulacao
    @Override
    public void executar() throws Exception {
        // pool de threads executando paralelamente a simulacao
        customPool.submit(() -> {
            for (int step = 0; step < Config.STEPS; step++) {

                // referencia de estrada
                final VeiculoPosicao[] estradaLeitura = estrada;

                // calculo da velocidade paralelamente
                veiculosList.parallelStream().forEach(v -> {
                    int vel = v.velocidade;
                    if (vel < Config.V_MAX) vel++;

                    int dist = 1;
                    while (dist <= Config.V_MAX) {
                        int andar = (v.posicao + dist) % Config.L;
                        if (estradaLeitura[andar] != null) break;
                        dist++;
                    }

                    vel = Math.min(vel, dist - 1);

                    if (ThreadLocalRandom.current().nextDouble() < Config.PROBABILIDADE) {
                        vel = Math.max(vel - 1, 0);
                    }
                    v.velocidade = vel;
                });

                // limpa a estrada
                Arrays.fill(estrada, null);

                // movimenta os veiculos paralelamente
                veiculosList.parallelStream().forEach(v -> {
                    v.andar(Config.L);
                    estrada[v.posicao] = v;
                });
            }
        }).get(); // espera as threads terminarem

        // termina o pool de threads
        customPool.shutdown();
    }
}