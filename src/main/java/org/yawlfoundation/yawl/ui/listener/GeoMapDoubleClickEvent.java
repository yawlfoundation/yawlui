package org.yawlfoundation.yawl.ui.listener;

/**
 *
 * @author Michael Adams
 * @date 7/1/2026
 */
public class GeoMapDoubleClickEvent {

    private final double latitude;
    private final double longitude;
    private final EventType type;

    public enum EventType { OnNewLocation, OnPolygonEdge }

    public GeoMapDoubleClickEvent(double latitude, double longitude) {
        this(EventType.OnNewLocation, latitude, longitude);
    }

    public GeoMapDoubleClickEvent(EventType type, double latitude, double longitude) {
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public EventType getType() {
        return type;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
