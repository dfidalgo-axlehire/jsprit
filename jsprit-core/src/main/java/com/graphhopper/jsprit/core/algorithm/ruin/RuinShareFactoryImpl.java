package com.graphhopper.jsprit.core.algorithm.ruin;

import com.graphhopper.jsprit.core.util.RandomNumberGeneration;

import java.util.Random;

public class RuinShareFactoryImpl implements RuinShareFactory

{

    private int maxShare;

    private int minShare;

    private Random random = RandomNumberGeneration.getRandom();

    public void setRandom(Random random) {
        this.random = random;
    }

    public RuinShareFactoryImpl(int minShare, int maxShare) {
        if (maxShare < minShare)
            throw new IllegalArgumentException("maxShare must be equal or greater than minShare");
        this.minShare = minShare;
        this.maxShare = maxShare;
    }

    public RuinShareFactoryImpl(int minShare, int maxShare, Random random) {
        if (maxShare < minShare)
            throw new IllegalArgumentException("maxShare must be equal or greater than minShare");
        this.minShare = minShare;
        this.maxShare = maxShare;
        this.random = random;
    }

    @Override
    public int createNumberToBeRemoved() {
        return (int) (minShare + (maxShare - minShare) * random.nextDouble());
    }

}
