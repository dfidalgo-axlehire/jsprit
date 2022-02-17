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
package com.graphhopper.jsprit.core.algorithm.listener.parallel;

import com.graphhopper.jsprit.core.algorithm.ParallelVehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.SearchStrategy;

import com.graphhopper.jsprit.core.algorithm.listener.PrioritizedVRAListener;
import com.graphhopper.jsprit.core.algorithm.listener.Priority;
import com.graphhopper.jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListener;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import org.redisson.api.RList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;


public class ParallelVehicleRoutingAlgorithmListeners {

    private TreeSet<PrioritizedVRAListener> algorithmListeners = new TreeSet<>((o1, o2) -> {
        if (o1 == o2) {
            return 0;
        }
        if (o1.getPriority() == Priority.HIGH && o2.getPriority() != Priority.HIGH) {
            return -1;
        } else if (o2.getPriority() == Priority.HIGH && o1.getPriority() != Priority.HIGH) {
            return 1;
        } else if (o1.getPriority() == Priority.MEDIUM && o2.getPriority() != Priority.MEDIUM) {
            return -1;
        } else if (o2.getPriority() == Priority.MEDIUM && o1.getPriority() != Priority.MEDIUM) {
            return 1;
        }
        return 1;
    });


    public Collection<VehicleRoutingAlgorithmListener> getAlgorithmListeners() {
        List<VehicleRoutingAlgorithmListener> list = new ArrayList<>();
        for (PrioritizedVRAListener l : algorithmListeners) {
            list.add(l.getListener());
        }
        return Collections.unmodifiableCollection(list);
    }

    public void remove(PrioritizedVRAListener listener) {
        boolean removed = algorithmListeners.remove(listener);
        if (!removed) {
            throw new IllegalStateException("cannot remove listener");
        }
    }

    public void addListener(VehicleRoutingAlgorithmListener listener, Priority priority) {
        algorithmListeners.add(new PrioritizedVRAListener(priority, listener));
    }

    public void addListener(VehicleRoutingAlgorithmListener listener) {
        addListener(listener, Priority.LOW);
    }

    public void algorithmEnds(VehicleRoutingProblem problem, RList<VehicleRoutingProblemSolution> solutions) {
        for (PrioritizedVRAListener l : algorithmListeners) {
            if (l.getListener() instanceof ParallelAlgorithmEndsListener) {
                ((ParallelAlgorithmEndsListener) l.getListener()).informAlgorithmEnds(problem, solutions);
            }
        }

    }

    public void iterationEnds(int i, VehicleRoutingProblem problem, RList<VehicleRoutingProblemSolution> solutions) {
        for (PrioritizedVRAListener l : algorithmListeners) {
            if (l.getListener() instanceof ParallelIterationEndsListener) {
                ((ParallelIterationEndsListener) l.getListener()).informIterationEnds(i, problem, solutions);
            }
        }
    }

    public void iterationStarts(int i, VehicleRoutingProblem problem, RList<VehicleRoutingProblemSolution> solutions) {
        for (PrioritizedVRAListener l : algorithmListeners) {
            if (l.getListener() instanceof ParallelIterationStartsListener) {
                ((ParallelIterationStartsListener) l.getListener()).informIterationStarts(i, problem, solutions);
            }
        }
    }


    public void algorithmStarts(VehicleRoutingProblem problem, ParallelVehicleRoutingAlgorithm algorithm, RList<VehicleRoutingProblemSolution> solutions) {
        for (PrioritizedVRAListener l : algorithmListeners) {
            if (l.getListener() instanceof ParallelAlgorithmStartsListener) {
                ((ParallelAlgorithmStartsListener) l.getListener()).informAlgorithmStarts(problem, algorithm, solutions);
            }
        }
    }

    public void add(PrioritizedVRAListener l) {
        algorithmListeners.add(l);
    }

    public void addAll(Collection<PrioritizedVRAListener> algorithmListeners) {
        for (PrioritizedVRAListener l : algorithmListeners) {
            this.algorithmListeners.add(l);
        }
    }

    public void selectedStrategy(SearchStrategy.DiscoveredSolution discoveredSolution, VehicleRoutingProblem problem, RList<VehicleRoutingProblemSolution> solutions) {
        for (PrioritizedVRAListener l : algorithmListeners) {
            if (l.getListener() instanceof ParallelStrategySelectedListener) {
                ((ParallelStrategySelectedListener) l.getListener()).informSelectedStrategy(discoveredSolution, problem, solutions);
            }
        }
    }
}
