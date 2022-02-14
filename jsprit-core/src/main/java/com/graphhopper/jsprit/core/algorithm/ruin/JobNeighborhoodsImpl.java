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

package com.graphhopper.jsprit.core.algorithm.ruin;

import com.graphhopper.jsprit.core.algorithm.ruin.distance.JobDistance;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.util.StopWatch;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Created by schroeder on 07/01/15.
 */
class JobNeighborhoodsImpl implements JobNeighborhoods {

    private static final Logger logger = Logger.getLogger(JobNeighborhoodsImpl.class.getName());

    private final VehicleRoutingProblem vrp;

    private final Map<String, TreeSet<ReferencedJob>> distanceNodeTree = new HashMap<>();

    private final JobDistance jobDistance;

    private double maxDistance = 0.;

    public JobNeighborhoodsImpl(VehicleRoutingProblem vrp, JobDistance jobDistance) {
        super();
        this.vrp = vrp;
        this.jobDistance = jobDistance;
        logger.info("initialise " + this);
    }

    @Override
    public Iterator<Job> getNearestNeighborsIterator(int nNeighbors, Job neighborTo) {
        TreeSet<ReferencedJob> tree = distanceNodeTree.get(neighborTo.getId());
        if (tree == null) return new Iterator<Job>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Job next() {
                return null;
            }

            @Override
            public void remove() {

            }
        };
        Iterator<ReferencedJob> descendingIterator = tree.iterator();
        return new NearestNeighborhoodIterator(descendingIterator, nNeighbors);
    }

    @Override
    public void initialise() {
        logger.info("calculates and memorizes distances from EACH job to EACH job --> n^2 calculations");
        calculateDistancesFromJob2Job();
    }

    @Override
    public double getMaxDistance() {
        return maxDistance;
    }

    private void calculateDistancesFromJob2Job() {
        logger.info("preprocess distances between locations ...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int nuOfDistancesStored = 0;
        for (Job i : vrp.getJobs().values()) {
            TreeSet<ReferencedJob> treeSet = new TreeSet<>(
                (o1, o2) -> {
                    if (o1.getDistance() <= o2.getDistance()) {
                        return -1;
                    } else {
                        return 1;
                    }
                });
            distanceNodeTree.put(i.getId(), treeSet);
            for (Job j : vrp.getJobs().values()) {
                if (i == j) continue;
                double distance = jobDistance.getDistance(i, j);
                if (distance > maxDistance) maxDistance = distance;
                ReferencedJob refNode = new ReferencedJob(j, distance);
                treeSet.add(refNode);
                nuOfDistancesStored++;
            }

        }
        stopWatch.stop();
        logger.info(String.format("preprocessing comp-time: %s; nuOfDistances stored: %s; estimated memory: %s" +
            " bytes", stopWatch, nuOfDistancesStored, (distanceNodeTree.keySet().size() * 64 + nuOfDistancesStored * 92)));
    }

}
