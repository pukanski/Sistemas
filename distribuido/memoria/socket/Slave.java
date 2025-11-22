package distribuido.memoria.socket;

import core.Config;
import model.VeiculoMemoria;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class Slave {
    static final int PORTA_BASE = 50000;

    public static void main(String[] args) {
        // define o id do slave pelo args
        int id = (args.length > 0) ? Integer.parseInt(args[0]) : 1;
        int porta = PORTA_BASE + (id - 1);
        System.out.println(">>> [Socket Memoria] Slave " + id + " ouvindo na porta " + porta);

        try (ServerSocket server = new ServerSocket(porta)) {
            while (true) {
                Socket client = server.accept(); // bloqueia a conexao ate o master se conectar
                tratarConexao(client); // executa a conexao
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void tratarConexao(Socket socket) {
        try {
            // reduz a latencia
            socket.setTcpNoDelay(true);

            // buffering
            try (
                    ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()))
            ) {
                // envia o cabeçalho de serializacao
                out.flush();

                Random random = new Random();

                while (true) {
                    // le a estrada
                    int inicio = in.readInt();
                    int fim = in.readInt();
                    VeiculoMemoria[] estrada = (VeiculoMemoria[]) in.readObject();

                    // array de resposta
                    VeiculoMemoria[] resposta = new VeiculoMemoria[Config.L];

                    // calcula as velocidades
                    for (int i = inicio; i < fim; i++) {
                        VeiculoMemoria v = estrada[i];
                        if (v != null) {
                            int vel = v.velocidade;
                            if (vel < Config.V_MAX) vel++;

                            int dist = 0;
                            for (int k = 1; k < Config.L; k++) {
                                dist++;
                                if (estrada[(i + k) % Config.L] != null) break;
                            }
                            vel = Math.min(vel, dist - 1);

                            if (random.nextDouble() < Config.PROBABILIDADE && vel > 0) vel--;
                            v.velocidade = vel;
                            resposta[(i + vel) % Config.L] = v;
                        }
                    }

                    out.writeObject(resposta); // manda o array de resposta
                    out.flush(); // envia o buffer acumulado pela rede
                    out.reset(); // limpa a tabela de referencia
                }
            }
        } catch (EOFException e) {
            System.out.println("Master desconectou.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}