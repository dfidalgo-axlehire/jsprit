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
package com.graphhopper.jsprit.core.algorithm.state;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.solution.route.RouteVisitor;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.ActWithoutStaticLocation;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

import java.io.Serializable;
import java.util.Iterator;


/**
 * Updates arrival and end times of activities.
 * <p>
 * <p>Note that this modifies arrTime and endTime of each activity in a route.
 *
 * @author stefan
 */
public class UpdateActivityPrevLocations implements RouteVisitor, StateUpdater, Serializable {

    private Location lastLocation;

    @Override
    public void visit(VehicleRoute route) {
        begin(route);
        Iterator<TourActivity> iterator = route.getTourActivities().iterator();
        while (iterator.hasNext()) {
            visit(iterator.next());
        }
        finish();
    }

    public void begin(VehicleRoute route) {
        this.lastLocation = route.getStart().getLocation();
    }


    public void visit(TourActivity activity) {
        if (activity instanceof ActWithoutStaticLocation) {
            ((ActWithoutStaticLocation) activity).setPreviousLocation(lastLocation);
        } else lastLocation = activity.getLocation();
    }


    public void finish() {

    }

}
