package distribuido.posicao.socket;

import core.Config;
import model.VeiculoPosicao;
import arquitetura.posicao.SequencialPosicao;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Master {
    static final String HOST = "localhost";
    static final int PORTA_BASE = 13000;

    private int numSlaves;
    private VeiculoPosicao[] estrada;
    private List<VeiculoPosicao> veiculosList;
    private List<SocketContext> conexoes;
    private ExecutorService executor;

    public Master(int n) {
        this.numSlaves = n;
        this.estrada = new VeiculoPosicao[Config.L];
        this.veiculosList = SequencialPosicao.gerarListaInicial(this.estrada);
        this.conexoes = new ArrayList<>();
        this.executor = Executors.newFixedThreadPool(n);
    }

    public void conectar() throws IOException {
        System.out.println("Conectando a " + numSlaves + " slaves (Socket Posicao)...");
        for (int i = 0; i < numSlaves; i++) {
            int porta = PORTA_BASE + i;
            conexoes.add(new SocketContext(HOST, porta));
            System.out.println(" + Conectado ao Slave " + (i + 1) + " na porta " + porta);
        }
    }

    public void executar() throws Exception {
        // define o tamanho da lista de veiculos com base nos slaves
        int chunkSize = (int) Math.ceil((double) veiculosList.size() / numSlaves);

        for (int step = 0; step < Config.STEPS; step++) {
            // copia da estrada
            final VeiculoPosicao[] copiaEstrada = Arrays.copyOf(estrada, Config.L);
            List<Future<List<VeiculoPosicao>>> futures = new ArrayList<>();

            for (int i = 0; i < numSlaves; i++) {
                int inicio = i * chunkSize;
                int fim = Math.min(inicio + chunkSize, veiculosList.size());
                List<VeiculoPosicao> subLista = new ArrayList<>(veiculosList.subList(inicio, fim));
                SocketContext ctx = conexoes.get(i);

                futures.add(executor.submit(() -> {
                    synchronized (ctx) {
                        ctx.out.writeObject(copiaEstrada); // envia a copia da estrada
                        ctx.out.writeObject(subLista); // envia a lista dos veiculos
                        ctx.out.flush(); // envia os dados
                        ctx.out.reset(); // limpa o cache
                        return (List<VeiculoPosicao>) ctx.in.readObject(); // bloqueia ate ter as respostas dos slaves
                    }
                }));
            }

            // limpa a estrada
            Arrays.fill(estrada, null);
            List<VeiculoPosicao> novaLista = new ArrayList<>();

            // espera todos os slaves e agrupa os resultados
            for (Future<List<VeiculoPosicao>> f : futures) {
                List<VeiculoPosicao> parcial = f.get();
                for (VeiculoPosicao v : parcial) {
                    v.andar(Config.L);
                    estrada[v.posicao] = v;
                    novaLista.add(v);
                }
            }
            this.veiculosList = novaLista;
        }
    }

    public void fechar() {
        executor.shutdown(); // encerra o pool de threads
        for(SocketContext ctx : conexoes) ctx.fechar(); // fecha os sockets
    }

    public static void main(String[] args) {
        // le o numero de slaves pelo args
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

            System.out.println("\nRESULTADO (Posicao Socket):");
            System.out.printf("Slaves: %d | Tempo: %d ms | Memoria: %d MB\n",
                    n, (end - start)/1_000_000, Math.max(0, memDepois - memAntes));

            master.fechar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // calcula a memoria usada em MB
    static long medMemoria() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    // classe pra gerenciar os sockets e o fluxo IO
    static class SocketContext {
        Socket s; ObjectOutputStream out; ObjectInputStream in;
        SocketContext(String host, int port) throws IOException {
            s = new Socket(host, port);
            out = new ObjectOutputStream(s.getOutputStream());
            in = new ObjectInputStream(s.getInputStream());
        }
        void fechar() { try { s.close(); } catch (Exception e) {} }
    }
}