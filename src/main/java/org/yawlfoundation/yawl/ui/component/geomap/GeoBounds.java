package org.yawlfoundation.yawl.ui.component.geomap;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Michael Adams
 * @date 22/12/2025
 */
public class GeoBounds {

    private final GeoCoordinate topLeft;
    private final GeoCoordinate bottomRight;

    public GeoBounds(GeoCoordinate topLeft, GeoCoordinate bottomRight) {
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
    }

    public GeoBounds(double lat1, double lng1, double lat2, double lng2) {
        this.topLeft = new GeoCoordinate(lat1, lng1);
        this.bottomRight = new GeoCoordinate(lat2, lng2);
    }

    public GeoCoordinate getTopLeft() {
        return topLeft;
    }

    public GeoCoordinate getBottomRight() {
        return bottomRight;
    }

    public List<GeoCoordinate> getPoints() {
        List<GeoCoordinate> points = new ArrayList<>();
        points.add(topLeft);
        points.add(bottomRight);
        return points;
    }

}
