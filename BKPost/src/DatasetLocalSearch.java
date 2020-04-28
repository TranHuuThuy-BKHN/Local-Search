import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class DatasetLocalSearch {

    protected String root;
    protected ArrayList<BKPostLocalSearch.Point2D> points;
    protected double demand[];
    protected int N;

    public DatasetLocalSearch(String root) {
        this.root = root;
        points = new ArrayList<>();
        points.add(new BKPostLocalSearch.Point2D(0, 0));

        try {
            System.setIn(new FileInputStream(root));
        } catch (FileNotFoundException e) {
            System.err.println("File " + root + " not found");
        }
        Scanner sc = new Scanner(System.in);
        N = Integer.parseInt(sc.nextLine());
        for (int i = 0; i < N; i++) {
            String number[] = sc.nextLine().split(",");
            double x = Double.parseDouble(number[0].substring(1));
            double y = Double.parseDouble(number[1].substring(1, number[1].length() - 1));
            points.add(new BKPostLocalSearch.Point2D(x, y));
        }
        sc.nextLine();
        demand = new double[N + 1];
        demand[0] = 0.0;
        for (int i = 1; i < N + 1; i++) {
            demand[i] = Double.parseDouble(sc.nextLine());
        }
    }

    public ArrayList<BKPostLocalSearch.Point2D> getPoints() {
        return points;
    }

    public double[] getDemand() {
        return demand;
    }

    public int getN() {
        return N;
    }

    public static void main(String[] args) {
        DatasetLocalSearch dataset = new DatasetLocalSearch("./Dataset Local Search/data_10");
    }
}
