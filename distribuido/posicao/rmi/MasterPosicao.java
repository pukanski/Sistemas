package distribuido.posicao.rmi;

import core.Config;
import model.VeiculoPosicao;
import arquitetura.posicao.SequencialPosicao;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;

public class MasterPosicao {
    private int numSlaves;
    private VeiculoPosicao[] estrada;
    private List<VeiculoPosicao> veiculosList;
    private List<ISlave> slaves;
    private ExecutorService executor;

    public MasterPosicao(int n) {
        this.numSlaves = n;
        this.estrada = new VeiculoPosicao[Config.L];
        this.veiculosList = SequencialPosicao.gerarListaInicial(this.estrada);
        this.slaves = new ArrayList<>();
        this.executor = Executors.newFixedThreadPool(n);
    }

    public void conectar() throws Exception {
        // registro rmi no localhost
        Registry registry = LocateRegistry.getRegistry("localhost");
        System.out.println("Procurando " + numSlaves + " slaves RMI (Posicao)...");
        for (int i = 1; i <= numSlaves; i++) {
            slaves.add((ISlave) registry.lookup("Slave" + i)); // tenta se conectar procurando por "Slave"
            System.out.println(" + Conectado ao Slave" + i);
        }
    }

    public void executar() throws Exception {
        // calcula o tamanho dos veiculos com base na quantidade de Slaves
        int chunkSize = (int) Math.ceil((double) veiculosList.size() / numSlaves);

        for (int step = 0; step < Config.STEPS; step++) {
            // lista de tarefas e a copia da estrada
            List<Future<List<VeiculoPosicao>>> futures = new ArrayList<>();
            final VeiculoPosicao[] copiaEstrada = Arrays.copyOf(estrada, Config.L);

            for (int i = 0; i < numSlaves; i++) {
                final int id = i;
                int inicio = i * chunkSize;
                int fim = Math.min(inicio + chunkSize, veiculosList.size());

                List<VeiculoPosicao> subLista = new ArrayList<>(veiculosList.subList(inicio, fim));

                // manda os veiculos e a estrada pra processamento
                futures.add(executor.submit(() ->
                        slaves.get(id).calcularVelocidades(copiaEstrada, subLista)
                ));
            }

            // limpa a estrada
            Arrays.fill(estrada, null);
            List<VeiculoPosicao> novaLista = new ArrayList<>();

            // aguarda resposta dos slaves, move os veiculos e reorganiza a estrada
            for (Future<List<VeiculoPosicao>> f : futures) {
                List<VeiculoPosicao> parcial = f.get(); // bloqueia ate receber as respostas
                for (VeiculoPosicao v : parcial) {
                    v.andar(Config.L);
                    estrada[v.posicao] = v;
                    novaLista.add(v);
                }
            }
            this.veiculosList = novaLista;

            if (Config.MODO_VISUAL) {
                imprimirEstrada(estrada, step);
                try { Thread.sleep(Config.DELAY_VISUAL_MS); } catch (Exception e) {}
            }
        }
    }

    public void encerrar() {
        executor.shutdown(); // encerra o pool de threads local
    }

    public static void main(String[] args) {
        // define o numero de slaves e rodadas pelo args
        int n = (args.length > 0) ? Integer.parseInt(args[0]) : 2;
        int rounds = (args.length > 1) ? Integer.parseInt(args[1]) : 5;

        try {
            System.out.println(" Master RMI (Posicao) - Slaves: " + n + " | Rodadas: " + rounds);

            MasterPosicao masterPosicao = new MasterPosicao(n);
            masterPosicao.conectar();

            // warmup
            masterPosicao.executar();

            long somaTempo = 0;
            long maxMemoria = 0;

            // loop de medicoes
            for(int i=1; i<=rounds; i++) {
                System.gc(); Thread.sleep(500);

                long memAntes = medMemoria();
                long start = System.nanoTime();

                masterPosicao.executar();

                long end = System.nanoTime();
                long memDepois = medMemoria();

                long tempoMs = (end - start)/1_000_000;
                long usoMem = Math.max(0, memDepois - memAntes);

                somaTempo += tempoMs;
                if(usoMem > maxMemoria) maxMemoria = usoMem;

                System.out.printf("Rodada %d: %d ms | %d MB\n", i, tempoMs, usoMem);
            }

            masterPosicao.encerrar();

            // resultado final media
            double media = (double) somaTempo / rounds;
            System.out.println("\nResultado Final:");
            System.out.printf("Tempo Medio: %.1f ms\n", media);
            System.out.printf("Memoria Pico: %d MB\n", maxMemoria);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // mede o uso da memoria em MB
    static long medMemoria() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    private void imprimirEstrada(VeiculoPosicao[] estrada, int step) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("T=%03d [", step));
        for (VeiculoPosicao v : estrada) {
            if (v == null) sb.append(".");
            else sb.append(v.velocidade);
        }
        sb.append("]");

        System.out.print("\033[H\033[2J");
        System.out.flush();
        System.out.println(sb.toString());
    }
}