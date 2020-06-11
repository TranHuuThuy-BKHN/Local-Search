import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

public class ChocoElement {
    public static void main(String[] args) {
        Model model = new Model("Element Constraint In Choco");

        IntVar X[] = model.intVarArray("X", 8, 1, 10);
        IntVar routers[] = model.intVarArray("Y", 10, 1, 2);
        IntVar Y[] = model.intVarArray("Z", 10, 0, 1);

        model.arithm(routers[8], "=", 1).post();
        model.arithm(routers[9], "=", 2).post();
        model.arithm(routers[6], "=", 1).post();
        model.arithm(routers[7], "=", 2).post();

        for (int i = 0; i < 8; i++)
            model.element(routers[i], routers, model.intOffsetView(X[i], -1), 0).post();

        model.allDifferent(X).post();

        for(int i = 0; i < 10; i++){
            model.ifThenElse(
                    model.arithm(routers[i], "=", 1),
                    model.arithm(Y[i], "=", 1),
                    model.arithm(Y[i], "=", 0)
            );
        }

        model.sum(Y, "=", 5).post();

        model.getSolver().solve();

        for (int i = 0; i < X.length; i++) {
            System.out.print(X[i].getValue() + " ");
        }
        System.out.println();
        for (int i = 0; i < routers.length; i++) {
            System.out.print(routers[i].getValue() + " ");
        }
    }
}
