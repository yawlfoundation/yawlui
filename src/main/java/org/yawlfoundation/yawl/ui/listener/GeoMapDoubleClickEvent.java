package org.yawlfoundation.yawl.ui.listener;

/**
 *
 * @author Michael Adams
 * @date 7/1/2026
 */
public class GeoMapDoubleClickEvent {

    private final double latitude;
    private final double longitude;

    public GeoMapDoubleClickEvent(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
