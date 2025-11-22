package distribuido.posicao.socket;

import core.Config;
import model.VeiculoPosicao;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Random;

public class Slave {
    // Mantive a porta 51000 do seu código original
    static final int PORTA_BASE = 51000;

    public static void main(String[] args) {
        // define o id pelo args
        int id = (args.length > 0) ? Integer.parseInt(args[0]) : 1;
        int porta = PORTA_BASE + (id - 1);
        System.out.println(">>> [Socket Posicao] Slave " + id + " ouvindo na porta " + porta);

        try (ServerSocket server = new ServerSocket(porta)) {
            while (true) {
                Socket client = server.accept(); // bloqueia a execucao ate o master entrar
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
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                 ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()))) {

                // envia o cabeçalho de serializacao
                out.flush();

                Random random = new Random();

                while (true) {
                    // le a estrada e a lista de veículos
                    VeiculoPosicao[] estrada = (VeiculoPosicao[]) in.readObject();
                    List<VeiculoPosicao> meusVeiculos = (List<VeiculoPosicao>) in.readObject();

                    // calcula a velocidade
                    for (VeiculoPosicao v : meusVeiculos) {
                        int vel = v.velocidade;
                        if (vel < Config.V_MAX) vel++;

                        int dist = 1;
                        while (dist <= Config.V_MAX) {
                            int andar = (v.posicao + dist) % Config.L;
                            if (estrada[andar] != null) break;
                            dist++;
                        }
                        vel = Math.min(vel, dist - 1);
                        if (random.nextDouble() < Config.PROBABILIDADE) vel = Math.max(vel - 1, 0);
                        v.velocidade = vel;
                    }

                    out.writeObject(meusVeiculos); // envia a lista de veiculos atualizado
                    out.flush(); // envia o buffer
                    out.reset(); // limpa o cache de referencias
                }
            }
        } catch (EOFException e) {
            System.out.println("Master desconectou.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}