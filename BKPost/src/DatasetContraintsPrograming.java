public class DatasetContraintsPrograming extends DatasetLocalSearch {

    private int demandInt[];

    public DatasetContraintsPrograming(String root) {
        super(root);
        demandInt = new int[getN() + 1];
        for (int i = 0; i < demandInt.length; i++) {
            demandInt[i] = (int) getDemand()[i];
        }
    }

    public int[] getDemandInt() {
        return this.demandInt;
    }
}
