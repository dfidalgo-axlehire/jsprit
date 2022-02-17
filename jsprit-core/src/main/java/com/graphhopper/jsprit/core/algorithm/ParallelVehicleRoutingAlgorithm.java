/*
 * Licensed to GraphHopper GmbH under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * GraphHopper GmbH licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graphhopper.jsprit.core.algorithm;

import com.graphhopper.jsprit.core.algorithm.SearchStrategy.DiscoveredSolution;
import com.graphhopper.jsprit.core.algorithm.listener.SearchStrategyListener;
import com.graphhopper.jsprit.core.algorithm.listener.SearchStrategyModuleListener;
import com.graphhopper.jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListener;
import com.graphhopper.jsprit.core.algorithm.listener.parallel.ParallelVehicleRoutingAlgorithmListeners;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.util.Solutions;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;


/**
 * Algorithm that solves a {@link VehicleRoutingProblem}.
 *
 * @author stefan schroeder
 */
public class ParallelVehicleRoutingAlgorithm {

    private static class Counter {
        private final String name;
        private long counter = 0;
        private long nextCounter = 1;
        private static final Logger log = LoggerFactory.getLogger(Counter.class);

        public Counter(final String name) {
            this.name = name;
        }

        public void incCounter() {
            long i = ++counter;
            long n = nextCounter;
            if (i >= n) {
                nextCounter = n * 2;
                log.info(this.name + n);
            }
        }

        public void reset() {
            counter = 0;
            nextCounter = 1;
        }
    }

    private final static Logger logger = LoggerFactory.getLogger(ParallelVehicleRoutingAlgorithm.class);

    private final Counter counter = new Counter("iterations ");

    private final VehicleRoutingProblem problem;

    private final SearchStrategyManager searchStrategyManager;

    private final ParallelVehicleRoutingAlgorithmListeners algoListeners = new ParallelVehicleRoutingAlgorithmListeners();

    private final RSet<VehicleRoutingProblemSolution> initialSolutions;

    private int maxIterations = 100;

    private RBucket<VehicleRoutingProblemSolution> bestEver;

    private final SolutionCostCalculator objectiveFunction;

    private RedissonClient redisson;

    private String id;

    public ParallelVehicleRoutingAlgorithm(VehicleRoutingProblem problem,
                                           SearchStrategyManager searchStrategyManager,
                                           String id,
                                           RedissonClient redisson) {
        super();
        this.id = id;
        this.redisson = redisson;
        this.problem = problem;
        this.searchStrategyManager = searchStrategyManager;
        this.initialSolutions = redisson.getSet("solutions_" + id);
        this.bestEver = redisson.getBucket("best_solution" + id);
        this.objectiveFunction = null;
    }

    public ParallelVehicleRoutingAlgorithm(VehicleRoutingProblem problem,
                                           Collection<VehicleRoutingProblemSolution> initialSolutions,
                                           SearchStrategyManager searchStrategyManager,
                                           String id,
                                           RedissonClient redisson) {
        super();
        this.id = id;
        this.redisson = redisson;
        this.problem = problem;
        this.searchStrategyManager = searchStrategyManager;

        this.initialSolutions = redisson.getSet(id);
        this.initialSolutions.addAllAsync(initialSolutions);
        this.bestEver = redisson.getBucket("best_solution" + id);

        objectiveFunction = null;
    }

    public ParallelVehicleRoutingAlgorithm(VehicleRoutingProblem problem,
                                           SearchStrategyManager searchStrategyManager,
                                           SolutionCostCalculator objectiveFunction,
                                           String id,
                                           RedissonClient redisson) {
        super();
        this.id = id;
        this.redisson = redisson;
        this.problem = problem;
        this.searchStrategyManager = searchStrategyManager;
        this.initialSolutions = redisson.getSet(id);
        this.bestEver = redisson.getBucket("best_solution" + id);
        this.objectiveFunction = objectiveFunction;
    }

