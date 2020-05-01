import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;

public class BKPostConstraintsPrograming {
    int K = 2; // number of posters
    int N = 10; // number of clients

    int timeService[];
    ArrayList<BKPostLocalSearch.Point2D> points;

    Model model;
    IntVar X[];
    IntVar router[];
    IntVar times[];
    IntVar obj;

    public BKPostConstraintsPrograming(int k, DatasetContraintsPrograming dataset) {
        K = k;
        timeService = dataset.getDemandInt();
        points = dataset.getPoints();
        N = dataset.getN();
        int bound_max = 0;


        for (BKPostLocalSearch.Point2D p : points) {
            for (BKPostLocalSearch.Point2D q : points)
                bound_max += (int) BKPostLocalSearch.Point2D.distance(p, q) / 2;
        }

        model = new Model("BK Post Constraints Programing");
        //Các điểm N+1 --> N+2K được thêm vào làm điểm bắt đầu và kết thúc của lộ tình

        // X[i] là điểm tiếp theo của i trên lộ trình
        X = model.intVarArray("Next Node", N + K, 1, N + 2 * K);

        // chỉ số của router qua điểm i
        router = model.intVarArray("Router of Node", N + 2 * K, 1, K);

        // thời gian tích lũy của router chứa i khi đến i
        times = model.intVarArray("Time Of Node", N + 2 * K, 0, bound_max);

        // add constraints

        //times[s] = 0, s = N+1, ... N+K
        for (int s = N + 1; s <= N + K; s++) {
            model.arithm(times[s - 1], "=", 0).post();
        }

        // Cặp điểm bắt đầu và kết thúc có cùng router và được gán mặc định ban đầu
        for (int i = 0; i < K; i++) {
            model.arithm(router[N + i], "=", router[N + K + i]).post();
            model.arithm(router[N + i], "=", i + 1).post();
        }

        // X[i] != X[j] với mọi i, j
        model.allDifferent(X).post();

        // X[i] != N+k, i < N, k = 1,.., K
        for (int i = 0; i < N; i++) {
            for (int j = N + 1; j <= N + K; j++)
                model.arithm(X[i], "!=", j).post();
        }

        // X[i] != i+1 , vd: điểm 1 phải có điểm tiếp theo khác 1
        for (int i = 0; i < N + K; i++)
            model.arithm(X[i], "!=", i + 1).post();

        // router[i] = router[X[i]]
        for (int i = 0; i < N + K; i++) {
            model.element(router[i], router, model.intOffsetView(X[i], -1), 0).post();
        }

        //X[i] = j --> time[j] = times[i] + timeMoving[i][j] + d[j]
        for (int i = 1; i <= N + K; i++) {
            for (int j = 1; j <= N + 2 * K; j++) {
                int timeCost;
                if (i <= N && j <= N) {
                    timeCost = (int) BKPostLocalSearch.Point2D.distance(points.get(i), points.get(j)) + timeService[j];
                } else if (i > N && j <= N) {
                    timeCost = (int) BKPostLocalSearch.Point2D.distance(points.get(0), points.get(j)) + timeService[j];
                } else timeCost = 0;

                model.ifThen(
                        model.arithm(X[i - 1], "=", j),
                        model.arithm(times[j - 1], "=",
                                model.intOffsetView(times[i - 1], timeCost)));
            }
        }

        obj = model.intVar("Time Objecttive", 0, bound_max);
        model.max(obj, times).post();

        model.setObjective(false, obj);
    }

    public void search() {
        Solver solver = model.getSolver();
        while (solver.solve()) {
            System.out.print("Next : ");
            for (int i = 0; i < N + K; i++) {
                System.out.print((i + 1) + "(" + X[i].getValue() + "), ");
            }
            System.out.print("\nRouter ");
            for (int i = 0; i < N + 2 * K; i++) {
                System.out.print((i + 1) + "(" + router[i].getValue() + "), ");
            }
            System.out.println(" Objective = " + obj.getValue() + "\n");
        }
    }

    public static void main(String[] args) {
        DatasetContraintsPrograming dataset = new DatasetContraintsPrograming("./Dataset Local Search/data_100");
        BKPostConstraintsPrograming app = new BKPostConstraintsPrograming(5, dataset);
        app.search();
    }
}
