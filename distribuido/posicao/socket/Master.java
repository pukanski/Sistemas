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
    static final int PORTA_BASE = 51000;

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
        System.out.println("Tentando conectar a " + numSlaves + " slaves (Socket Posicao)...");
        for (int i = 0; i < numSlaves; i++) {
            int porta = PORTA_BASE + i;
            conexoes.add(new SocketContext(HOST, porta));
            System.out.println(" + Conectado ao Slave " + (i + 1));
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

            if (Config.MODO_VISUAL) {
                imprimirEstrada(estrada, step);
                try { Thread.sleep(Config.DELAY_VISUAL_MS); } catch (Exception e) {}
            }
        }
    }

    public void fechar() {
        executor.shutdown(); // encerra o pool de threads
        for(SocketContext ctx : conexoes) ctx.fechar(); // fecha os sockets
    }

    public static void main(String[] args) {
        // le o numero de slaves e rodadas pelos args
        int n = (args.length > 0) ? Integer.parseInt(args[0]) : 2;
        int rounds = (args.length > 1) ? Integer.parseInt(args[1]) : 5;

        try {
            System.out.println(" Master Socket (Posicao) - Slaves: " + n + " | Rodadas: " + rounds);

            Master master = new Master(n);
            master.conectar();

            // warmup
            master.executar();

            long somaTempo = 0;
            long maxMemoria = 0;

            // loop de medicoes
            for(int i=1; i<=rounds; i++) {
                System.gc(); Thread.sleep(500);

                long memAntes = medMemoria();
                long start = System.nanoTime();

                master.executar();

                long end = System.nanoTime();
                long memDepois = medMemoria();

                long tempoMs = (end - start)/1_000_000;
                long usoMem = Math.max(0, memDepois - memAntes);

                somaTempo += tempoMs;
                if(usoMem > maxMemoria) maxMemoria = usoMem;

                System.out.printf("Rodada %d: %d ms | %d MB\n", i, tempoMs, usoMem);
            }

            master.fechar();

            double media = (double) somaTempo / rounds;
            System.out.println("\nResultado Final:");
            System.out.printf("Tempo Medio: %.1f ms\n", media);
            System.out.printf("Memoria Pico: %d MB\n", maxMemoria);

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
        Socket s;
        ObjectOutputStream out;
        ObjectInputStream in;

        SocketContext(String host, int port) throws IOException {
            s = new Socket(host, port);

            // agrupa os bytes antes de enviar
            s.setTcpNoDelay(true); // desliga o algoritmo de Nagle

            out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out.flush(); // envia o cabeçalho inicial

            in = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
        }

        void fechar() { try { s.close(); } catch (Exception e) {} }
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