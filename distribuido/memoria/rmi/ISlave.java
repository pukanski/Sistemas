package distribuido.memoria.rmi;

import model.VeiculoMemoria;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ISlave extends Remote {
    // recebe a estrada e devolve com os novos segmentos calculados
    VeiculoMemoria[] calcularSegmento(VeiculoMemoria[] estradaAtual, int inicio, int fim) throws RemoteException;
}