    /**
     * Adds solution to the collection of initial solutions.
     * <p>
     * This method may lead to errors if tour activities in the solution are different to the
     * ones in the VRP (including differences in indexing)
     *
     * @param solution the solution to be added
     */
    public void addInitialSolution(VehicleRoutingProblemSolution solution) {
        solution = VehicleRoutingProblemSolution.copyOf(solution);
        verifyAndAdaptSolution(solution);
        initialSolutions.addAsync(solution);
    }

    private void verifyAndAdaptSolution(VehicleRoutingProblemSolution solution) {
        Set<Job> jobsNotInSolution = new HashSet<>(problem.getJobs().values());
        jobsNotInSolution.removeAll(solution.getUnassignedJobs());
        for (VehicleRoute route : solution.getRoutes()) {
            jobsNotInSolution.removeAll(route.getTourActivities().getJobs());
            if (route.getVehicle().getIndex() == 0) {
                throw new IllegalStateException(
                    "vehicle used in initial solution has no index. probably a vehicle is used that has not been added to the " +
                        " the VehicleRoutingProblem. only use vehicles that have already been added to the problem.");
            }
            for (TourActivity act : route.getActivities()) {
                if (act.getIndex() == 0) {
                    throw new IllegalStateException(
                        "act in initial solution has no index. activities are created and associated to their job in VehicleRoutingProblem\n." +
                            " thus if you build vehicle-routes use the jobActivityFactory from vehicle routing problem like that \n" +
                            " VehicleRoute.Builder.newInstance(knownVehicle).setJobActivityFactory(vrp.getJobActivityFactory).addService(..)....build() \n" +
                            " then the activities that are created to build the route are identical to the ones used in VehicleRoutingProblem");
                }
            }
        }
        solution.getUnassignedJobs().addAll(jobsNotInSolution);
        solution.setCost(getObjectiveFunction().getCosts(solution));
    }

    /**
     * Gets the {@link SearchStrategyManager}.
     *
     * @return SearchStrategyManager
     */
    public SearchStrategyManager getSearchStrategyManager() {
        return searchStrategyManager;
    }

    /**
     * Runs the vehicle routing algorithm and returns a number of generated solutions.
     * <p>
     * <p>The algorithm runs as long as it is specified in nuOfIterations and prematureBreak. In each iteration it selects a searchStrategy according
     * to searchStrategyManager and runs the strategy to improve solutions.
     * <p>Note that clients are allowed to observe/listen the algorithm. See {@link VehicleRoutingAlgorithmListener} and its according listeners.
     *
     * @return Collection<VehicleRoutingProblemSolution> the solutions
     * @see {@link SearchStrategyManager}, {@link VehicleRoutingAlgorithmListener}, {@link com.graphhopper.jsprit.core.algorithm.listener.AlgorithmStartsListener}, {@link com.graphhopper.jsprit.core.algorithm.listener.AlgorithmEndsListener}, {@link com.graphhopper.jsprit.core.algorithm.listener.IterationStartsListener}, {@link com.graphhopper.jsprit.core.algorithm.listener.IterationEndsListener}
     */
    public Collection<VehicleRoutingProblemSolution> searchSolutions() {
        logger.info("algorithm starts: [maxIterations={}]", maxIterations);
        double now = System.currentTimeMillis();
        int noIterationsThisAlgoIsRunning = maxIterations;
        counter.reset();
        RList<VehicleRoutingProblemSolution> solutions = redisson.getList("internal_solutions_" + id);
        solutions.addAll(initialSolutions);
        algorithmStarts(problem, solutions);
        if(bestEver.get() == null) {
            bestEver.set(Solutions.bestOf(solutions));
        }
        if (logger.isTraceEnabled()) {
            log(solutions);
        }
        logger.info("iterations start");
        for(int i =0; i< maxIterations; i++) {
            iterationStarts(i + 1, problem, solutions);
            logger.debug("start iteration: {}", i);
            counter.incCounter();
            SearchStrategy strategy = searchStrategyManager.getRandomStrategy();
            DiscoveredSolution discoveredSolution = strategy.run(problem, solutions);
            if (logger.isTraceEnabled()) {
                log(discoveredSolution);
            }
            memorizeIfBestEver(discoveredSolution);
            selectedStrategy(discoveredSolution, problem, solutions);
            iterationEnds(i + 1, problem, solutions);
        }
        logger.info("iterations end at {} iterations", noIterationsThisAlgoIsRunning);
        addBestEver(solutions);
        algorithmEnds(problem, solutions);
        logger.info("took {} seconds", ((System.currentTimeMillis() - now) / 1000.0));
        initialSolutions.addAll(solutions);
        return solutions.stream().toList();
    }

