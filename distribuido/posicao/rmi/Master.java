package distribuido.posicao.rmi;

import core.Config;
import model.VeiculoPosicao;
import arquitetura.posicao.SequencialPosicao;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;

public class Master {

    private int numSlaves;
    private VeiculoPosicao[] estrada;
    private List<VeiculoPosicao> veiculosList;
    private List<ISlave> slaves;
    private ExecutorService executor;

    public Master(int n) {
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
        }
        executor.shutdown(); // encerra o pool de threads local
    }

    public static void main(String[] args) {
        // define o numero de slaves pelo args
        int n = (args.length > 0) ? Integer.parseInt(args[0]) : 2;

        try {
            Master master = new Master(n);
            master.conectar();

            System.gc(); Thread.sleep(500);
            long memAntes = medMemoria();
            long start = System.nanoTime();

            master.executar();

            long end = System.nanoTime();
            long memDepois = medMemoria();

            System.out.println("\nResultado (Posicao RMI):");
            System.out.printf("Slaves: %d | Tempo: %d ms | Memoria: %d MB\n",
                    n, (end - start)/1_000_000, Math.max(0, memDepois - memAntes));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // mede o uso da memoria em MB
    static long medMemoria() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }
}