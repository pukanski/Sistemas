package core;

import arquitetura.posicao.*;
import arquitetura.memoria.*;

import java.util.ArrayList;
import java.util.List;

public class Benchmark {

    // configuracoes do benchmark
    static final int RODADAS = 5;       // quantas vezes rodar para tirar a media
    static final boolean WARMUP = true; // rodar uma vez antes sem contar (aquecimento da JVM)

    public static void main(String[] args) {
        System.out.println("Benchmark de Simulacao de Trafego (Media de " + RODADAS + " rodadas)  ");
        System.out.println("Configuração:");
        System.out.printf("  Estrada (L): %d células\n", Config.L);
        System.out.printf("  Carros:      %d\n", Config.NUM_VEICULOS);
        System.out.printf("  Passos:      %d\n", Config.STEPS);
        System.out.printf("  Threads:     %d\n", Config.NUM_THREADS);

        // lista das simulacoes testadas
        List<ISimulacao> simulacoes = new ArrayList<>();

        // arquitetura posicao
        simulacoes.add(new SequencialPosicao());
        simulacoes.add(new ParaleloPosicaoStream());
        simulacoes.add(new ParaleloPosicaoExecutor());
        simulacoes.add(new ParaleloPosicaoCyclicBarrier());

        // arquitetura memoria
        simulacoes.add(new SequencialMemoria());
        simulacoes.add(new ParaleloMemoriaStream());
        simulacoes.add(new ParaleloMemoriaExecutor());
        simulacoes.add(new ParaleloMemoriaCyclicBarrier());

        // cabecalho ajustado para media
        System.out.printf("%-35s | %-15s | %-15s\n", "IMPLEMENTAÇÃO", "TEMPO MÉDIO (ms)", "MEMÓRIA PICO (MB)");
        System.out.println("-------------------------------------------------------------------------");

        // execucoes
        for (ISimulacao sim : simulacoes) {
            rodarTeste(sim);
        }

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("Benchmark concluído.");
    }

    static void rodarTeste(ISimulacao sim) {
        long somaTempo = 0;
        long maxMemoria = 0;

        try {
            // warmup do jit
            if (WARMUP) {
                System.gc();
                sim.inicializar();
                sim.executar();
            }

            // rodadas usadas pra calcular o tempo medio
            for (int i = 0; i < RODADAS; i++) {

                // usa o garbage collector pra limpar a memoria antes da execucao
                System.gc();
                try {
                    Thread.sleep(200); // pausa pro gc funcionar direito
                } catch (InterruptedException e) {}

                // monta a estrada
                sim.inicializar();

                // comeca as medicoes
                long memoriaAntes = medMemoria();
                long inicio = System.nanoTime();

                // simula o trafego
                sim.executar();

                // termina as medicoes
                long fim = System.nanoTime();
                long memoriaDepois = medMemoria();

                // transforma de nano pra mili segundos
                long tempoMs = (fim - inicio) / 1_000_000;

                // memoria a mais que foi usada na simulacao
                long usoMemoria = Math.max(0, memoriaDepois - memoriaAntes);

                // acumula para a media e verifica se foi o pico de memoria
                somaTempo += tempoMs;
                if (usoMemoria > maxMemoria) {
                    maxMemoria = usoMemoria;
                }
            }

            // calcula a media do tempo
            double mediaTempo = (double) somaTempo / RODADAS;

            // imprime as informacacoes da simulacao
            System.out.printf("%-35s | %-15.1f | %-15d\n", sim.getNome(), mediaTempo, maxMemoria);

        } catch (Exception e) {
            System.out.printf("%-35s | FALHOU: %s\n", sim.getNome(), e.getMessage());
            e.printStackTrace();
        }
    }

    // informacoes do sistema
    static long medMemoria() {
        Runtime rt = Runtime.getRuntime();
        // pega a memoria total diminui pela livre e depois converte os bytes em megabytes
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }
}