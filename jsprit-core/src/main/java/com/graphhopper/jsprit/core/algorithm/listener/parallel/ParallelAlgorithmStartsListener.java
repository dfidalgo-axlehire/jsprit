package com.graphhopper.jsprit.core.algorithm.listener.parallel;

import com.graphhopper.jsprit.core.algorithm.ParallelVehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListener;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import org.redisson.api.RList;
import org.redisson.api.RSet;

public interface ParallelAlgorithmStartsListener extends VehicleRoutingAlgorithmListener {

    void informAlgorithmStarts(VehicleRoutingProblem problem, ParallelVehicleRoutingAlgorithm algorithm, RList<VehicleRoutingProblemSolution> solutions);

}
