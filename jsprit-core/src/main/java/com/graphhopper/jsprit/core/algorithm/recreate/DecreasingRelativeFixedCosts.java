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
package com.graphhopper.jsprit.core.algorithm.recreate;

import com.graphhopper.jsprit.core.algorithm.state.InternalStates;
import com.graphhopper.jsprit.core.problem.Capacity;
import com.graphhopper.jsprit.core.problem.constraint.SoftRouteConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.state.RouteAndActivityStateGetter;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;

import java.util.logging.Logger;


public final class DecreasingRelativeFixedCosts extends SolutionCompletenessRatio implements SoftRouteConstraint {

    private static final Logger logger = Logger.getLogger(DecreasingRelativeFixedCosts.class.getName());

    private double weightDeltaFixCost = 0.5;

    private RouteAndActivityStateGetter stateGetter;

    public DecreasingRelativeFixedCosts(RouteAndActivityStateGetter stateGetter, int noJobs) {
        super(noJobs);
        this.stateGetter = stateGetter;
        logger.info(String.format("initialise %s", this));
    }


    public void setWeightOfFixCost(double weight) {
        weightDeltaFixCost = weight;
        logger.info(String.format("set weightOfFixCostSaving to %s", weight));
    }

    @Override
    public String toString() {
        return "[name=DecreasingRelativeFixedCosts][weightOfFixedCostSavings=" + weightDeltaFixCost + "]";
    }

    private Capacity getCurrentMaxLoadInRoute(VehicleRoute route) {
        Capacity maxLoad = stateGetter.getRouteState(route, InternalStates.MAXLOAD, Capacity.class);
        if (maxLoad == null) maxLoad = Capacity.Builder.newInstance().build();
        return maxLoad;
    }

    @Override
    public double getCosts(JobInsertionContext insertionContext) {
        VehicleRoute route = insertionContext.getRoute();
        Capacity currentLoad = getCurrentMaxLoadInRoute(route);
        Capacity load = Capacity.addup(currentLoad, insertionContext.getJob().getSize());
        double currentRelFix = 0d;
        if (route.getVehicle() != null && !(route.getVehicle() instanceof VehicleImpl.NoVehicle)) {
            currentRelFix = route.getVehicle().getType().getVehicleCostParams().fix * Capacity.divide(currentLoad, route.getVehicle().getType().getCapacityDimensions());
        }
        double newRelFix = insertionContext.getNewVehicle().getType().getVehicleCostParams().fix * (Capacity.divide(load, insertionContext.getNewVehicle().getType().getCapacityDimensions()));
        double decreasingRelativeFixedCosts = (1 - solutionCompletenessRatio) * (newRelFix - currentRelFix);
        return weightDeltaFixCost * solutionCompletenessRatio * decreasingRelativeFixedCosts;
    }


}
