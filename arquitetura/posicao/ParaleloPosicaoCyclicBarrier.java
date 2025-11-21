package arquitetura.posicao;

import core.Config;
import core.ISimulacao;
import model.VeiculoPosicao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class ParaleloPosicaoCyclicBarrier implements ISimulacao {
    private VeiculoPosicao[] estrada;
    private List<VeiculoPosicao> veiculosList;
    private ExecutorService executor;

    // nome pra tabela
    @Override
    public String getNome() {
        return "Paralelo CyclicBarrier (Posicao)";
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
        // cyclic barrier falando pra limpar a estrada antes do proximo ciclo
        CyclicBarrier barreiraCalculo = new CyclicBarrier(Config.NUM_THREADS, () -> {
            Arrays.fill(estrada, null);
        });

        // segunda barreira pra sincronizar o fim do movimento antes do proximo ciclo
        CyclicBarrier barreiraMovimento = new CyclicBarrier(Config.NUM_THREADS);

        // prepara a lista das threads
        List<Worker> workers = new ArrayList<>();
        int chunkSize = (int) Math.ceil((double) veiculosList.size() / Config.NUM_THREADS);

        // distribui a lista de veiculos e atribui uma thread para parte
        for (int i = 0; i < veiculosList.size(); i += chunkSize) {
            List<VeiculoPosicao> subLista = veiculosList.subList(i, Math.min(i + chunkSize, veiculosList.size()));
            workers.add(new Worker(subLista, barreiraCalculo, barreiraMovimento));
        }

        // inicia e depois termina as threads
        executor.invokeAll(workers);
        executor.shutdown();
    }

    // classe que implementa a thread
    private class Worker implements Callable<Void> {
        private final List<VeiculoPosicao> meusVeiculos;
        private final CyclicBarrier barreiraCalc;
        private final CyclicBarrier barreiraMov;

        public Worker(List<VeiculoPosicao> meusVeiculos, CyclicBarrier bCalc, CyclicBarrier bMov) {
            this.meusVeiculos = meusVeiculos;
            this.barreiraCalc = bCalc;
            this.barreiraMov = bMov;
        }

        // execucao da thread
        @Override
        public Void call() throws Exception {
            // Random local para evitar contenção entre threads
            ThreadLocalRandom random = ThreadLocalRandom.current();

            // loop da Simulação
            for (int step = 0; step < Config.STEPS; step++) {
                // calculo da velocidade
                for (VeiculoPosicao v : meusVeiculos) {
                    int vel = v.velocidade;
                    if (vel < Config.V_MAX) vel++;

                    int dist = 1;
                    while (dist <= Config.V_MAX) {
                        int andar = (v.posicao + dist) % Config.L;
                        if (estrada[andar] != null) break;
                        dist++;
                    }

                    vel = Math.min(vel, dist - 1);
                    if (random.nextDouble() < Config.PROBABILIDADE) {
                        vel = Math.max(vel - 1, 0);
                    }
                    v.velocidade = vel;
                }

                // barreira que limpa a estrada
                try {
                    barreiraCalc.await();
                } catch (BrokenBarrierException e) { break; }


                // movimenta os carros
                for (VeiculoPosicao v : meusVeiculos) {
                    v.andar(Config.L);
                    estrada[v.posicao] = v;
                }

                // sincroniza os carros
                try {
                    barreiraMov.await();
                } catch (BrokenBarrierException e) { break; }
            }
            return null;
        }
    }
}