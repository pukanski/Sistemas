package distribuido.posicao.rmi;

import core.Config;
import model.VeiculoPosicao;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Random;
import java.rmi.RemoteException;

public class Slave implements ISlave {
    private final Random random = new Random();

    @Override
    public List<VeiculoPosicao> calcularVelocidades(VeiculoPosicao[] estradaCompleta, List<VeiculoPosicao> meusVeiculos) throws RemoteException {
        // calcula a velocidade
        for (VeiculoPosicao v : meusVeiculos) {
            int vel = v.velocidade;

            if (vel < Config.V_MAX) vel++;

            int dist = 1;
            while (dist <= Config.V_MAX) {
                int andar = (v.posicao + dist) % Config.L;
                if (estradaCompleta[andar] != null) break;
                dist++;
            }

            vel = Math.min(vel, dist - 1);

            if (random.nextDouble() < Config.PROBABILIDADE) {
                vel = Math.max(vel - 1, 0);
            }

            v.velocidade = vel;
        }

        return meusVeiculos;
    }

    public static void main(String[] args) {
        try {
            // define o nome do slave pelo args
            String nome = (args.length > 0) ? args[0] : "Slave1";

            Slave obj = new Slave();
            // exporta o objeto com uma porta anonima
            ISlave stub = (ISlave) UnicastRemoteObject.exportObject(obj, 0);
            // registro rmi rodando na porta padrao
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(nome, stub);

            System.out.println(">>> [RMI Posicao] Servidor '" + nome + "' pronto.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}