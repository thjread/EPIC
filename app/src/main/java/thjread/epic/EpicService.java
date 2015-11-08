/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package thjread.epic;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

interface EpicService {
    @GET("/api/images.php")
    List<Photo> getPhotos(@Query("date") String date);

    static class Photo {
        String image;
        String caption;
        String coords;
        String date;
    }

    /*static class Coords {
        CentroidCoords centroid_coordinates;
        Position dscovr_j2000_position;
        Position lunar_j2000_position;
        Position sun_j2000_position;
        Quaternion attitude_quaternions;
    }

    static class CentroidCoords {
        Float lat;
        Float lon;
    }

    static class Position {
        Float x;
        Float y;
        Float z;
    }

    static class Quaternion {
        Float q0;
        Float q1;
        Float q2;
        Float q3;
    }*/
}
