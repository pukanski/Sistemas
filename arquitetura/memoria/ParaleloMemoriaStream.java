package arquitetura.memoria;

import core.Config;
import core.ISimulacao;
import model.VeiculoMemoria;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class ParaleloMemoriaStream implements ISimulacao {
    private VeiculoMemoria[] estradaAtual;
    private VeiculoMemoria[] proximaEstrada;
    private ForkJoinPool customPool;

    // nome pra tabela
    @Override
    public String getNome() {
        return "Paralelo Stream (Memoria)";
    }

    // monta a estrada
    @Override
    public void inicializar() {
        this.estradaAtual = new VeiculoMemoria[Config.L];
        this.proximaEstrada = new VeiculoMemoria[Config.L];
        SequencialMemoria.inicializarEstrada(this.estradaAtual);
        this.customPool = new ForkJoinPool(Config.NUM_THREADS);
    }

    // roda a simulacao
    @Override
    public void executar() throws Exception {
        // array pra trocar dentro do submit
        final VeiculoMemoria[][] buffers = { estradaAtual, proximaEstrada };

        // pool de threads executando paralelamente a simulacao e trocando os buffers no final
        customPool.submit(() -> {
            for (int step = 0; step < Config.STEPS; step++) {

                final VeiculoMemoria[] leitura = buffers[0];
                final VeiculoMemoria[] escrita = buffers[1];

                // divide os indices entre as threads
                IntStream.range(0, Config.L).parallel().forEach(i -> {
                    VeiculoMemoria veiculo = leitura[i];

                    if (veiculo != null) {
                        int v = veiculo.velocidade;
                        // logica de velocidade igual a sequencial
                        if (v < Config.V_MAX) v++;

                        int dist = 0;
                        for (int k = 1; k < Config.L; k++) {
                            dist++;
                            if (leitura[(i + k) % Config.L] != null) break;
                        }
                        v = Math.min(v, dist - 1);

                        if (ThreadLocalRandom.current().nextDouble() < Config.PROBABILIDADE && v > 0) {
                            v--;
                        }
                        veiculo.velocidade = v;
                        escrita[(i + v) % Config.L] = veiculo;
                    }
                });

                // troca de Buffers das estradas
                buffers[0] = escrita;
                buffers[1] = leitura;
                Arrays.fill(buffers[1], null); // lçimpa a escrita
            }
        }).get();

        // termina o pool de threads
        customPool.shutdown();
    }
}