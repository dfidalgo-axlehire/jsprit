package com.graphhopper.jsprit.core.algorithm.box;

public enum Construction {

    BEST_INSERTION("best_insertion"), REGRET_INSERTION("regret_insertion");

    String name;

    Construction(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

}
