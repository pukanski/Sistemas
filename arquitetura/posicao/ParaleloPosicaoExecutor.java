package arquitetura.posicao;

import core.Config;
import core.ISimulacao;
import model.VeiculoPosicao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class ParaleloPosicaoExecutor implements ISimulacao {
    private VeiculoPosicao[] estrada;
    private List<VeiculoPosicao> veiculosList;
    private ExecutorService executor;

    // nome pra tabela
    @Override
    public String getNome() {
        return "Paralelo Executor (Posicao)";
    }

    // monta a estrada
    @Override
    public void inicializar() {
        this.estrada = new VeiculoPosicao[Config.L];
        this.veiculosList = SequencialPosicao.gerarListaInicial(this.estrada);
        this.executor = Executors.newFixedThreadPool(Config.NUM_THREADS);
    }

    // roda a simulacao
    @Override
    public void executar() throws InterruptedException {
        // divide a estrada
        List<List<VeiculoPosicao>> chunks = new ArrayList<>();
        int chunkSize = (int) Math.ceil((double) veiculosList.size() / Config.NUM_THREADS);

        // divide a lista de veiculos para cada thread
        for (int i = 0; i < veiculosList.size(); i += chunkSize) {
            chunks.add(veiculosList.subList(i, Math.min(i + chunkSize, veiculosList.size())));
        }
        int numTarefas = chunks.size();

        for (int step = 0; step < Config.STEPS; step++) {

            // inicializa as threads para o calculo da velocidade
            CountDownLatch latchCalc = new CountDownLatch(numTarefas);
            final VeiculoPosicao[] estradaLeitura = estrada;

            for (List<VeiculoPosicao> chunk : chunks) {
                executor.submit(() -> {
                    try {
                        ThreadLocalRandom rnd = ThreadLocalRandom.current();
                        for (VeiculoPosicao v : chunk) {
                            int vel = v.velocidade;
                            if (vel < Config.V_MAX) vel++;

                            int dist = 1;
                            while (dist <= Config.V_MAX) {
                                int andar = (v.posicao + dist) % Config.L;
                                if (estradaLeitura[andar] != null) break;
                                dist++;
                            }

                            vel = Math.min(vel, dist - 1);
                            if (rnd.nextDouble() < Config.PROBABILIDADE) vel = Math.max(vel - 1, 0);
                            v.velocidade = vel;
                        }
                    } finally {
                        latchCalc.countDown(); // final da tarefa
                    }
                });
            }
            latchCalc.await(); // espera o termino de todas as threads

            // limpa a estrada
            Arrays.fill(estrada, null);

            // threads pra atualizar o movimento
            CountDownLatch latchMove = new CountDownLatch(numTarefas);
            final VeiculoPosicao[] estradaEscrita = estrada;

            for (List<VeiculoPosicao> chunk : chunks) {
                executor.submit(() -> {
                    try {
                        for (VeiculoPosicao v : chunk) {
                            v.andar(Config.L);
                            estradaEscrita[v.posicao] = v;
                        }
                    } finally {
                        latchMove.countDown(); // final da tarefa
                    }
                });
            }
            latchMove.await(); // espera o termino de todas as threads, garantindo que todos os carros se moveram
        }

        // encerra as threads
        executor.shutdown();
    }
}