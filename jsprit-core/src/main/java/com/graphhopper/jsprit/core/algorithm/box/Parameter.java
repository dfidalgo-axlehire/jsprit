package com.graphhopper.jsprit.core.algorithm.box;

public enum Parameter {

    FIXED_COST_PARAM("fixed_cost_param"), VEHICLE_SWITCH("vehicle_switch"), REGRET_TIME_WINDOW_SCORER("regret.tw_scorer"),
    REGRET_DISTANCE_SCORER("regret.distance_scorer"), INITIAL_THRESHOLD("initial_threshold"), ITERATIONS("iterations"),
    THREADS("threads"),
    RANDOM_REGRET_MIN_SHARE("random_regret.min_share"),
    RANDOM_REGRET_MAX_SHARE("random_regret.max_share"),
    RANDOM_BEST_MIN_SHARE("random_best.min_share"),
    RANDOM_BEST_MAX_SHARE("random_best.max_share"),
    RADIAL_MIN_SHARE("radial.min_share"),
    RADIAL_MAX_SHARE("radial.max_share"),
    CLUSTER_MIN_SHARE("cluster.min_share"),
    CLUSTER_MAX_SHARE("cluster.max_share"),
    WORST_MIN_SHARE("worst.min_share"),
    WORST_MAX_SHARE("worst.max_share"),
    THRESHOLD_ALPHA("threshold.alpha"),
    THRESHOLD_INI("threshold.ini"),
    THRESHOLD_INI_ABS("threshold.ini_abs"),
    INSERTION_NOISE_LEVEL("insertion.noise_level"),
    INSERTION_NOISE_PROB("insertion.noise_prob"),
    RUIN_WORST_NOISE_LEVEL("worst.noise_level"),
    RUIN_WORST_NOISE_PROB("worst.noise_prob"),
    FAST_REGRET("regret.fast"),
    MAX_TRANSPORT_COSTS("max_transport_costs"),
    CONSTRUCTION("construction"),
    BREAK_SCHEDULING("break_scheduling"),
    STRING_K_MIN("string_kmin"),
    STRING_K_MAX("string_kmax"),
    STRING_L_MIN("string_lmin"),
    STRING_L_MAX("string_lmax"),
    MIN_UNASSIGNED("min_unassigned"),
    PROPORTION_UNASSIGNED("proportion_unassigned");



    String paraName;

    Parameter(String name) {
        this.paraName = name;
    }

    public String toString() {
        return paraName;
    }

}
