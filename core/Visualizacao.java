package core;

import arquitetura.memoria.*;
import arquitetura.posicao.*;
import distribuido.posicao.rmi.MasterPosicao;

import java.util.Scanner;

public class Visualizacao {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        Config.L = 50;
        Config.NUM_VEICULOS = 16;
        Config.STEPS = 30;
        Config.MODO_VISUAL = true;
        Config.DELAY_VISUAL_MS = 150;
        Config.NUM_THREADS = 4;

        while (true) {
            System.out.print("\033[H\033[2J"); System.out.flush();
            System.out.println("Menu de visualizacao da simulacao de NaSch          ");
            System.out.println("Config: L=" + Config.L + ", Carros=" + Config.NUM_VEICULOS);
            System.out.println("-----------------------------------------");
            System.out.println("1. Arquitetura MEMORIA");
            System.out.println("2. Arquitetura POSICAO");
            System.out.println("0. Sair");
            System.out.print("Escolha: ");

            int arq = scanner.nextInt();
            if (arq == 0) break;

            System.out.println("\n----- Escolha a Implementação -----");
            System.out.println("1. Sequencial");
            System.out.println("2. Paralelo (Stream)");
            System.out.println("3. Paralelo (Executor)");
            System.out.println("4. Paralelo (CyclicBarrier)");
            System.out.println("5. Distribuído (RMI)");
            System.out.println("6. Distribuído (Socket)");
            System.out.print("Opção: ");
            int impl = scanner.nextInt();

            System.out.println("\n>>> Iniciando simulacao... <<<\n");
            Thread.sleep(1000);

            try {
                if (arq == 1) rodarMemoria(impl);
                else if (arq == 2) rodarPosicao(impl);
            } catch (Exception e) {
                System.err.println("Erro na execucao: " + e.getMessage());
            }

            System.out.println("\n----- Fim da Simulação -----");
            System.out.println("Pressione Enter para voltar ao menu");
            new Scanner(System.in).nextLine();
        }
    }

    static void rodarMemoria(int opcao) throws Exception {
        ISimulacao sim = null;
        switch (opcao) {
            case 1: sim = new SequencialMemoria(); break;
            case 2: sim = new ParaleloMemoriaStream(); break;
            case 3: sim = new ParaleloMemoriaExecutor(); break;
            case 4: sim = new ParaleloMemoriaCyclicBarrier(); break;
            case 5:
                avisoDistribuido("RMI Memoria");
                distribuido.memoria.rmi.Master.main(new String[]{"2", "1"});
                return;
            case 6:
                avisoDistribuido("Socket Memoria");
                distribuido.memoria.socket.Master.main(new String[]{"2", "1"});
                return;
        }
        if (sim != null) {
            sim.inicializar();
            sim.executar();
        }
    }

    static void rodarPosicao(int opcao) throws Exception {
        ISimulacao sim = null;
        switch (opcao) {
            case 1: sim = new SequencialPosicao(); break;
            case 2: sim = new ParaleloPosicaoStream(); break;
            case 3: sim = new ParaleloPosicaoExecutor(); break;
            case 4: sim = new ParaleloPosicaoCyclicBarrier(); break;
            case 5:
                avisoDistribuido("RMI Posicao");
                MasterPosicao.main(new String[]{"2", "1"});
                return;
            case 6:
                avisoDistribuido("Socket Posicao");
                distribuido.posicao.socket.Master.main(new String[]{"2", "1"});
                return;
        }
        if (sim != null) {
            sim.inicializar();
            sim.executar();
        }
    }

    static void avisoDistribuido(String nome) throws Exception {
        System.out.println("Iniciar 2 Slaves antes.");
        System.out.println("Pressione ENTER se os Slaves já estiverem rodando...");
        new java.util.Scanner(System.in).nextLine();
    }
}