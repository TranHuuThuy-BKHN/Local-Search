from ortools.sat.python import cp_model
from ortools.linear_solver import pywraplp
import numpy as np
from math import sqrt 

class Dataset(object):
    def __init__(self, path='./Dataset Local Search/data_10'):
        # read data from path
        self.path = path
        self.points, self.N, self.demand = self.getData(self.path)
    
    # return points 2D, number points and demand of each point
    def getData(self, path):
        points, n, demand = [], 0, []
        # open and read file
        with open(path) as file:
            n = int(file.readline())
            # read points
            for i in range(n):
                line = file.readline().rstrip('\n').split()
                points.append([float(line[0]), float(line[1])])
            file.readline()
            # read demand
            for i in range(n):
                demand.append(float(file.readline().rstrip('\n')))

        return np.array(points), n, np.array(demand)



class PostMIP():
    def __init__(self, dataset, k, M=100):
        self.dataset = dataset
        self.k, self.M = k,M
        self.dataset.points, self.dataset.demand = list(self.dataset.points),list(self.dataset.demand)
        for i in range(2*k):
            self.dataset.points.append([0, 0])
            self.dataset.demand.append(0)
        self.dataset.points = np.array(self.dataset.points)
        self.dataset.demand = np.array(self.dataset.demand)

        #declare MIP solver
        self.solver = pywraplp.Solver("BK Post Mixed Integer Programing", pywraplp.Solver.CBC_MIXED_INTEGER_PROGRAMMING)

        # define variables
        self.X = np.empty((dataset.N + k, dataset.N + 2*k)) # (n+k) x (n+2k) , X[i, j] = 1 if j+1_th point is next point of i+1_th point
        self.router = np.empty((1, dataset.N + 2*k)) # router of each point
        self.times = np.empty((1, dataset.N + 2*k))  # time of each point

    def stateModel(self):
        # init variables
        self.X = np.array([[self.solver.IntVar(0, 1, 'x[{0},{1}]'.format(i,j)) for j in range(self.X.shape[1])] for i in range(self.X.shape[0])])
        self.router = np.array([self.solver.IntVar(1, self.k, 'Router of point {0}'.format(i+1)) for i in range(self.router.shape[1])])
        self.times = np.array([self.solver.NumVar(0.0, self.solver.infinity(), 'Time of point {0}'.format(i+1)) for i in range(self.times.shape[1])])

        # add contraints
        N, k, M = self.dataset.N, self.k, self.M

        # times[N+i] = 0, i = 0,1,..k-1
        for i in range(k):
            self.solver.Add(self.times[N+i] == 0)
        #-----------------------------------------------

        # router[N+i] = router(N+i+k) = i+1 , i = 0, 1, ..k-1
        for i in range(self.k):
            self.solver.Add(self.router[N+i] - self.router[N+i+k] == 0)
            self.solver.Add(self.router[N+i] - i - 1 == 0)
        # -----------------------------------------------

        # sum(X[i, j]) = 1, i=0,1,...,N+k-1 , each point is not end depot, it has next point
        for i in range(N + k):
            self.solver.Add(self.solver.Sum(self.X[i, :]) == 1)
        # -----------------------------------------------------

        # sum(X[i, j]) = 1, j = 0,1,2,...N-1, N+k, ...N+2k-1
        for j in range(N+2*k):
            if j < N or j >= N+k:
                self.solver.Add(self.solver.Sum(self.X[:, j]) == 1)
        # ---------------------------------------------------

        # sum(X[i, j]) = 0 , j = N,N-1, ..., N+k-1, next point of each point is not start depot
        for j in range(N, N+k):
            self.solver.Add(self.solver.Sum(self.X[:, j]) == 0)
        # -----------------------------------------------------

        # X[i, i] = 0 , i = 0,1, ..N+k-1
        for i in range(N+k):
            self.solver.Add(self.X[i,i] == 0)
        
        # X[i, j] = 1 --> router[i] = router[j] , use big M
        for i in range(self.X.shape[0]):
            for j in range(self.X.shape[1]):
                self.solver.Add(self.router[i] + M*(1-self.X[i, j]) >= self.router[j])
                self.solver.Add(self.router[i] - M*(1-self.X[i, j]) <= self.router[j])
        #-----------------------------------------------------------

        # X[i, j] = 1 --> times[j] = times[i] + d[i, j] + demand[j]
        def distance(p1, p2):
            return sqrt((p1[0] - p2[0])**2 + (p1[1] - p2[1])**2)

        for i in range(self.X.shape[0]):
            for j in range(self.X.shape[1]):
                d = 0;
                if j < N: d =  distance(self.dataset.points[i], self.dataset.points[j])
                self.solver.Add(self.times[j] + M*(1-self.X[i, j]) >= self.times[i] + self.dataset.demand[j] +d)
                self.solver.Add(self.times[j] - M*(1-self.X[i, j]) <= self.times[i] + self.dataset.demand[j] + d)
        #-----------------------------------------------------------


        # minimaze max times
        for i in range(N+k+1, N+2*k):
            self.solver.Add(self.times[i] <= self.times[N+k])
        self.solver.Minimize(self.times[N+k])
        
    def search(self):
        print('Number of variables : ',self.solver.NumVariables())
        print('Number of Contraints : ',self.solver.NumConstraints())
        print('MIP is solving...')
        status = self.solver.Solve()
        print('Status :', status)
        if status == pywraplp.Solver.OPTIMAL:
            print("Solution is found")
            for i in range(self.X.shape[0]):
                for j in range(self.X.shape[1]):
                    print(self.X[i,j].solution_value(),end=' ')
                print()
            print('router')
            for r in self.router:
                print(r.solution_value(), end=' ')
            print('time')
            for r in self.times:
                print(r.solution_value(), end=' ')
            print()


dataset = Dataset(path='./Dataset Local Search/data_am_10')
app = PostMIP(dataset, 2)
app.stateModel()
app.search()
