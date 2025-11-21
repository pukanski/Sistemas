package distribuido.posicao.rmi;

import model.VeiculoPosicao;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ISlave extends Remote {
    // recebe a estrada e os veiculos pra atualizar
    List<VeiculoPosicao> calcularVelocidades(VeiculoPosicao[] estradaCompleta, List<VeiculoPosicao> meusVeiculos) throws RemoteException;
}