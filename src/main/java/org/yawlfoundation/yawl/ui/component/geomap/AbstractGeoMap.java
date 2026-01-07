package org.yawlfoundation.yawl.ui.component.geomap;

import com.vaadin.flow.component.Component;
import org.yawlfoundation.yawl.ui.util.ApplicationProperties;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Michael Adams
 * @date 15/12/2025
 */
abstract class AbstractGeoMap<T> {

    private static final double LAT_DEGREE_PER_METRE = 0.00001;
    protected static final String DEFAULT_COLOR = "#FF0000";    // red
    private static final GeoCoordinate DEFAULT_ORIGIN =
            new GeoCoordinate(48.8575, 2.3514);  // Paris

    protected static final int DEFAULT_ZOOM_LEVEL = 16;
    protected static final Object MOUSE_EVENT_MUTEX = new Object();

    private final Map<Integer, T> overlays = new HashMap<>();
    private final AtomicInteger _nextRef = new AtomicInteger();

    private final List<String> _colours = ApplicationProperties.getGeoColors();
    protected final double _fillOpacity = ApplicationProperties.getGeoFillOpacity();
    protected final int _lineWeight = ApplicationProperties.getGeoLineWeight();

    private int _nextColour = 0;
    private String _pushedColour = null;


    public AbstractGeoMap() { }

    public abstract Component getMap();


    public abstract int drawMarker(GeoCoordinate coordinate, boolean draggable);

    public abstract void updateMarker(int ref, GeoCoordinate coordinate);

    public abstract void removeMarker(int ref);


    public abstract int drawCircle(GeoCoordinate coordinate, double radius, boolean draggable);

    public abstract void updateCircle(int ref, GeoCoordinate coordinate, double radius);

    public abstract void removeCircle(int ref);


    public abstract int drawRectangle(GeoCoordinate topLeft, GeoCoordinate bottomRight, boolean draggable);

    public abstract void updateRectangle(int ref, GeoCoordinate topLeft, GeoCoordinate bottomRight);

    public abstract void removeRectangle(int ref);


    public abstract int drawPolygon(List<GeoCoordinate> coordinates, boolean draggable);

    public abstract void updatePolygon(int ref, List<GeoCoordinate> coordinates);

    public abstract void removePolygon(int ref);
    

    protected GeoCoordinate getDefaultOrigin() {
        List<Double> defOrigin = ApplicationProperties.getGeoOrigin();
        if (defOrigin.size() < 2) {
            return DEFAULT_ORIGIN;
        }
        GeoCoordinate origin = new GeoCoordinate(defOrigin.get(0), defOrigin.get(1));
        try {
            origin.validate();
        }
        catch (IllegalArgumentException e) {
            return DEFAULT_ORIGIN;
        }
        return origin;
    }

    
    protected int addOverlay(T overlay) {
        int ref = _nextRef.getAndIncrement();
        overlays.put(ref, overlay);
        return ref;
    }


    protected T getOverlay(int ref) {
        return overlays.get(ref);
    }


    protected int updateOverlay(T oldOverlay, T newOverlay) {
        int ref = getOverlayRef(oldOverlay);
        overlays.put(ref, newOverlay);
        return ref;
    }


    public T removeOverlay(int ref) {
        return overlays.remove(ref);
    }


    public T removeOverlay(T overlay) {
        return overlays.remove(getOverlayRef(overlay));
    }


    protected List<T> getOverlays() {
        return new ArrayList<>(overlays.values());
    }


    protected int getOverlayRef(T overlay) {
        for (int ref : overlays.keySet()) {
            if (overlay == overlays.get(ref)) {
                return ref;
            }
        }
        return -1;
    }


    protected double getFillOpacity() {
        return _fillOpacity;
    }

    protected int getLineWeight() {
        return _lineWeight;
    }

    protected void pushColour(String colour) {
        _pushedColour = colour;
    }

    protected String getPushedColour() {
        return _pushedColour;
    }


    // for a circle
    protected GeoBounds getBounds(GeoCoordinate center, double radius) {
        double radiusDegrees = radius * LAT_DEGREE_PER_METRE;
        double lat = center.lat();
        double lon = center.lon();

        return new GeoBounds(lat - radiusDegrees, lon - radiusDegrees,
                lat + radiusDegrees, lon + radiusDegrees);
    }


    protected GeoBounds getBounds(List<GeoCoordinate> points) {
        double maxLat = -100;
        double maxLon = -200;
        double minLat = Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        for (GeoCoordinate point : points) {
            maxLat = Math.max(maxLat, point.lat());
            maxLon = Math.max(maxLon, point.lon());
            minLat = Math.min(minLat, point.lat());
            minLon = Math.min(minLon, point.lon());
        }

        return new GeoBounds(maxLat, minLon, minLat, maxLon);
    }


    protected String getNextColour() {
        if (_pushedColour != null) {
            String colour = _pushedColour;
            _pushedColour = null;
            return colour;
        }
        if (_colours.isEmpty()) {
            return DEFAULT_COLOR;
        }
        if (_nextColour == _colours.size()) {
            _nextColour = 0;
        }
        return _colours.get(_nextColour++);
    }
    
}
