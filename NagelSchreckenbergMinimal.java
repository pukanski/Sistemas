package trafego;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class NagelSchreckenbergMinimal {

    // Representa um carro com posição e velocidade
    static class Car {
        int position;
        int velocity;

        Car(int position, int velocity) {
            this.position = position;
            this.velocity = velocity;
        }

        void move(int roadLength) {
            position = (position + velocity) % roadLength; // estrada circular
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // ===== Parâmetros =====
        int roadLength = 100;     // comprimento da estrada
        int numCars = 20;         // número de carros
        int vmax = 5;             // velocidade máxima
        double slowdownProb = 0.3; // probabilidade de desaceleração aleatória
        int steps = 200;          // número de iterações
        int delayMs = 100;        // tempo entre frames (ms)

        Random random = new Random();

        // ===== Inicialização da estrada =====
        Car[] road = new Car[roadLength];
        List<Car> cars = new ArrayList<>();

        // Gera posições iniciais únicas
        Set<Integer> positions = new HashSet<>();
        while (positions.size() < numCars) {
            positions.add(random.nextInt(roadLength));
        }

        // Cria os carros
        for (int pos : positions) {
            Car car = new Car(pos, random.nextInt(vmax + 1));
            cars.add(car);
            road[pos] = car;
        }

        // ===== Loop da simulação =====
        for (int step = 0; step < steps; step++) {

            // (1) Atualizar velocidades em paralelo
            cars.parallelStream().forEach(car -> {
                int v = car.velocity;

                // Aceleração
                if (v < vmax) v++;

                // Distância até o próximo carro
                int d = 1;
                while (d <= vmax) {
                    int ahead = (car.position + d) % roadLength;
                    if (road[ahead] != null) break;
                    d++;
                }

                // Evitar colisão
                v = Math.min(v, d - 1);

                // Aleatoriedade
                if (random.nextDouble() < slowdownProb)
                    v = Math.max(v - 1, 0);

                car.velocity = v;
            });

            // (2) Limpa a estrada
            Arrays.fill(road, null);

            // (3) Move carros
            cars.parallelStream().forEach(car -> car.move(roadLength));

            // (4) Reposiciona carros na estrada
            for (Car car : cars) {
                road[car.position] = car;
            }

            // (5) Imprime estado da estrada
            System.out.print("\033[H\033[2J"); // limpa tela
            System.out.flush();
            printRoad(road);

            Thread.sleep(delayMs);
        }
    }

    // ===== Função utilitária para mostrar a estrada =====
    static void printRoad(Car[] road) {
        StringBuilder sb = new StringBuilder(road.length);
        for (Car c : road) {
            if (c == null) sb.append('.');
            else sb.append(c.velocity);
        }
        System.out.println(sb.toString());
    }
}
