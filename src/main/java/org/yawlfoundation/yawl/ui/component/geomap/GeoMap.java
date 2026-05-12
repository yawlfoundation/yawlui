package org.yawlfoundation.yawl.ui.component.geomap;

import com.vaadin.flow.component.Component;

import java.util.List;

/**
 *
 * @author Michael Adams
 * @date 9/4/2026
 */
public interface GeoMap {

    Component getMap();

    int drawMarker(GeoCoordinate coordinate, boolean draggable, String label);

    void updateMarker(int ref, GeoCoordinate coordinate, String label);

    void removeMarker(int ref);


    int drawCircle(GeoCoordinate coordinate, double radius, boolean draggable, String label);

    void updateCircle(int ref, GeoCoordinate coordinate, double radius, String label);

    void removeCircle(int ref);


    int drawRectangle(GeoCoordinate topLeft, GeoCoordinate bottomRight, boolean draggable, String label);

    void updateRectangle(int ref, GeoCoordinate topLeft, GeoCoordinate bottomRight, String label);

    void removeRectangle(int ref);
    

    int drawPolygon(List<GeoCoordinate> coordinates, boolean draggable, String label);

    void updatePolygon(int ref, List<GeoCoordinate> coordinates, String label);

    void removePolygon(int ref);

}
