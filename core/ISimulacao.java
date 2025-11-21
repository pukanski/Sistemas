package core;

public interface ISimulacao {
    // monta a estrada
    void inicializar();

    // executa a simulacao de trafego
    void executar() throws Exception;

    // nome da classe pro benchmark
    String getNome();
}