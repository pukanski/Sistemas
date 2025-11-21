package distribuido.memoria.socket;

import core.Config;
import model.VeiculoMemoria;
import arquitetura.memoria.SequencialMemoria;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Master {
    static final String HOST = "localhost";
    static final int PORTA_BASE = 12345;

    private int numSlaves;
    private VeiculoMemoria[] estradaAtual;
    private VeiculoMemoria[] proximaEstrada;
    private List<SocketContext> conexoes;
    private ExecutorService executor;

    public Master(int n) {
        this.numSlaves = n;
        this.estradaAtual = new VeiculoMemoria[Config.L];
        this.proximaEstrada = new VeiculoMemoria[Config.L];
        this.conexoes = new ArrayList<>();
        this.executor = Executors.newFixedThreadPool(n);

        SequencialMemoria.inicializarEstrada(estradaAtual);
    }

    // metodo pra estabelecer as conexoes usando TCP
    public void conectar() throws IOException {
        System.out.println("Tentando conectar a " + numSlaves + " slaves (Socket)...");
        for (int i = 0; i < numSlaves; i++) {
            int porta = PORTA_BASE + i;
            conexoes.add(new SocketContext(HOST, porta));
            System.out.println(" + Conectado ao Slave " + (i + 1) + " na porta " + porta);
        }
    }

    public void executar() throws Exception {
        int segmento = (int) Math.ceil((double) Config.L / numSlaves);

        for (int step = 0; step < Config.STEPS; step++) {
            final VeiculoMemoria[] copia = Arrays.copyOf(estradaAtual, Config.L);
            List<Future<VeiculoMemoria[]>> futures = new ArrayList<>();

            for (int i = 0; i < numSlaves; i++) {
                final int inicio = i * segmento;
                final int fim = Math.min(inicio + segmento, Config.L);
                final SocketContext ctx = conexoes.get(i);

                // envia a tarefa pra uma thread que gerencia a comunicao
                futures.add(executor.submit(() -> {
                    synchronized(ctx) {
                        ctx.out.writeInt(inicio); // enviao indice de inicio
                        ctx.out.writeInt(fim); // envia o indice final
                        ctx.out.writeObject(copia); // envia a copia da estrada
                        ctx.out.flush(); // envia os dados
                        ctx.out.reset(); // limpa o cache
                        return (VeiculoMemoria[]) ctx.in.readObject(); // bloqueia ate ter as respostas dos slaves
                    }
                }));
            }

            // sincroniza os resultados
            Arrays.fill(proximaEstrada, null);
            for(Future<VeiculoMemoria[]> f : futures) {
                VeiculoMemoria[] parcial = f.get(); // espera as threads receberem os dados
                for(int k=0; k<Config.L; k++) {
                    if(parcial[k] != null) proximaEstrada[k] = parcial[k];
                }
            }

            // troca as estradas
            VeiculoMemoria[] temp = estradaAtual;
            estradaAtual = proximaEstrada;
            proximaEstrada = temp;
        }
    }

    public void fechar() {
        executor.shutdown(); // encerra o pool
        for(SocketContext ctx : conexoes) ctx.fechar(); // fecha os sockets
    }

    public static void main(String[] args) {
        // define o numero de slaves pelo args
        int n = (args.length > 0) ? Integer.parseInt(args[0]) : 2;

        try {
            Master master = new Master(n);
            master.conectar(); // conecta com os slaves

            System.gc(); Thread.sleep(500);
            long memAntes = medMemoria();
            long start = System.nanoTime();

            master.executar();

            long end = System.nanoTime();
            long memDepois = medMemoria();

            System.out.println("\nResultado (Memoria Socket):");
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

    // encapsula o Socket e os fluxos IO
    static class SocketContext {
        Socket s; ObjectOutputStream out; ObjectInputStream in;
        SocketContext(String host, int port) throws IOException {
            s = new Socket(host, port);
            out = new ObjectOutputStream(s.getOutputStream());
            in = new ObjectInputStream(s.getInputStream());
        }
        void fechar() { try { s.close(); } catch (Exception e) {} } // fecha a conexao
    }
}