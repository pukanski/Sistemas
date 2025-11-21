package core;

import arquitetura.posicao.*;
import arquitetura.memoria.*;

import java.util.ArrayList;
import java.util.List;

public class Benchmark {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("   BENCHMARK DE SIMULAÇÃO DE TRÁFEGO (Nagel-Schreckenberg)      ");
        System.out.println("=================================================================");
        System.out.println("Configuração:");
        System.out.printf("  Estrada (L): %d células\n", Config.L);
        System.out.printf("  Carros:      %d\n", Config.NUM_VEICULOS);
        System.out.printf("  Passos:      %d\n", Config.STEPS);
        System.out.printf("  Threads:     %d (Hardware)\n", Config.NUM_THREADS);
        System.out.println("=================================================================\n");

        // lista das simulacoes testadas
        List<ISimulacao> simulacoes = new ArrayList<>();

        // arquitetura posicao
        simulacoes.add(new SequencialPosicao());
        simulacoes.add(new ParaleloPosicaoStream());
        simulacoes.add(new ParaleloPosicaoExecutor());
        simulacoes.add(new ParaleloPosicaoCyclicBarrier());

        // arquitetura paralelo
        simulacoes.add(new SequencialMemoria());
        simulacoes.add(new ParaleloMemoriaStream());
        simulacoes.add(new ParaleloMemoriaExecutor());
        simulacoes.add(new ParaleloMemoriaCyclicBarrier());

        // cabecalho
        System.out.printf("%-35s | %-10s | %-15s\n", "IMPLEMENTAÇÃO", "TEMPO (ms)", "MEMÓRIA (MB)");
        System.out.println("----------------------------------------------------------------------");

        // execocoes
        for (ISimulacao sim : simulacoes) {
            rodarTeste(sim);
        }

        System.out.println("----------------------------------------------------------------------");
        System.out.println("Benchmark concluído.");
    }

    static void rodarTeste(ISimulacao sim) {
        // usa o garbage collector pra limpar a memoria antes da execucao
        System.gc();
        try {
            Thread.sleep(500); // pausa pro gc funcionar direito
        } catch (InterruptedException e) {}

        try {
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

            // imprime as inforcacoes da simulacao
            System.out.printf("%-35s | %-10d | %-15d\n", sim.getNome(), tempoMs, usoMemoria);

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