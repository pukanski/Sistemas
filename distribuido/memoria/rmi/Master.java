package distribuido.memoria.rmi;

import core.Config;
import model.VeiculoMemoria;
import arquitetura.memoria.SequencialMemoria;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;

public class Master {
    private int numSlaves;

    private VeiculoMemoria[] estradaAtual;
    private VeiculoMemoria[] proximaEstrada;
    private ExecutorService executor;
    private List<ISlave> slaves;

    // construtor do master e ja inicializa a estrada
    public Master(int n) {
        this.numSlaves = n;
        this.estradaAtual = new VeiculoMemoria[Config.L];
        this.proximaEstrada = new VeiculoMemoria[Config.L];
        this.slaves = new ArrayList<>();
        this.executor = Executors.newFixedThreadPool(n);

        SequencialMemoria.inicializarEstrada(estradaAtual);
    }

    public void conectar() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost");
        System.out.println("Procurando " + numSlaves + " slaves...");

        for (int i = 1; i <= numSlaves; i++) {
            // tenta se conectar procurando "Slave"
            slaves.add((ISlave) registry.lookup("Slave" + i));
            System.out.println(" + Conectado ao Slave" + i);
        }
    }

    public void executar() throws Exception {
        // divide a estrada a partir do numero de slaves
        int segmento = (int) Math.ceil((double) Config.L / numSlaves);

        for (int step = 0; step < Config.STEPS; step++) {
            List<Future<VeiculoMemoria[]>> futures = new ArrayList<>();

            // copia da estrada pra enviar
            final VeiculoMemoria[] copiaEstrada = Arrays.copyOf(estradaAtual, Config.L);

            // distribui as taferas pros slaves
            for (int i = 0; i < numSlaves; i++) {
                final int id = i;
                final int inicio = i * segmento;
                final int fim = Math.min(inicio + segmento, Config.L);

                // chama o metodo nos slaves
                futures.add(executor.submit(() ->
                        slaves.get(id).calcularSegmento(copiaEstrada, inicio, fim)
                ));
            }

            // espera as respostas e cria a proxima estrada
            Arrays.fill(proximaEstrada, null);
            for (Future<VeiculoMemoria[]> f : futures) {
                VeiculoMemoria[] parcial = f.get(); // bloqueia ate o slave mandar o resultado
                for (int k = 0; k < Config.L; k++) {
                    if (parcial[k] != null) proximaEstrada[k] = parcial[k];
                }
            }

            // faz a troca de estradas
            VeiculoMemoria[] temp = estradaAtual;
            estradaAtual = proximaEstrada;
            proximaEstrada = temp;
        }
    }

    public void encerrar() {
        executor.shutdown();
    }

    public static void main(String[] args) {
        // le o numero de slaves e execucoes pelos args
        int n = (args.length > 0) ? Integer.parseInt(args[0]) : 2;
        int rounds = (args.length > 1) ? Integer.parseInt(args[1]) : 5;

        try {
            System.out.println(" Master RMI (Memoria) - Slaves: " + n + " | Rodadas: " + rounds);

            Master master = new Master(n);
            master.conectar();

            // warmup
            master.executar();

            long somaTempo = 0;
            long maxMemoria = 0;

            // loop de medicoes
            for (int i = 1; i <= rounds; i++) {
                // limpa a memoria pro benchmark
                System.gc();
                Thread.sleep(500);

                long memAntes = medMemoria();
                long start = System.nanoTime();

                master.executar(); // executa a simulacao

                long end = System.nanoTime();
                long memDepois = medMemoria();

                long tempoMs = (end - start) / 1_000_000;
                long usoMem = Math.max(0, memDepois - memAntes);

                somaTempo += tempoMs;
                if (usoMem > maxMemoria) maxMemoria = usoMem;

                System.out.printf("Rodada %d: %d ms | %d MB\n", i, tempoMs, usoMem);
            }

            master.encerrar();

            // resultado final media
            double media = (double) somaTempo / rounds;
            System.out.println("\nResultado Final:");
            System.out.printf("Tempo Medio: %.1f ms\n", media);
            System.out.printf("Memoria Pico: %d MB (no Master)\n", maxMemoria);

        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
        }
    }

    // calcula a memoria em MB
    static long medMemoria() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }
}