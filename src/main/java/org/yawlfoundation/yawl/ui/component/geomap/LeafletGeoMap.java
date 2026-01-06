package org.yawlfoundation.yawl.ui.component.geomap;

/**
 *
 * @author Michael Adams
 * @date 15/12/2025
 */
public class LeafletGeoMap { //} extends AbstractGeoMap<LComponent> {

//    private final Map<LComponent, List<LPoint>> _polygon2points = new HashMap<>();
//    private LMap _map;
//
//    public LeafletGeoMap() {
////        setFlexDirection(FlexLayout.FlexDirection.ROW);
////        getStyle().set("position", "relative");
////        setSizeFull();
//    }
//
//
//    @Override
//    public Component getMap() {
//        GeoCoordinate coordinate = getDefaultOrigin();
//        _map = new LMap(coordinate.lat(), coordinate.lon(), DEFAULT_ZOOM_LEVEL,
//                LTileLayer.DEFAULT_OPENSTREETMAP_TILE);
//        _map.setSizeFull();
//        return _map;
//    }
//
//
//    @Override
//    public int drawMarker(GeoCoordinate coordinate) {
//        LMarker marker = new LMarker(coordinate.lat(), coordinate.lon());
//        _map.addLComponents(marker);
//        int ref = addOverlay(marker);
//        centerAndZoom();
//        return ref;
//    }
//
//    @Override
//    public void removeMarker(int ref) {
//        removeOverlay(ref);
//    }
//
//
//    @Override
//    public int drawCircle(GeoCoordinate coordinate, double radius) {
//        LCircle circle = new LCircle(coordinate.lat(), coordinate.lon(), radius);
//        circle.setStrokeColor(getNextColour());
//        if (_fillOpacity > 0) {
//            circle.setFillColor(circle.getStrokeColor());
//            circle.setFillOpacity(_fillOpacity);
//            circle.setFill(true);
//        }
//        circle.setStrokeWeight(_lineWeight);
//        _map.addLComponents(circle);
//        int ref = addOverlay(circle);
//        centerAndZoom();
//        return ref;
//    }
//
//    @Override
//    public void removeCircle(int ref) {
//        removeOverlay(ref);
//    }
//
//
//    @Override
//    public int drawRectangle(GeoCoordinate topLeft, GeoCoordinate bottomRight) {
//        return drawPolygon(
//                List.of(topLeft,
//                        new GeoCoordinate(topLeft.lat(), bottomRight.lon()),
//                        bottomRight,
//                        new GeoCoordinate(bottomRight.lat(), topLeft.lon())
//                        ));
//    }
//
//    @Override
//    public void removeRectangle(int ref) {
//        removeOverlay(ref);
//    }
//
//
//    @Override
//    public int drawPolygon(List<GeoCoordinate> coordinates) {
//        List<LPoint> vertices = convertToLPointList(coordinates);
//        LPolygon polygon = new LPolygon(vertices);
//
//        // need this because there's no polygon.getPoints()
//        _polygon2points.put(polygon, vertices);
//
//        polygon.setStrokeColor(getNextColour());
//        if (_fillOpacity > 0) {
//            polygon.setFillColor(polygon.getStrokeColor());
//            polygon.setFillOpacity(_fillOpacity);
//            polygon.setFill(true);
//        }
//        polygon.setStrokeWeight(_lineWeight);
//        _map.addLComponents(polygon);
//        int ref = addOverlay(polygon);
//        centerAndZoom();
//        return ref;
//    }
//
//    @Override
//    public void removePolygon(int ref) {
//        removeOverlay(ref);
//    }
//
//
//    public LComponent removeOverlay(int ref) {
//        LComponent overlay = super.removeOverlay(ref);
//        if (overlay != null) {
//            if (overlay instanceof LCircle) {
//                pushColour(((LCircle) overlay).getStrokeColor());
//            }
//            if (overlay instanceof LPolygon) {
//                pushColour(((LPolygon) overlay).getStrokeColor());
//                _polygon2points.remove(overlay);
//            }
//        }
//        _map.removeLComponents(overlay);
//        return overlay;
//    }
//
//
//    private LPoint toLPoint(GeoCoordinate coordinate) {
//        return new LPoint(coordinate.lat(), coordinate.lon());
//    }
//
//
//    private List<LPoint> convertToLPointList(List<GeoCoordinate> coordinates) {
//        List<LPoint> points = new ArrayList<>();
//        coordinates.forEach(coordinate -> points.add(toLPoint(coordinate)));
//        return points;
//    }
//
//    private List<GeoCoordinate> convertToGeoPointList(List<LPoint> points) {
//        List<GeoCoordinate> coordinates = new ArrayList<>();
//        points.forEach(point -> coordinates.add(new GeoCoordinate(point.getLat(), point.getLon())));
//        return coordinates;
//    }
//
//
//    private void centerAndZoom() {
//        List<GeoCoordinate> points = new ArrayList<>();
//        for (LComponent overlay : getOverlays()) {
//            if (overlay instanceof LMarker) {
//                LMarker marker = (LMarker) overlay;
//                points.add(new GeoCoordinate(marker.getLat(), marker.getLon()));
//            }
//            else if (overlay instanceof LCircle) {
//                LCircle circle = (LCircle) overlay;
//                LPoint center = circle.getGeometry();
//                GeoCoordinate centerGeo = new GeoCoordinate(center.getLat(), center.getLon());
//                GeoBounds bounds = getBounds(centerGeo, circle.getRadius());
//                points.addAll(bounds.getPoints());
//            }
//            else if (overlay instanceof LPolygon) {
//                List<LPoint> polyPoints = _polygon2points.get(overlay);
//                points.addAll(convertToGeoPointList(polyPoints));
//            }
//        }
//
//        GeoBounds bounds = getBounds(points);
//        LPoint tl = toLPoint(bounds.getTopLeft());
//        LPoint br = toLPoint(bounds.getBottomRight());
//        _map.centerAndZoom(tl, br);
//    }
    
}
