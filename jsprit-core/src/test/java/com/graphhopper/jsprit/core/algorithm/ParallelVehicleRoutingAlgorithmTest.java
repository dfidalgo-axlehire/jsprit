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

import com.graphhopper.jsprit.core.algorithm.listener.parallel.ParallelIterationStartsListener;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParallelVehicleRoutingAlgorithmTest {

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
    public void whenSettingIterations_itIsSetCorrectly() {
        ParallelVehicleRoutingAlgorithm algorithm = new ParallelVehicleRoutingAlgorithm(mock(VehicleRoutingProblem.class),
                                                                                mock(SearchStrategyManager.class),
                                                                                UUID.randomUUID().toString(),
                                                                                redissonClient);
        algorithm.setMaxIterations(50);
        assertEquals(50, algorithm.getMaxIterations());
    }

    @Test
    public void whenSettingIterationsWithMaxIterations_itIsSetCorrectly() {
        ParallelVehicleRoutingAlgorithm algorithm = new ParallelVehicleRoutingAlgorithm(mock(VehicleRoutingProblem.class),
                                                                                        mock(SearchStrategyManager.class),
                                                                                        UUID.randomUUID().toString(),
                                                                                        redissonClient);
        algorithm.setMaxIterations(50);
        assertEquals(50, algorithm.getMaxIterations());
    }

    private static class CountIterations implements ParallelIterationStartsListener {

        private int countIterations = 0;

        @Override
        public void informIterationStarts(int i, VehicleRoutingProblem problem, RList<VehicleRoutingProblemSolution> solutions) {
            countIterations++;
        }

        public int getCountIterations() {
            return countIterations;
        }

    }

    @Test
    public void whenSettingIterationsWithMaxIterations_iterAreExecutedCorrectly() {
        SearchStrategyManager stratManager = mock(SearchStrategyManager.class);
        ParallelVehicleRoutingAlgorithm algorithm = new ParallelVehicleRoutingAlgorithm(mock(VehicleRoutingProblem.class),
            stratManager, UUID.randomUUID().toString(), redissonClient);
        when(stratManager.getRandomStrategy()).thenReturn(mock(SearchStrategy.class));
        when(stratManager.getWeights()).thenReturn(Arrays.asList(1.0));
        algorithm.setMaxIterations(1000);
        CountIterations counter = new CountIterations();
        algorithm.addListener(counter);
        algorithm.searchSolutions();
        assertEquals(1000, counter.getCountIterations());
    }

    @Test
    public void whenSettingIterations_iterAreExecutedCorrectly() {
        SearchStrategyManager stratManager = mock(SearchStrategyManager.class);
        ParallelVehicleRoutingAlgorithm algorithm = new ParallelVehicleRoutingAlgorithm(mock(VehicleRoutingProblem.class),
            stratManager, UUID.randomUUID().toString(), redissonClient);
        when(stratManager.getRandomStrategy()).thenReturn(mock(SearchStrategy.class));
        when(stratManager.getWeights()).thenReturn(Arrays.asList(1.0));
        algorithm.setMaxIterations(1000);
        CountIterations counter = new CountIterations();
        algorithm.addListener(counter);
        algorithm.searchSolutions();
        assertEquals(1000, counter.getCountIterations());
    }
    @AfterEach
    public void tearDown() {
        redisServer.stop();
    }

}
