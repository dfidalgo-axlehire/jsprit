package com.graphhopper.jsprit.core.algorithm.listener;

public class PrioritizedVRAListener {

    Priority priority;
    VehicleRoutingAlgorithmListener l;

    public PrioritizedVRAListener(Priority priority, VehicleRoutingAlgorithmListener l) {
        super();
        this.priority = priority;
        this.l = l;
    }

    public Priority getPriority() {
        return priority;
    }

    public VehicleRoutingAlgorithmListener getListener() {
        return l;
    }

}
