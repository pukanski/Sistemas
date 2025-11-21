package distribuido.memoria.rmi;

import core.Config;
import model.VeiculoMemoria;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import java.rmi.RemoteException;

public class Slave implements ISlave {
    private final Random random = new Random();

    @Override
    public VeiculoMemoria[] calcularSegmento(VeiculoMemoria[] estradaAtual, int inicio, int fim) throws RemoteException {
        // o array pra retornar
        VeiculoMemoria[] proxima = new VeiculoMemoria[Config.L];

        for (int i = inicio; i < fim; i++) {
            VeiculoMemoria carro = estradaAtual[i];
            if (carro != null) {
                int v = carro.velocidade;

                if (v < Config.V_MAX) v++;

                int dist = 0;
                for (int k = 1; k < Config.L; k++) {
                    dist++;
                    if (estradaAtual[(i + k) % Config.L] != null) break;
                }

                v = Math.min(v, dist - 1);

                if (random.nextDouble() < Config.PROBABILIDADE && v > 0) v--;

                carro.velocidade = v;

                // escreve nos resultados
                proxima[(i + v) % Config.L] = carro;
            }
        }
        return proxima;
    }

    public static void main(String[] args) {
        try {
            // nome do slave por args
            String nome = (args.length > 0) ? args[0] : "Slave1";

            // exporta o objeto com uma porta anonima
            Slave obj = new Slave();
            ISlave stub = (ISlave) UnicastRemoteObject.exportObject(obj, 0);

            // registro rmi rodando na porta padrao
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(nome, stub);

            System.out.println(">>> [RMI] Servidor '" + nome + "' aguardando...");

        } catch (Exception e) {
            System.err.println("Erro no Slave: " + e.toString());
        }
    }
}