    private void addBestEver(Collection<VehicleRoutingProblemSolution> solutions) {
        if (bestEver != null && bestEver.get() != null) {
            solutions.add(bestEver.get());
        }
    }

    private void log(Collection<VehicleRoutingProblemSolution> solutions) {
        for (VehicleRoutingProblemSolution sol : solutions) {
            log(sol);
        }
    }

    private void log(VehicleRoutingProblemSolution solution) {
        logger.trace("solution costs: {}", solution.getCost());
        for (VehicleRoute r : solution.getRoutes()) {
            StringBuilder b = new StringBuilder();
            b.append(r.getVehicle().getId()).append(" : ").append("[ ");
            for (TourActivity act : r.getActivities()) {
                if (act instanceof TourActivity.JobActivity) {
                    b.append(((TourActivity.JobActivity) act).getJob().getId()).append(" ");
                }
            }
            b.append("]");
            logger.trace(b.toString());
        }
        StringBuilder b = new StringBuilder();
        b.append("unassigned : [ ");
        for (Job j : solution.getUnassignedJobs()) {
            b.append(j.getId()).append(" ");
        }
        b.append("]");
        logger.trace(b.toString());
    }

    private void log(DiscoveredSolution discoveredSolution) {
        logger.trace("discovered solution: {}", discoveredSolution);
        log(discoveredSolution.getSolution());
    }

    private void memorizeIfBestEver(DiscoveredSolution discoveredSolution) {
        if (discoveredSolution == null) {
            return;
        }
        if (bestEver.get() == null || discoveredSolution.getSolution().getCost() < bestEver.get().getCost()) {
            bestEver.set(discoveredSolution.getSolution());
        }
    }

    private void selectedStrategy(DiscoveredSolution discoveredSolution,
                                  VehicleRoutingProblem problem,
                                  RList<VehicleRoutingProblemSolution> solutions) {
        algoListeners.selectedStrategy(discoveredSolution, problem, solutions);
    }

    private void algorithmEnds(VehicleRoutingProblem problem, RList<VehicleRoutingProblemSolution> solutions) {
        algoListeners.algorithmEnds(problem, solutions);
    }

    public ParallelVehicleRoutingAlgorithmListeners getAlgorithmListeners() {
        return algoListeners;
    }

    public void addListener(VehicleRoutingAlgorithmListener l) {
        algoListeners.addListener(l);
        if (l instanceof SearchStrategyListener) {
            searchStrategyManager.addSearchStrategyListener((SearchStrategyListener) l);
        }
        if (l instanceof SearchStrategyModuleListener) {
            searchStrategyManager.addSearchStrategyModuleListener((SearchStrategyModuleListener) l);
        }
    }

    private void iterationEnds(int i, VehicleRoutingProblem problem, RList<VehicleRoutingProblemSolution> solutions) {
        algoListeners.iterationEnds(i, problem, solutions);
    }

    private void iterationStarts(int i, VehicleRoutingProblem problem, RList<VehicleRoutingProblemSolution> solutions) {
        algoListeners.iterationStarts(i, problem, solutions);
    }

    private void algorithmStarts(VehicleRoutingProblem problem, RList<VehicleRoutingProblemSolution> solutions) {
        algoListeners.algorithmStarts(problem, this, solutions);
    }

    /**
     * Sets max number of iterations.
     *
     * @param maxIterations max number of iteration the algorithm runs
     */
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        logger.debug("set maxIterations to {}", this.maxIterations);
    }

    /**
     * Gets max number of iterations.
     *
     * @return max number of iterations
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    public SolutionCostCalculator getObjectiveFunction() {
        return objectiveFunction;
    }

}
