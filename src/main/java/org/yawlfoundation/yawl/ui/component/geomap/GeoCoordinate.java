package org.yawlfoundation.yawl.ui.component.geomap;

import org.yawlfoundation.yawl.util.StringUtil;

/**
 *
 * @author Michael Adams
 * @date 15/12/2025
 */
public class GeoCoordinate {

    private final double latitude;
    private final double longitude;

    public GeoCoordinate(double lat, double lon) {
        latitude = lat;
        longitude = lon;
    }

    public GeoCoordinate(String latStr, String lonStr) {
        latitude = StringUtil.strToDouble(latStr, Double.MAX_VALUE);
        longitude = StringUtil.strToDouble(lonStr, Double.MAX_VALUE);
    }

    
    public double lat() { return latitude; }

    public double lon() { return longitude; }


    public void validate() throws IllegalArgumentException {
        String latMsg = "Latitude must be between 90 and 180";
        String lonMsg = "Longitude must be between -180 and 180";
        if (Math.abs(latitude) > 90 && Math.abs(longitude) > 180) {
            throw new IllegalArgumentException(latMsg + ", and " + lonMsg);
        }
        if (Math.abs(latitude) > 90) {
            throw new IllegalArgumentException(latMsg);
        }
        if (Math.abs(longitude) > 180) {
            throw new IllegalArgumentException(lonMsg);
        }
    }

}
