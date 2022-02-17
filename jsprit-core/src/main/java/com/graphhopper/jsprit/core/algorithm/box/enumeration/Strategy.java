package com.graphhopper.jsprit.core.algorithm.box.enumeration;

public enum Strategy {

    RADIAL_BEST("radial_best"),
    RADIAL_REGRET("radial_regret"),
    RANDOM_BEST("random_best"),
    RANDOM_REGRET("random_regret"),
    WORST_BEST("worst_best"),
    WORST_REGRET("worst_regret"),
    CLUSTER_BEST("cluster_best"),
    CLUSTER_REGRET("cluster_regret"),
    STRING_BEST("string_best"),
    STRING_REGRET("string_regret");

    String strategyName;

    Strategy(String strategyName) {
        this.strategyName = strategyName;
    }

    public String toString() {
        return strategyName;
    }
}
