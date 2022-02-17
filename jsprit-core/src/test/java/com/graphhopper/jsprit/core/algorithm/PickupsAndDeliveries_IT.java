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
package com.graphhopper.jsprit.core.algorithm;

import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.box.enumeration.Parameter;
import com.graphhopper.jsprit.core.algorithm.box.parallel.ParallelJsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.LiLimReader;
import com.graphhopper.jsprit.core.util.Solutions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class PickupsAndDeliveries_IT {

    private static RedisServer redisServer;
    private static RedissonClient redissonClient;

    @BeforeEach
    public void setup() throws IOException {
        redisServer = new RedisServer(6379);
        redisServer.start();

        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");

        redissonClient = Redisson.create(config);
    }

    @Test
    public void whenSolvingLR101InstanceOfLiLim_solutionsMustNoBeWorseThan5PercentOfBestKnownSolution() {
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        new LiLimReader(vrpBuilder).read(getClass().getResourceAsStream("lr101.txt"));
        VehicleRoutingProblem vrp = vrpBuilder.build();
        VehicleRoutingAlgorithm vra = Jsprit.Builder.newInstance(vrp).setProperty(Parameter.FAST_REGRET, "true").buildAlgorithm();
        Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();
        assertEquals(1650.8, Solutions.bestOf(solutions).getCost(), 80.);
        assertEquals(19, Solutions.bestOf(solutions).getRoutes().size(), 1);
    }

    @Test
    public void whenSolvingLR101InstanceOfLiLim_solutionsMustNoBeWorseThan5PercentOfBestKnownSolution_parallel() {
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        new LiLimReader(vrpBuilder).read(getClass().getResourceAsStream("lr101.txt"));
        VehicleRoutingProblem vrp = vrpBuilder.build();
        Collection<VehicleRoutingProblemSolution> solutions = new ArrayList<>();
        String id = UUID.randomUUID().toString();
        IntStream.range(0, 20).parallel().forEach((i) -> {
            ParallelVehicleRoutingAlgorithm vra = ParallelJsprit.Builder.newInstance(vrp)
                                                                        .addId(id)
                                                                        .addRedisson(redissonClient)
                                                                        .setProperty(Parameter.FAST_REGRET, "true")
                                                                        .buildAlgorithm();

            vra.setMaxIterations(100);
            solutions.addAll(vra.searchSolutions());
        });
        assertEquals(1650.8, Solutions.bestOf(solutions).getCost(), 80.);
        assertEquals(19, Solutions.bestOf(solutions).getRoutes().size(), 1);
    }

    @AfterEach
    public void tearDown() {
        redisServer.stop();
    }

}
