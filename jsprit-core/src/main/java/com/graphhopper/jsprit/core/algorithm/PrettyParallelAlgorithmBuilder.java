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

import com.graphhopper.jsprit.core.algorithm.acceptor.SchrimpfAcceptance;
import com.graphhopper.jsprit.core.algorithm.acceptor.SolutionAcceptor;
import com.graphhopper.jsprit.core.algorithm.listener.parallel.ParallelAlgorithmStartsListener;
import com.graphhopper.jsprit.core.algorithm.recreate.InsertionStrategy;
import com.graphhopper.jsprit.core.algorithm.recreate.VehicleSwitched;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleFleetManager;
import org.redisson.api.RedissonClient;

import java.util.Random;

/**
 * Created by schroeder on 10.12.14.
 */
public class PrettyParallelAlgorithmBuilder {

    private final VehicleRoutingProblem vrp;

    private final VehicleFleetManager fleetManager;

    private final StateManager stateManager;

    private final ConstraintManager constraintManager;

    private SearchStrategyManager searchStrategyManager;

    private InsertionStrategy iniInsertionStrategy;

    private SolutionCostCalculator iniObjFunction;

    private boolean coreStuff = false;

    private SolutionCostCalculator objectiveFunction = null;

    public static PrettyParallelAlgorithmBuilder newInstance(VehicleRoutingProblem vrp, VehicleFleetManager fleetManager, StateManager stateManager, ConstraintManager constraintManager) {
        return new PrettyParallelAlgorithmBuilder(vrp, fleetManager, stateManager, constraintManager);
    }

    PrettyParallelAlgorithmBuilder(VehicleRoutingProblem vrp, VehicleFleetManager fleetManager, StateManager stateManager, ConstraintManager constraintManager) {
        this.vrp = vrp;
        this.fleetManager = fleetManager;
        this.stateManager = stateManager;
        this.constraintManager = constraintManager;
        this.searchStrategyManager = new SearchStrategyManager();
    }

    public PrettyParallelAlgorithmBuilder setRandom(Random random) {
        searchStrategyManager.setRandom(random);
        return this;
    }

    public PrettyParallelAlgorithmBuilder withStrategy(SearchStrategy strategy, double weight) {
        searchStrategyManager.addStrategy(strategy, weight);
        return this;
    }

    public PrettyParallelAlgorithmBuilder constructInitialSolutionWith(InsertionStrategy insertionStrategy, SolutionCostCalculator objFunction) {
        this.iniInsertionStrategy = insertionStrategy;
        this.iniObjFunction = objFunction;
        return this;
    }

    public ParallelVehicleRoutingAlgorithm build(String id, RedissonClient redisson) {
        if (coreStuff) {
            AlgorithmUtil.addCoreConstraints(constraintManager,stateManager,vrp);
        }
        ParallelVehicleRoutingAlgorithm vra = new ParallelVehicleRoutingAlgorithm(vrp, searchStrategyManager, objectiveFunction, id, redisson);
        vra.addListener(stateManager);
        RemoveEmptyVehicles removeEmptyVehicles = new RemoveEmptyVehicles(fleetManager);
        ResetAndIniFleetManager resetAndIniFleetManager = new ResetAndIniFleetManager(fleetManager);
        VehicleSwitched vehicleSwitched = new VehicleSwitched(fleetManager);
        vra.addListener(removeEmptyVehicles);
        vra.addListener(resetAndIniFleetManager);
        vra.addListener(vehicleSwitched);
        if (iniInsertionStrategy != null) {
            if (!iniInsertionStrategy.getListeners().contains(removeEmptyVehicles))
                iniInsertionStrategy.addListener(removeEmptyVehicles);
            if (!iniInsertionStrategy.getListeners().contains(resetAndIniFleetManager))
                iniInsertionStrategy.addListener(resetAndIniFleetManager);
            if (!iniInsertionStrategy.getListeners().contains(vehicleSwitched))
                iniInsertionStrategy.addListener(vehicleSwitched);
            if (!iniInsertionStrategy.getListeners().contains(stateManager))
                iniInsertionStrategy.addListener(stateManager);
            vra.addListener((ParallelAlgorithmStartsListener) (problem, algorithm, solutions) -> {
                if (solutions.isEmpty()) {
                    solutions.add(new InsertionInitialSolutionFactory(iniInsertionStrategy, iniObjFunction).createSolution(vrp));
                }
            });
        }
        addArbitraryListener(vra);
        return vra;
    }

    private void addArbitraryListener(ParallelVehicleRoutingAlgorithm vra) {
        searchSchrimpfAndRegister(vra);
    }

    private void searchSchrimpfAndRegister(ParallelVehicleRoutingAlgorithm vra) {
        boolean schrimpfAdded = false;
        for (SearchStrategy strategy : vra.getSearchStrategyManager().getStrategies()) {
            SolutionAcceptor acceptor = strategy.getSolutionAcceptor();
            if (acceptor instanceof SchrimpfAcceptance) {
                if (!schrimpfAdded) {
                    vra.addListener((SchrimpfAcceptance) acceptor);
                    schrimpfAdded = true;
                }
            }
        }
    }

    public PrettyParallelAlgorithmBuilder addCoreStateAndConstraintStuff() {
        this.coreStuff = true;
        return this;
    }


    public PrettyParallelAlgorithmBuilder withObjectiveFunction(SolutionCostCalculator objectiveFunction) {
        this.objectiveFunction = objectiveFunction;
        return this;
    }
}
