package com.graphhopper.jsprit.core.algorithm.listener.parallel;

import com.graphhopper.jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListener;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import org.redisson.api.RList;
import org.redisson.api.RSet;

public interface ParallelIterationStartsListener extends VehicleRoutingAlgorithmListener {

    void informIterationStarts(int i, VehicleRoutingProblem problem, RList<VehicleRoutingProblemSolution> solutions);

}
