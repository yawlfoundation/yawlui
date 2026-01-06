package org.yawlfoundation.yawl.ui.listener;

import org.yawlfoundation.yawl.ui.component.geomap.GeoCoordinate;
import org.yawlfoundation.yawl.ui.component.geomap.OverlayType;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Michael Adams
 * @date 22/12/2025
 */
public class GeoMapOverlayMovedEvent {

    private final List<GeoCoordinate> _points = new ArrayList<>();
    private final OverlayType _overlayType;
    private final int _ref;
    private double _radius;

    public GeoMapOverlayMovedEvent(GeoCoordinate location, int ref) {
        _points.add(location);
        _overlayType = OverlayType.Marker;
        _ref = ref;
    }

    public GeoMapOverlayMovedEvent(GeoCoordinate center, double radius, int ref) {
        _points.add(center);
        _overlayType = OverlayType.Circle;
        _ref = ref;
        _radius = radius;
    }

    public GeoMapOverlayMovedEvent(List<GeoCoordinate>  points, int ref) {
        _points.addAll(points);
        _overlayType = OverlayType.Polygon;
        _ref = ref;
    }

    // for Marker and Circle
    public GeoCoordinate getPoint() {
        return _points.get(0);
    }

    // for Polygon
    public List<GeoCoordinate> getPoints() {
        return _points;
    }

    public OverlayType getOverlayType() {
        return _overlayType;
    }

    public int getRef() {
        return _ref;
    }

    public double getRadius() {
        return _radius;
    }
}
