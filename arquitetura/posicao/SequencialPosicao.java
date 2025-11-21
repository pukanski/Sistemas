package arquitetura.posicao;

import core.Config;
import core.ISimulacao;
import model.VeiculoPosicao;

import java.util.*;

public class SequencialPosicao implements ISimulacao {
    private VeiculoPosicao[] estrada;
    private List<VeiculoPosicao> veiculosList;
    private Random random;

    // nome pra tabela
    @Override
    public String getNome() {
        return "Sequencial (Posicao)";
    }

    // monta a estrada
    @Override
    public void inicializar() {
        this.estrada = new VeiculoPosicao[Config.L];
        this.veiculosList = new ArrayList<>();
        this.random = new Random();

        // garante que cada carro tenha uma posicao unica na estrda
        Set<Integer> posicoes = new HashSet<>();
        while (posicoes.size() < Config.NUM_VEICULOS) {
            posicoes.add(random.nextInt(Config.L));
        }

        // cria os veiculoos
        for (int pos : posicoes) {
            VeiculoPosicao v = new VeiculoPosicao(pos, random.nextInt(Config.V_MAX + 1));
            this.veiculosList.add(v);
            this.estrada[pos] = v;
        }
    }

    // roda a simulacao
    @Override
    public void executar() {
        for (int step = 0; step < Config.STEPS; step++) {

            // calculo das velocidades
            for (VeiculoPosicao v : veiculosList) {
                int vel = v.velocidade;

                // fase 1: acelera
                if (vel < Config.V_MAX) vel++;

                // calcula a distancia pro proximo carro
                int dist = 1;
                while (dist <= Config.V_MAX) {
                    int andar = (v.posicao + dist) % Config.L;
                    if (estrada[andar] != null) break;
                    dist++;
                }

                // fase 2: desacelera
                vel = Math.min(vel, dist - 1);

                // fase 3: randomiza
                if (random.nextDouble() < Config.PROBABILIDADE) {
                    vel = Math.max(vel - 1, 0);
                }

                // fase 4: define a velocidade
                v.velocidade = vel;
            }

            // limpa a estrada
            Arrays.fill(estrada, null);

            // move os veiculos e atualiza a estrada
            for (VeiculoPosicao v : veiculosList) {
                v.andar(Config.L);
                estrada[v.posicao] = v;
            }
        }
    }

    // metodo usado pra inicializacao pra classes paralelas
    public static List<VeiculoPosicao> gerarListaInicial(VeiculoPosicao[] estradaRef) {
        List<VeiculoPosicao> lista = new ArrayList<>();
        Random r = new Random();
        Set<Integer> posicoes = new HashSet<>();
        while (posicoes.size() < Config.NUM_VEICULOS) posicoes.add(r.nextInt(Config.L));
        for (int pos : posicoes) {
            VeiculoPosicao v = new VeiculoPosicao(pos, r.nextInt(Config.V_MAX + 1));
            lista.add(v);
            estradaRef[pos] = v;
        }
        return lista;
    }
}