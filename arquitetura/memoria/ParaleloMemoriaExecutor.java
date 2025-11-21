package arquitetura.memoria;

import core.Config;
import core.ISimulacao;
import model.VeiculoMemoria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class ParaleloMemoriaExecutor implements ISimulacao {
    private VeiculoMemoria[] estradaAtual;
    private VeiculoMemoria[] proximaEstrada;
    private ExecutorService executor;

    // nome pra tabela
    @Override
    public String getNome() {
        return "Paralelo Executor (Memoria)";
    }

    // monta a estrada
    @Override
    public void inicializar() {
        this.estradaAtual = new VeiculoMemoria[Config.L];
        this.proximaEstrada = new VeiculoMemoria[Config.L];
        SequencialMemoria.inicializarEstrada(this.estradaAtual);
        this.executor = Executors.newFixedThreadPool(Config.NUM_THREADS);
    }

    // roda a simulacao
    @Override
    public void executar() throws InterruptedException {
        // divide a estrada entre as threads
        int segmento = (int) Math.ceil((double) Config.L / Config.NUM_THREADS);

        for (int step = 0; step < Config.STEPS; step++) {
            List<Callable<Void>> tarefas = new ArrayList<>();

            // arrays da estrada
            final VeiculoMemoria[] leitura = estradaAtual;
            final VeiculoMemoria[] escrita = proximaEstrada;

            // cria as tarefas
            for (int i = 0; i < Config.NUM_THREADS; i++) {
                final int inicio = i * segmento;
                final int fim = Math.min(inicio + segmento, Config.L);

                if (inicio < Config.L) {
                    tarefas.add(() -> {
                        ThreadLocalRandom rnd = ThreadLocalRandom.current();
                        for (int idx = inicio; idx < fim; idx++) {
                            VeiculoMemoria veiculo = leitura[idx];
                            if (veiculo != null) {
                                int v = veiculo.velocidade;
                                if (v < Config.V_MAX) v++;

                                int dist = 0;
                                for (int k = 1; k < Config.L; k++) {
                                    dist++;
                                    if (leitura[(idx + k) % Config.L] != null) break;
                                }
                                v = Math.min(v, dist - 1);
                                if (rnd.nextDouble() < Config.PROBABILIDADE && v > 0) v--;
                                veiculo.velocidade = v;
                                escrita[(idx + v) % Config.L] = veiculo;
                            }
                        }
                        return null;
                    });
                }
            }

            // executa todas as threads
            executor.invokeAll(tarefas);

            // troca os arrays e limpa
            VeiculoMemoria[] temp = estradaAtual;
            estradaAtual = proximaEstrada;
            proximaEstrada = temp;
            Arrays.fill(proximaEstrada, null);
        }

        // encerra as threads
        executor.shutdown();
    }
}