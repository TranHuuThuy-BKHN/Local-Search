import localsearch.domainspecific.vehiclerouting.vrp.IFunctionVR;
import localsearch.domainspecific.vehiclerouting.vrp.VRManager;
import localsearch.domainspecific.vehiclerouting.vrp.VarRoutesVR;
import localsearch.domainspecific.vehiclerouting.vrp.entities.ArcWeightsManager;
import localsearch.domainspecific.vehiclerouting.vrp.entities.Point;
import localsearch.domainspecific.vehiclerouting.vrp.functions.AccumulatedEdgeWeightsOnPathVR;
import localsearch.domainspecific.vehiclerouting.vrp.functions.MaxVR;
import localsearch.domainspecific.vehiclerouting.vrp.invariants.AccumulatedWeightEdgesVR;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class BKPostLocalSearch {
    public static final String ONE_POINT_MOVE = "One Point Move";
    public static final String TWO_POINTS_MOVE = "Two Points Move";
    public static final String TWO_OPT_MOVE_1 = "Two Opt Move 1";
    public static final String CROSS_EXCHANGE_MOVE = "Cross Exchange Move";

    int K = 2; // number of posters
    int N = 10; // number of clients

    double timeService[];
    ArrayList<Point2D> points;

    ArrayList<Point> starts; // auxillary vertex start --> 0
    ArrayList<Point> ends;   // auxillary vertex end --> time to all == 0
    ArrayList<Point> clients; // N clients
    ArrayList<Point> allPoints;

    VRManager mgr;

    VarRoutesVR routers;  // routers of posters

    ArcWeightsManager awm; // weight of edge --> timeMoving

    HashMap<Point, Integer> mapPoint2ID;

    IFunctionVR obj;
    IFunctionVR[] times;// times[k] is the time moving + time service of router k

    Random R = new Random();


    public BKPostLocalSearch(int k, DatasetLocalSearch dataset) {
        K = k;
        N = dataset.getN();
        this.timeService = dataset.getDemand();
        this.points = dataset.getPoints();
    }

    public void mapping() {
        starts = new ArrayList<>();
        ends = new ArrayList<>();
        clients = new ArrayList<>();
        allPoints = new ArrayList<>();
        mapPoint2ID = new HashMap<>();

        // generate start point and end point of each router
        for (int i = 0; i < K; i++) {
            Point s = new Point(0);
            Point e = new Point(N + i + 1);

            starts.add(s);
            ends.add(e);
            allPoints.add(s);
            allPoints.add(e);
            mapPoint2ID.put(s, 0);
            mapPoint2ID.put(e, N + i + 1);
        }

        // generate clients
        for (int i = 1; i <= N; i++) {
            Point c = new Point(i);
            clients.add(c);
            allPoints.add(c);
            mapPoint2ID.put(c, i);
        }

        awm = new ArcWeightsManager(allPoints);

        for (Point p : allPoints) {
            for (Point q : allPoints) {
                if (p.ID > N || q.ID > N)
                    awm.setWeight(p, q, 0);
                else
                    awm.setWeight(p, q, Point2D.distance(points.get(mapPoint2ID.get(p)), points.get(mapPoint2ID.get(q))) + timeService[mapPoint2ID.get(q)]);
            }
        }

    }

    public void stateModel() {
        mgr = new VRManager();
        routers = new VarRoutesVR(mgr);

        // start and end  of router
        for (int i = 0; i < starts.size(); i++) {
            routers.addRoute(starts.get(i), ends.get(i));
        }

        // client of all router
        for (Point c : clients)
            routers.addClientPoint(c);

        // time to all client
        AccumulatedWeightEdgesVR aew = new AccumulatedWeightEdgesVR(routers, awm);


        // time of moving to end point
        times = new IFunctionVR[K];

        for (int i = 1; i <= K; i++) {
            times[i - 1] = new AccumulatedEdgeWeightsOnPathVR(aew, routers.endPoint(i));
        }

        obj = new MaxVR(times);

        mgr.close();
    }

    public void initSolution() {
        ArrayList<Point> listPoints = new ArrayList<>();
        for (int k = 1; k <= routers.getNbRoutes(); k++) {
            listPoints.add(routers.startPoint(k));
        }
        for (Point p : clients) {
            Point x = listPoints.get(R.nextInt(listPoints.size()));
            mgr.performAddOnePoint(p, x);
            listPoints.add(p);
        }
    }


    public void exploreNeighborhoodOnePointMove(ArrayList<Move> cand) {
        cand.clear();
        double minDelta = Double.MAX_VALUE;

        for (int k = 1; k <= K; k++) {
            for (Point y = routers.startPoint(k); y != routers.endPoint(k); y = routers.next(y)) {
                for (Point x : clients) {
                    if (x == y || x == routers.next(y)) continue;

                    double deltaObj = obj.evaluateOnePointMove(x, y);

                    if (deltaObj < minDelta) {
                        minDelta = deltaObj;
                        cand.clear();
                        cand.add(new Move(x, y));
                    } else if (deltaObj == minDelta) {
                        cand.add(new Move(x, y));
                    }
                }
            }
        }
    }

    public void exploreNeighborhoodTowPointsMove(ArrayList<Move> cand) {
        cand.clear();
        double minDelta = Double.MAX_VALUE;
        for (int k = 1; k <= K; k++) {
            for (Point y = routers.next(routers.startPoint(k)); y != routers.endPoint(k); y = routers.next(y)) {
                for (Point x = routers.next(y); x != routers.endPoint(k); x = routers.next(x)) {
                    if (x == routers.next(y)) continue;

                    double deltaObj = obj.evaluateTwoPointsMove(x, y);

                    if (deltaObj < minDelta) {
                        minDelta = deltaObj;
                        cand.clear();
                        cand.add(new Move(x, y));
                    } else if (deltaObj == minDelta) {
                        cand.add(new Move(x, y));
                    }
                }
            }
        }
    }

    public void exploreNeighborhoodTowOptMove(ArrayList<Move> cand) {
        cand.clear();
        double minDelta = Double.MAX_VALUE;
        for (int k1 = 1; k1 < K; k1++) {
            for (int k2 = k1 + 1; k2 <= K; k2++) {
                for (Point y = routers.startPoint(k1); y != routers.endPoint(k1); y = routers.next(y)) {
                    for (Point x = routers.startPoint(k2); x != routers.endPoint(k2); x = routers.next(x)) {
                        if (x == routers.startPoint(k2) || y == routers.startPoint(k1)) continue;

                        double deltaObj = obj.evaluateTwoOptMove1(x, y);

                        if (deltaObj < minDelta) {
                            minDelta = deltaObj;
                            cand.clear();
                            cand.add(new Move(x, y));
                        } else if (deltaObj == minDelta) {
                            cand.add(new Move(x, y));
                        }
                    }
                }
            }
        }
    }

    public void exploreNeighborhoodCrossExchangeMove(ArrayList<Move> cand) {
        cand.clear();
        double minDelta = Double.MAX_VALUE;

        for (int k1 = 1; k1 < K; k1++) {
            for (int k2 = k1 + 1; k2 <= K; k2++) {
                for (Point y1 = routers.startPoint(k1); y1 != routers.endPoint(k1); y1 = routers.next(y1)) {
                    for (Point x1 = routers.startPoint(k1); x1 != y1; x1 = routers.next(x1)) {
                        for (Point y2 = routers.startPoint(k2); y2 != routers.endPoint(k2); y2 = routers.next(y2)) {
                            for (Point x2 = routers.startPoint(k2); x2 != y2; x2 = routers.next(x2)) {
                                double deltaObj = obj.evaluateCrossExchangeMove(x1, y1, x2, y2);

                                if (deltaObj < minDelta) {
                                    minDelta = deltaObj;
                                    cand.clear();
                                    cand.add(new Move(x1, y1, x2, y2));
                                } else if (deltaObj == minDelta) {
                                    cand.add(new Move(x1, y1, x2, y2));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void search(int loop, final String TYPE) {
        initSolution();
        int i = 0;
        ArrayList<Move> cand = new ArrayList<>();
        Move m;

        while (i++ < loop) {
            switch (TYPE) {
                case ONE_POINT_MOVE:
                    exploreNeighborhoodOnePointMove(cand);
                    if (cand.size() == 0) {
                        System.out.println("Local Optimazation");
                        break;
                    }
                    m = cand.get(R.nextInt(cand.size()));
                    mgr.performOnePointMove(m.x, m.y);
                    break;

                case TWO_POINTS_MOVE:
                    exploreNeighborhoodTowPointsMove(cand);
                    if (cand.size() == 0) {
                        System.out.println("Local Optimazation");
                        break;
                    }
                    m = cand.get(R.nextInt(cand.size()));
                    mgr.performTwoPointsMove(m.x, m.y);
                    break;

                case TWO_OPT_MOVE_1:
                    exploreNeighborhoodTowOptMove(cand);
                    if (cand.size() == 0) {
                        System.out.println("Local Optimazation");
                        break;
                    }
                    m = cand.get(R.nextInt(cand.size()));
                    mgr.performTwoOptMove1(m.x, m.y);
                    break;

                case CROSS_EXCHANGE_MOVE:
                    exploreNeighborhoodCrossExchangeMove(cand);
                    if (cand.size() == 0) {
                        System.out.println("Local Optimazation");
                        break;
                    }
                    m = cand.get(R.nextInt(cand.size()));
                    mgr.performCrossExchangeMove(m.x, m.y, m.x2, m.y2);
                    break;

                default:
                    System.err.println(TYPE + " not define");
                    return;
            }

            System.out.println("Step " + i + ", objective = " + obj.getValue());

        }
        System.out.println(routers.toString());
        for (int k = 1; k <= K; k++) {
            System.out.println("Time of router " + k + " = " + times[k - 1].getValue());
        }

        //draw GUI
        GuiBKPost gui = new GuiBKPost("Gui BK Post Local Search", new Dimension(600, 600));
        ArrayList<ArrayList<Integer>> roadmaps = new ArrayList<>();
        roadmaps.clear();
        for (int k = 1; k <= K; k++) {
            ArrayList<Integer> roadmap = new ArrayList<>();
            for (Point p = routers.startPoint(k); p != routers.endPoint(k); p = routers.next(p)) {
                roadmap.add(p.ID);
            }
            roadmaps.add(roadmap);
        }
        gui.drawRouter(points, roadmaps);
    }


    class Move {
        Point x;
        Point y;
        Point x2;
        Point y2;

        public Move(Point x, Point y) {
            this.x = x;
            this.y = y;
        }

        public Move(Point x, Point y, Point x2, Point y2) {
            this.x = x;
            this.y = y;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    static class Point2D {
        double x;
        double y;

        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public static double distance(Point2D a, Point2D b) {
            return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
        }
    }

    public static void main(String[] args) {
        DatasetLocalSearch dataset = new DatasetLocalSearch("./Dataset Local Search/data_am_20");
        BKPostLocalSearch app = new BKPostLocalSearch(5, dataset);

        app.mapping();
        app.stateModel();
        app.search(1000, ONE_POINT_MOVE);
    }

}


