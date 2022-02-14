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

package com.graphhopper.jsprit.core.problem;

import com.graphhopper.jsprit.core.util.Coordinate;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by schroeder on 16.12.14.
 */
public class LocationTest {

    @Test
    public void whenIndexSet_buildLocation() {
        Location l = Location.Builder.newInstance().setIndex(1).build();
        assertEquals(1, l.getIndex());
        assertTrue(true);
    }

    @Test
    public void whenNameSet_buildLocation() {
        Location l = Location.Builder.newInstance().setName("mystreet 6a").setIndex(1).build();
        assertEquals("mystreet 6a", l.getName());
    }

    @Test
    public void whenIndexSetWitFactory_returnCorrectLocation() {
        Location l = Location.newInstance(1);
        assertEquals(1, l.getIndex());
        assertTrue(true);
    }

    @Test
    public void whenIndexSmallerZero_throwException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Location l = Location.Builder.newInstance().setIndex(-1).build();
        });
    }

    @Test
    public void whenCoordinateAndIdAndIndexNotSet_throwException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Location l = Location.Builder.newInstance().build();
        });
    }

    @Test
    public void whenIdSet_build() {
        Location l = Location.Builder.newInstance().setId("id").build();
        assertEquals("id", l.getId());
        assertTrue(true);
    }

    @Test
    public void whenIdSetWithFactory_returnCorrectLocation() {
        Location l = Location.newInstance("id");
        assertEquals("id", l.getId());
        assertTrue(true);
    }

    @Test
    public void whenCoordinateSet_build() {
        Location l = Location.Builder.newInstance().setCoordinate(Coordinate.newInstance(10, 20)).build();
        assertEquals(10., l.getCoordinate().getX(), 0.001);
        assertEquals(20., l.getCoordinate().getY(), 0.001);
        assertTrue(true);
    }

    @Test
    public void whenCoordinateSetWithFactory_returnCorrectLocation() {
        //        Location l = Location.Builder.newInstance().setCoordinate(Coordinate.newInstance(10,20)).build();
        Location l = Location.newInstance(10, 20);
        assertEquals(10., l.getCoordinate().getX(), 0.001);
        assertEquals(20., l.getCoordinate().getY(), 0.001);
        assertTrue(true);
    }


    @Test
    public void whenSettingUserData_itIsAssociatedWithTheLocation() {
        Location one = Location.Builder.newInstance().setCoordinate(Coordinate.newInstance(10, 20))
            .setUserData(new HashMap<String, Object>()).build();
        Location two = Location.Builder.newInstance().setIndex(1).setUserData(42).build();
        Location three = Location.Builder.newInstance().setIndex(2).build();

        assertTrue(one.getUserData() instanceof Map);
        assertEquals(42, two.getUserData());
        assertNull(three.getUserData());
    }

}
