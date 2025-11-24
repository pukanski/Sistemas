package arquitetura.memoria;

import core.Config;
import core.ISimulacao;
import model.VeiculoMemoria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ParaleloMemoriaCyclicBarrier implements ISimulacao {

    // arrays volateis da estrada
    private volatile VeiculoMemoria[] leitura;
    private volatile VeiculoMemoria[] escrita;

    // arrays reais da estrada
    private VeiculoMemoria[] bufferA;
    private VeiculoMemoria[] bufferB;

    private ExecutorService executor;

    // nome pra tabela
    @Override
    public String getNome() {
        return "Paralelo CyclicBarrier (Memoria)";
    }

    // monta a estrada
    @Override
    public void inicializar() {
        this.bufferA = new VeiculoMemoria[Config.L];
        this.bufferB = new VeiculoMemoria[Config.L];
        SequencialMemoria.inicializarEstrada(this.bufferA);

        this.leitura = bufferA;
        this.escrita = bufferB;

        this.executor = Executors.newFixedThreadPool(Config.NUM_THREADS);
    }

    // roda a simulacao
    @Override
    public void executar() throws InterruptedException {
        AtomicInteger contador = new AtomicInteger(0);

        // cria a cyclic barrier falando pra acontecer a troca de buffers no final do calculo de velocidade e antes do proximo ciclo
        CyclicBarrier barreira = new CyclicBarrier(Config.NUM_THREADS, () -> {
            VeiculoMemoria[] temp = leitura;
            leitura = escrita;
            escrita = temp;

            if (Config.MODO_VISUAL) {
                int stepAtual = contador.incrementAndGet();
                imprimirEstrada(leitura, stepAtual);
                try { Thread.sleep(Config.DELAY_VISUAL_MS); } catch (InterruptedException e) {}
            }

            Arrays.fill(escrita, null);
        });

        // prepara a lista das threads
        List<Worker> workers = new ArrayList<>();
        int segmento = (int) Math.ceil((double) Config.L / Config.NUM_THREADS);

        for (int i = 0; i < Config.NUM_THREADS; i++) {
            int inicio = i * segmento;
            int fim = Math.min(inicio + segmento, Config.L);
            if (inicio < Config.L) {
                workers.add(new Worker(inicio, fim, barreira));
            }
        }

        // inicia e depois termina as threads
        executor.invokeAll(workers);
        executor.shutdown();
    }

    // classe que implementa a thread
    private class Worker implements Callable<Void> {
        final int inicio, fim;
        final CyclicBarrier barreira;

        public Worker(int inicio, int fim, CyclicBarrier barreira) {
            this.inicio = inicio;
            this.fim = fim;
            this.barreira = barreira;
        }

        // execucao da thread
        @Override
        public Void call() throws Exception {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            for (int s = 0; s < Config.STEPS; s++) {
                // pega referencia dos arrays volateis
                VeiculoMemoria[] l = ParaleloMemoriaCyclicBarrier.this.leitura;
                VeiculoMemoria[] e = ParaleloMemoriaCyclicBarrier.this.escrita;

                // calculo da velocidade
                for (int i = inicio; i < fim; i++) {
                    VeiculoMemoria veiculo = l[i];
                    if (veiculo != null) {
                        int v = veiculo.velocidade;
                        if (v < Config.V_MAX) v++;

                        int dist = 0;
                        for (int k = 1; k < Config.L; k++) {
                            dist++;
                            if (l[(i + k) % Config.L] != null) break;
                        }
                        v = Math.min(v, dist - 1);
                        if (rnd.nextDouble() < Config.PROBABILIDADE && v > 0) v--;

                        veiculo.velocidade = v;
                        e[(i + v) % Config.L] = veiculo;
                    }
                }

                // espera todas as threads terminarem e depois inicia a troca de buffer
                try {
                    barreira.await();
                } catch (BrokenBarrierException ex) {
                    break;
                }
            }
            return null;
        }
    }

    private void imprimirEstrada(VeiculoMemoria[] estrada, int step) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("T=%03d [", step));
        for (VeiculoMemoria v : estrada) {
            if (v == null) sb.append(".");
            else sb.append(v.velocidade);
        }
        sb.append("]");

        System.out.print("\033[H\033[2J");
        System.out.flush();
        System.out.println(sb.toString());
    }
}