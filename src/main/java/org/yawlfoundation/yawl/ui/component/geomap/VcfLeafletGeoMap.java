package org.yawlfoundation.yawl.ui.component.geomap;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.VaadinService;
import org.apache.commons.lang3.StringUtils;
import org.vaadin.addons.componentfactory.leaflet.LeafletMap;
import org.vaadin.addons.componentfactory.leaflet.layer.InteractiveLayer;
import org.vaadin.addons.componentfactory.leaflet.layer.groups.LayerGroup;
import org.vaadin.addons.componentfactory.leaflet.layer.map.options.DefaultMapOptions;
import org.vaadin.addons.componentfactory.leaflet.layer.map.options.MapOptions;
import org.vaadin.addons.componentfactory.leaflet.layer.raster.TileLayer;
import org.vaadin.addons.componentfactory.leaflet.layer.ui.marker.Marker;
import org.vaadin.addons.componentfactory.leaflet.layer.ui.tooltip.Tooltip;
import org.vaadin.addons.componentfactory.leaflet.layer.vectors.Circle;
import org.vaadin.addons.componentfactory.leaflet.layer.vectors.Path;
import org.vaadin.addons.componentfactory.leaflet.layer.vectors.Polygon;
import org.vaadin.addons.componentfactory.leaflet.layer.vectors.structure.MultiLatLngArray;
import org.vaadin.addons.componentfactory.leaflet.types.Icon;
import org.vaadin.addons.componentfactory.leaflet.types.LatLng;
import org.vaadin.addons.componentfactory.leaflet.types.LatLngBounds;
import org.vaadin.addons.componentfactory.leaflet.types.Point;
import org.yawlfoundation.yawl.ui.listener.GeoMapDoubleClickEvent;
import org.yawlfoundation.yawl.ui.listener.GeoMapDoubleClickListener;
import org.yawlfoundation.yawl.ui.listener.GeoMapOverlayMoveListener;
import org.yawlfoundation.yawl.ui.listener.GeoMapOverlayMovedEvent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 *
 * @author Michael Adams
 * @date 21/12/2025
 */
public class VcfLeafletGeoMap extends AbstractGeoMap<InteractiveLayer> {

    private static final String BASE_URL = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
    private static final String OSM_ATTRIBUTION =
            "© <a href=\"https://www.openstreetmap.org\">OpenStreetMap</a>";
    private static final int MOUSE_MOVE_STEP = 4;
    private static final long MOUSE_MOVE_MIN_MS = 40;
    private static final String DEFAULT_MARKER_ICON_PATH = "icons/marker-icon.png";
    
    private final LeafletMap _map;
    private final LayerGroup _markers;
    private final Map<InteractiveLayer, Marker> _dragMarkerMap = new HashMap<>();
    private final Map<InteractiveLayer, Marker> _resizeMarkerMap = new HashMap<>();
    private final Map<InteractiveLayer, List<Marker>> _vertexMarkerMap = new HashMap<>();
    private final Map<Marker, String> _markerColorMap = new HashMap<>();
    private final List<GeoMapOverlayMoveListener> _moveListeners = new ArrayList<>();
    private final List<GeoMapDoubleClickListener> _dblClickListeners = new ArrayList<>();

    private double edgeToleranceMeters = 5;


    public VcfLeafletGeoMap() {
        _map = newMap(toLatLng(getDefaultOrigin()), true);
        _map.setSizeFull();
        _map.getStyle().set("min-width", "0");

        _markers = new LayerGroup();
        _markers.addTo(_map);

        _map.getElement().executeJs(
            "this.map.doubleClickZoom.disable();"
        );
        
        _map.addAttachListener(e ->
                _map.getElement().executeJs(
                        "setTimeout(() => this.invalidateSize(true), 500);\n" +
                                "setTimeout(() => this.invalidateSize(true), 1500);"
                )
        );

        _map.onZoomEnd(e -> {
            _map.invalidateSize(true);
            scaleEdgeTolerance();
        });

        _map.onMoveEnd(e -> _map.invalidateSize(true));

        _map.onDoubleClick(e -> {
            _dblClickListeners.forEach(l ->
                    l.mapDoubleClick(new GeoMapDoubleClickEvent(e.getLatLng().getLat(),
                            e.getLatLng().getLng())));
        });
    }

    
    @Override
    public Component getMap() {
        Div mapDiv = new Div();
        mapDiv.setSizeFull();
        mapDiv.getStyle().set("min-width", "0");
        mapDiv.add(_map);
        scaleEdgeTolerance();
        return mapDiv;
    }


    public InteractiveLayer removeOverlay(int ref) {
        InteractiveLayer layer = super.removeOverlay(ref);
        if (layer != null) {
            if (layer instanceof Marker) {
                _markers.removeLayer(layer);
            }
            else {
                layer.remove();
                Marker dragMarker = _dragMarkerMap.remove(layer);
                if (dragMarker != null) {
                    _markers.removeLayer(dragMarker);
                }
                Marker resizeMarker = _resizeMarkerMap.remove(layer);
                if (resizeMarker != null) {
                    _markers.removeLayer(resizeMarker);
                }
                List<Marker> vertexMarkers = _vertexMarkerMap.remove(layer);
                if (vertexMarkers != null) {
                    vertexMarkers.forEach(_markers::removeLayer);
                }
            }
            centerAndZoom();
        }
        return layer;
    }


    public String getOverlayColor(int ref) {
        InteractiveLayer overlay = getOverlay(ref);
        if (overlay != null) {
            if (overlay instanceof Path) {
                return ((Path) overlay).getColor();
            }
            else if (overlay instanceof Marker) {
                return _markerColorMap.get((Marker) overlay);
            }
        }
        return DEFAULT_COLOR;
    }

    
    public void addMoveListener(GeoMapOverlayMoveListener listener) {
        _moveListeners.add(listener);
    }


    public void addDoubleClickListener(GeoMapDoubleClickListener listener) {
        _dblClickListeners.add(listener);
    }


    public void invalidateSize() {
            _map.getElement().executeJs(
                "requestAnimationFrame(() => this.invalidateSize())"
        );
    }

    public void zoomOut() {
        _map.zoomOut(1);
    }


    public double getEdgeToleranceMeters() {
        return edgeToleranceMeters;
    }


    @Override
    public int drawMarker(GeoCoordinate coordinate, boolean draggable, String label) {
        Marker marker = drawMarker(toLatLng(coordinate), draggable, label);
        marker.setIcon(getNextMarkerIcon(marker));

        marker.onDragEnd(event -> announceMoveCompleted(marker, event.getLatLng()));

        marker.addTo(_markers);
        int ref = addOverlay(marker);
        centerAndZoom();
        return ref;
    }


    @Override
    public synchronized void updateMarker(int ref, GeoCoordinate coordinate, String label) {
        Marker currentMarker = (Marker) getOverlay(ref);
        LatLng updatedPos = toLatLng(coordinate);
        boolean markerReplaced = false;

        if (labelChanged(currentMarker, label)) {
            boolean draggable = currentMarker.isDraggable();
            Icon icon = currentMarker.getIcon();
            currentMarker.remove();
            Marker newMarker = drawMarker(updatedPos, draggable, label);
            newMarker.setIcon(icon);
            newMarker.onDragEnd(event -> announceMoveCompleted(newMarker, event.getLatLng()));
            newMarker.addTo(_markers);
            
            updateOverlay(currentMarker, newMarker);
            currentMarker = newMarker;
            markerReplaced = true;
        }
        
        if (! (markerReplaced || latLngEquals(currentMarker.getLatLng(), updatedPos))) {
            currentMarker.setLatLng(updatedPos);
        }

        centerAndZoom();
    }


    
    @Override
    public void removeMarker(int ref) { }

    
    @Override
    public int drawCircle(GeoCoordinate coordinate, double radius, boolean draggable, String label) {
        LatLng center = toLatLng(coordinate);
        Circle circle = drawCircle(center, radius, getNextColour());
        final int ref = addOverlay(circle);
        circle.setClassName("circle-" + ref);
        
        if (StringUtils.isNotEmpty(label)) {
            circle.bindTooltip(createLabel(label));
         }

        circle.addTo(_map);

        if (draggable) {
            Marker dragMarker = createDraggableMarker(center, ref, "drag");
            LatLng resizePos = offsetByMeters(center, 0, radius);
            Marker resizeMarker = createDraggableMarker(resizePos, ref,"resize");

            _map.getElement().executeJs(getCircleScript(ref));
            
            dragMarker.onDragEnd(event -> {
                Circle refCircle = (Circle) getOverlay(ref);
                drag(refCircle, event.getLatLng());           // one last time
                finaliseCircleMove(ref, true);
            });

            resizeMarker.onDragEnd(event -> {
                Circle refCircle = (Circle) getOverlay(ref);
                resize(refCircle, event.getLatLng());
                finaliseCircleMove(ref, true);
            });

            dragMarker.addTo(_markers);
            _dragMarkerMap.put(circle, dragMarker);
            resizeMarker.addTo(_markers);
            _resizeMarkerMap.put(circle, resizeMarker);
        }
        centerAndZoom();
        return ref;
    }
    

    @Override
    public synchronized void updateCircle(int ref, GeoCoordinate coordinate,
                                          double radius, String label) {
        Circle currentCircle = (Circle) getOverlay(ref);
        LatLng updatedCenter = toLatLng(coordinate);
        boolean movedOrResized = true;

        if (currentCircle.getRadius() != radius) {
            resize(currentCircle, currentCircle.getLatlng(), radius);
            currentCircle = (Circle) getOverlay(ref);
        }
        if (! latLngEquals(currentCircle.getLatlng(), updatedCenter)) {
            drag(currentCircle, updatedCenter);
            currentCircle = (Circle) getOverlay(ref);
        }
        if (labelChanged(currentCircle, label)) {
            updateLabel(currentCircle, label);
            movedOrResized = false;
        }
        if (movedOrResized) {
            finaliseCircleMove(ref, false);
        }
    }


    @Override
    public void removeCircle(int ref) { }

    
    private void finaliseCircleMove(int ref, boolean announce) {
        Circle circle = (Circle) getOverlay(ref);
        if (announce) {
            announceMoveCompleted(circle, circle.getLatlng(), ref);
        }

        // update final marker positions
        _dragMarkerMap.get(circle).setLatLng(circle.getLatlng());
        LatLng newResizePos = offsetByMeters(circle.getLatlng(),0, circle.getRadius());
        _resizeMarkerMap.get(circle).setLatLng(newResizePos);
        _map.getUI().ifPresent(ui -> ui.access(_map::invalidateSize));
    }


    private void finalisePolygonMove(int ref, List<LatLng> vertices,
                                     List<Marker> resizeMarkers, boolean announce) {
        Polygon polygon = (Polygon) getOverlay(ref);
        if (announce) {
            announceMoveCompleted(polygon, vertices, ref);
        }

        _dragMarkerMap.get(polygon).setLatLng(getCentroid(vertices));
        updateResizeMarkerPositions(resizeMarkers, vertices);
        _map.getUI().ifPresent(ui -> ui.access(_map::invalidateSize));
    }


    @Override
    public int drawRectangle(GeoCoordinate topLeft, GeoCoordinate bottomRight, boolean draggable, String label) {
        return drawPolygon(toRectangleVertices(topLeft, bottomRight), true, draggable, label);
    }

    
    @Override
    public void updateRectangle(int ref, GeoCoordinate topLeft, GeoCoordinate bottomRight, String label) {
        updatePolygon(ref, toRectangleVertices(topLeft, bottomRight), label);
    }


    @Override
    public void removeRectangle(int ref) { }


    @Override
    public int drawPolygon(List<GeoCoordinate> coordinates, boolean draggable, String label) {
        return drawPolygon(coordinates, false, draggable, label);
    }


    private int drawPolygon(List<GeoCoordinate> coordinates, boolean isRectangle,
                            boolean draggable, String label) {
        List<LatLng> vertices = sortClockwise(toLatLngList(coordinates));
        Polygon polygon = drawPolygon(vertices, getNextColour());
        int ref = addOverlay(polygon);
        polygon.setClassName("polygon-" + ref);

        if (StringUtils.isNotEmpty(label)) {
            polygon.bindTooltip(createLabel(label));
        }
        
        polygon.addTo(_map);

        if (draggable) {
            AtomicReference<LatLng> centroid = new AtomicReference<>(getCentroid(vertices));
            Marker dragMarker = createDraggableMarker(centroid.get(), ref, "drag");
            List<Marker> resizeMarkers = createResizeMarkers(vertices, ref);

            _map.getElement().executeJs(getPolygonScript(ref, isRectangle));

            dragMarker.onDragEnd(event -> {
                Polygon currentPolygon = (Polygon) getOverlay(ref);
                List<LatLng> currentVertices = getPolygonVertices(currentPolygon);
                LatLng newCentroid = event.getLatLng();
                List<LatLng> moved = updateVerticesOnMove(currentVertices,
                        new AtomicReference<>(getCentroid(currentVertices)), newCentroid);
                drag(currentPolygon, moved, ref); // Your existing drag method that removes/redraws
                vertices.clear();
                vertices.addAll(moved);
                finalisePolygonMove(ref, vertices, resizeMarkers, true);
            });

            for (int i = 0; i < resizeMarkers.size(); i++) {
                int index = i;
                resizeMarkers.get(i).onDragEnd(event -> {
                    Polygon currentPolygon = (Polygon) getOverlay(ref);
                    List<LatLng> currentVertices = getPolygonVertices(currentPolygon);
                    List<LatLng> updated = updateVerticesOnResize(currentVertices, index,
                            event.getLatLng(), isRectangle);
                    resize(currentPolygon, updated, ref);
                    vertices.clear();
                    vertices.addAll(updated);
                    finalisePolygonMove(ref, vertices, resizeMarkers, true);
                });
            }

            dragMarker.addTo(_markers);
            _dragMarkerMap.put(polygon, dragMarker);
            resizeMarkers.forEach(m -> {
                if (m.getLatLng() != null) m.addTo(_markers);
            });
            _vertexMarkerMap.put(polygon, resizeMarkers);
        }
        centerAndZoom();
        return ref;
    }


    public synchronized void updatePolygon(int ref, List<GeoCoordinate> coordinates, String label) {
        Polygon currentPolygon = (Polygon) getOverlay(ref);
        List<LatLng> newVertices = sortClockwise(toLatLngList(coordinates));
        boolean structureChanged = false;

        if (!verticesEqual(getPolygonVertices(currentPolygon), newVertices)) {
            resize(currentPolygon, newVertices, ref);
            currentPolygon = (Polygon) getOverlay(ref);
            structureChanged = true;
        }
        if (labelChanged(currentPolygon, label)) {
            updateLabel(currentPolygon, label);
            currentPolygon = (Polygon) getOverlay(ref);
        }
        if (structureChanged) {
            finalisePolygonMove(ref, newVertices, _vertexMarkerMap.get(currentPolygon), false);
        }
    }


    @Override
    public void removePolygon(int ref) { }


    private LeafletMap newMap(LatLng center, boolean drag) {
        MapOptions options = new DefaultMapOptions();
        options.setCenter(center);
        options.setZoom(16);
        options.setDragging(drag);
        LeafletMap map = new LeafletMap(options);
        TileLayer osm = new TileLayer(BASE_URL);
        osm.setAttribution(OSM_ATTRIBUTION);
        map.addLayer(osm);
        map.setMaxZoom(18);
        return map;
    }


   private List<GeoCoordinate> toRectangleVertices(GeoCoordinate topLeft,
                                                   GeoCoordinate bottomRight) {
       return List.of(topLeft,
               new GeoCoordinate(topLeft.lat(), bottomRight.lon()),
               bottomRight,
               new GeoCoordinate(bottomRight.lat(), topLeft.lon())
       );
   }


   private void resetPolygonVertices(int ref, List<LatLng> vertices) {
       Polygon refPolygon = (Polygon) getOverlay(ref);
       vertices.clear();
       vertices.addAll(getPolygonVertices(refPolygon));
   }


   private List<LatLng> getPolygonVertices(Polygon polygon) {
       MultiLatLngArray array = (MultiLatLngArray) polygon.getLatlngs();
       return sortClockwise(new ArrayList<>(array.getFirst()));
   }


    private void announceMove(Marker marker, LatLng movedTo) {
        if (hasMoved(marker.getLatLng(), movedTo)) {
            GeoMapOverlayMovedEvent moveEvent = new GeoMapOverlayMovedEvent(
                    toGeoCoordinate(movedTo), getOverlayRef(marker));
            announceMove(moveEvent);
        }
    }

    private void announceMove(Circle circle, LatLng movedTo, int ref) {
        GeoMapOverlayMovedEvent moveEvent = new GeoMapOverlayMovedEvent(
                toGeoCoordinate(movedTo), circle.getRadius(), ref);
        announceMove(moveEvent);
    }


    private void announceMove(Polygon polygon, List<LatLng> movedTo, int ref) {
        GeoMapOverlayMovedEvent moveEvent = new GeoMapOverlayMovedEvent(
               toGeoCoordinateList(movedTo), ref);
        announceMove(moveEvent);
    }


    private void announceMoveCompleted(Marker marker, LatLng movedTo) {
        if (hasMoved(marker.getLatLng(), movedTo)) {
            GeoMapOverlayMovedEvent moveEvent = new GeoMapOverlayMovedEvent(
                    toGeoCoordinate(movedTo), getOverlayRef(marker),
                    GeoMapOverlayMovedEvent.Mode.COMPLETED);
            announceMove(moveEvent);
        }
    }


    private void announceMoveCompleted(Circle circle, LatLng movedTo, int ref) {
        GeoMapOverlayMovedEvent moveEvent = new GeoMapOverlayMovedEvent(
                toGeoCoordinate(movedTo), circle.getRadius(), ref,
                GeoMapOverlayMovedEvent.Mode.COMPLETED);
        announceMove(moveEvent);
    }


    private void announceMoveCompleted(Polygon polygon, List<LatLng> movedTo, int ref) {
        GeoMapOverlayMovedEvent moveEvent = new GeoMapOverlayMovedEvent(
               toGeoCoordinateList(movedTo), ref, GeoMapOverlayMovedEvent.Mode.COMPLETED);
        announceMove(moveEvent);
    }


    private void announceMove(GeoMapOverlayMovedEvent event) {
        _moveListeners.forEach(l -> l.overlayMoved(event));
    }


    private List<LatLng> updateVerticesOnMove(List<LatLng> vertices,
                                              AtomicReference<LatLng> oldCentroid,
                                              LatLng movedTo) {
        double dLat = movedTo.getLat() - oldCentroid.get().getLat();
        double dLon = movedTo.getLng() - oldCentroid.get().getLng();

        List<LatLng> updated = new ArrayList<>();
        for (LatLng vertex : vertices) {
            updated.add(new LatLng(vertex.getLat() + dLat,
                    vertex.getLng() + dLon));
        }
        return updated;
    }


    private List<LatLng> updateVerticesOnResize(List<LatLng> ogVertices, int index,
                                                LatLng newVertex, boolean isRectangle) {
        List<LatLng> updated = new ArrayList<>(ogVertices);
        updated.remove(index);
        updated.add(index, newVertex);
        if (isRectangle) {
            LatLng topLeft = updated.get(0);
            LatLng topRight = updated.get(1);
            LatLng bottomRight = updated.get(2);
            LatLng bottomLeft = updated.get(3);
            switch (index) {      // which one has moved
                case 0:  {   //top left
                     topRight.setLat(newVertex.getLat());
                     bottomLeft.setLng(newVertex.getLng());
                     break;
                }
                case 1:  {  // top right
                    topLeft.setLat(newVertex.getLat());
                    bottomRight.setLng(newVertex.getLng());
                    break;
                }
                case 2:  {   // bottom right
                    topRight.setLng(newVertex.getLng());
                    bottomLeft.setLat(newVertex.getLat());
                    break;
                }
                case 3:  {   // bottom left
                    topLeft.setLng(newVertex.getLng());
                    bottomRight.setLat(newVertex.getLat());
                }
            }
            return List.of(topLeft, topRight, bottomRight, bottomLeft);
        }
        return updated;
    }


    private Marker drawMarker(LatLng location, boolean draggable, String label) {
        Marker marker = new Marker(location);
        marker.setDraggable(draggable);
        marker.setBubblingMouseEvents(false);
        if (StringUtils.isNotEmpty(label)) {
            marker.bindTooltip(createLabel(label));
        }
        return marker;
    }


    private Circle drawCircle(LatLng center, double radius, String color) {
        Circle circle = new Circle(center, radius);
        configureOverlay(circle, color);
        return circle;
    }

    
    private Polygon drawPolygon(List<LatLng> vertices, String color) {
        Polygon polygon = new Polygon(vertices);
        configureOverlay(polygon, color);

        polygon.onDoubleClick(e -> {
            LatLng latLng = e.getLatLng();
            if (isNearEdge(e.getLatLng(), getPolygonVertices(polygon))) { 
                _dblClickListeners.forEach(l ->
                        l.mapDoubleClick(new GeoMapDoubleClickEvent(
                                GeoMapDoubleClickEvent.EventType.OnPolygonEdge,
                                latLng.getLat(),  latLng.getLng())));
            }
        });

        return polygon;
    }

    private Marker createDraggableMarker(LatLng location, int ref, String iconName) {
        return createDraggableMarker(location, String.valueOf(ref), iconName);
    }

    
    private Marker createDraggableMarker(LatLng location, String refStr, String iconName) {
        Marker marker = new Marker(location);
        marker.setDraggable(true);
        marker.setTitle(iconName + "-marker-" + refStr);

        String iconPath = "icons/marker-" + iconName + ".png";
        Icon icon =  new Icon(iconPath, iconPath, null);
        icon.setIconSize(new Point(32,32));
        icon.setIconAnchor(new Point(16,16));
        marker.setIcon(icon);

        marker.setOpacity(0);

        marker.onMouseOver(event -> marker.setOpacity(1));
        marker.onMouseOut(event -> marker.setOpacity(0));
        return marker;
    }


    private List<Marker> createResizeMarkers(List<LatLng> vertices, int ref) {
        List<Marker> markers = new ArrayList<>();
        int i = 0;
        for (; i < vertices.size(); i++) {
            String refStr = ref + "-" + i;
            markers.add(createDraggableMarker(vertices.get(i), refStr, "resizeV"));
        }

        // add some spares for dynamically-user-added vertices
        for (int j = i; j < 20 + i; j++) {
            String refStr = ref + "-" + j;
            markers.add(createDraggableMarker(null, refStr, "resizeV"));
        }
        return markers;
    }


    private void updateResizeMarkerPositions(List<Marker> markers, List<LatLng> vertices) {
        int i = 0;
        for (; i < vertices.size(); i++) {
            Marker marker = markers.get(i);
            if (marker.getLatLng() == null) {    // a new coord, so add its marker
                marker.addTo(_markers);
            }
            marker.setLatLng(vertices.get(i));
        }
        for(; i < markers.size(); i++) {     // a removed coord, so hide the marker
            Marker marker = markers.get(i);
           if (_markers.hasLayer(marker)) {
               _markers.removeLayer(marker);
           }
           else break;                        // the rest are not shown on map
        }
    }


    private void updateMarkerMaps(Circle oldCircle, Circle newCircle) {
        Marker resizeMarker = _resizeMarkerMap.remove(oldCircle);
       _resizeMarkerMap.put(newCircle, resizeMarker);
       Marker dragMarker = _dragMarkerMap.remove(oldCircle);
       _dragMarkerMap.put(newCircle, dragMarker);
    }


    private void updateMarkerMaps(Polygon oldPolygon, Polygon newPolygon) {
        Marker dragMarker = _dragMarkerMap.remove(oldPolygon);
        _dragMarkerMap.put(newPolygon, dragMarker);
        List<Marker> resizeMarkers = _vertexMarkerMap.remove(oldPolygon);
        _vertexMarkerMap.put(newPolygon, resizeMarkers);
    }


    private int resize(Circle circle, LatLng resizePos) {
        LatLng center = circle.getLatlng();
        double radius = distanceInMeters(center, resizePos);
        return resize(circle, center, radius);
    }

    
    private int resize(Circle circle, LatLng center, double radius) {
        synchronized (MOUSE_EVENT_MUTEX) {
            circle.remove();
            Circle resizedCircle = drawCircle(center, radius, circle.getColor());
            int ref = updateOverlay(circle, resizedCircle);
            resizedCircle.setClassName("circle-" + ref);
            moveLabel(circle, resizedCircle);
            resizedCircle.addTo(_map);
            centerAndZoom();
            updateMarkerMaps(circle, resizedCircle);
            return ref;
        }
    }

    
    private int resize(Polygon polygon, List<LatLng> updated, int ref) {
        synchronized (MOUSE_EVENT_MUTEX) {
            erasePolygonFromBrowser(ref);       // clear client side polygon svg
            _map.removeLayer(polygon);          // clear server side polygon object
            Polygon resizedPolygon = drawPolygon(updated, polygon.getColor());
            updateOverlay(polygon, resizedPolygon);
            resizedPolygon.setClassName("polygon-" + ref);
            moveLabel(polygon, resizedPolygon);
            resizedPolygon.addTo(_map);
            centerAndZoom();
            updateMarkerMaps(polygon, resizedPolygon);
            return ref;
        }
    }


    
    private int drag(Circle circle, LatLng movedTo) {
        synchronized (MOUSE_EVENT_MUTEX) {
            circle.remove();
            Circle draggedCircle = drawCircle(movedTo, circle.getRadius(), circle.getColor());
            int ref = updateOverlay(circle, draggedCircle);
            draggedCircle.setClassName("circle-" + ref);
            moveLabel(circle, draggedCircle);
            draggedCircle.addTo(_map);
            centerAndZoom();
            updateMarkerMaps(circle, draggedCircle);
            return ref;
        }
    }


    private int drag(Polygon polygon, List<LatLng> vertices, int ref) {
        synchronized (MOUSE_EVENT_MUTEX) {
            erasePolygonFromBrowser(ref);        // clear client side polygon svg
            _map.removeLayer(polygon);           // clear server side polygon object

            // 3. Create and register the new one
            Polygon draggedPolygon = drawPolygon(vertices, polygon.getColor());
            updateOverlay(polygon, draggedPolygon);
            draggedPolygon.setClassName("polygon-" + ref);
            moveLabel(polygon, draggedPolygon);
            draggedPolygon.addTo(_map);
            centerAndZoom();
            updateMarkerMaps(polygon, draggedPolygon);
            return ref;
        }
    }


    private void configureOverlay(Path overlay, String color) {
        overlay.setBubblingMouseEvents(false);
        overlay.setColor(color);
        overlay.setFillOpacity(getFillOpacity());
        if (getFillOpacity() > 0) {
            overlay.setFillColor(color);
            overlay.setFill(true);
        }
        overlay.setWeight(getLineWeight());
    }


    private void moveLabel(InteractiveLayer oldLayer, InteractiveLayer newLayer) {
        Tooltip tooltip = oldLayer.getTooltip();
        if (tooltip != null) {
            newLayer.bindTooltip(tooltip);
        }
    }


    private boolean labelChanged(InteractiveLayer layer, String content) {
        if (! (layer == null || content == null)) {
            Tooltip label = layer.getTooltip();
            if (label == null) return true;        // no prev label, new content
            return !content.equals(label.getContent());
        }
        return false;
    }


    private void updateLabel(Circle circle, String newContent) {
        circle.remove();
        Circle newCircle = drawCircle(circle.getLatlng(),
                circle.getRadius(), circle.getColor());
        newCircle.bindTooltip(createLabel(newContent));
        int ref = updateOverlay(circle, newCircle);
        newCircle.setClassName("circle-" + ref);
        newCircle.addTo(_map);
        updateMarkerMaps(circle, newCircle);
    }


    private void updateLabel(Polygon polygon, String newContent) {
        polygon.remove();
        Polygon newPolygon = drawPolygon(getPolygonVertices(polygon), polygon.getColor());
        newPolygon.bindTooltip(createLabel(newContent));
        int ref = updateOverlay(polygon, newPolygon);
        newPolygon.setClassName("polygon-" + ref);
        newPolygon.addTo(_map);
        updateMarkerMaps(polygon, newPolygon);
    }


    private Tooltip createLabel(String text) {
        Tooltip tip = new Tooltip(text);
        tip.setPermanent(isLabelPermanent());
        tip.setDirection("bottom");
        tip.setOffset(new Point(0, 10));
        return tip;
    }

    
    private LatLng toLatLng(GeoCoordinate coordinate) {
        return new LatLng(coordinate.lat(), coordinate.lon());
    }


    private List<LatLng> toLatLngList(List<GeoCoordinate> coordinates) {
        List<LatLng> points = new ArrayList<>();
        for (GeoCoordinate coordinate : coordinates) {
            points.add(toLatLng(coordinate));
        }
        return points;
    }

    
    private GeoCoordinate toGeoCoordinate(LatLng point) {
        return new GeoCoordinate(point.getLat(), point.getLng());
    }


    private List<GeoCoordinate> toGeoCoordinateList(List<LatLng> points) {
        List<GeoCoordinate> coordinates = new ArrayList<>();
        for (LatLng point : points) {
            coordinates.add(toGeoCoordinate(point));
        }
        return coordinates;
    }


    private void centerAndZoom() {
        List<GeoCoordinate> points = new ArrayList<>();
        for (InteractiveLayer overlay : getOverlays()) {
            if (overlay instanceof Marker marker) {
                points.add(toGeoCoordinate(marker.getLatLng()));
            }
            else if (overlay instanceof Circle circle) {
                GeoCoordinate centerGeo = toGeoCoordinate(circle.getLatlng());
                GeoBounds bounds = getBounds(centerGeo, circle.getRadius());
                points.addAll(bounds.getPoints());
            }
            else if (overlay instanceof Polygon polygon) {
                List<LatLng> polyPoints = getPolygonVertices(polygon);
                points.addAll(toGeoCoordinateList(polyPoints));
           }
        }

        GeoBounds geoBounds = getBounds(points);
        LatLngBounds bounds = new LatLngBounds(toLatLng(geoBounds.getTopLeft()),
                toLatLng(geoBounds.getBottomRight())); 
        _map.fitBounds(bounds);
    }


    private LatLng getCentroid(List<LatLng> points) {
        double lat = 0;
        double lon = 0;

        for (LatLng p : points) {
            lat += p.getLat();
            lon += p.getLng();
        }

        int n = points.size();
        return new LatLng(lat / n, lon / n);
    }


    // scale edge click tolerance to map size
    private void scaleEdgeTolerance() {
        getVisibleMapWidthMeters(meters ->
                edgeToleranceMeters = Math.max(3, meters * 0.003));
    }


    private double distanceInMeters(LatLng a, LatLng b) {
        double R = 6378137.0; // Earth radius (m)

        double lat1 = Math.toRadians(a.getLat());
        double lat2 = Math.toRadians(b.getLat());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(b.getLng() - a.getLng());

        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        return 2 * R * Math.asin(Math.sqrt(h));
    }


    private LatLng offsetByMeters(LatLng origin, double northMeters, double eastMeters) {
        double R = 6378137.0;  // Earth radius (meters)

        double dLat = northMeters / R;
        double dLon = eastMeters / (R * Math.cos(Math.toRadians(origin.getLat())));

        double newLat = origin.getLat() + Math.toDegrees(dLat);
        double newLon = origin.getLng() + Math.toDegrees(dLon);

        return new LatLng(newLat, newLon);
    }

    
    private boolean hasMoved(LatLng oldPoint, LatLng newPoint) {
        return ! oldPoint.getLat().equals(newPoint.getLat()) ||
                ! oldPoint.getLng().equals(newPoint.getLng());
    }


    private Icon getNextMarkerIcon(Marker marker) {
        String nextColour = getNextColour();
        if (! DEFAULT_COLOR.equals(nextColour)) {
            String dataUri = getColouredMarkerIconAsDataUri(DEFAULT_MARKER_ICON_PATH, nextColour);
            if (dataUri != null) {
                _markerColorMap.put(marker, nextColour);
                return new Icon(dataUri, dataUri, null);
            }
        }
        return new Icon(DEFAULT_MARKER_ICON_PATH, DEFAULT_MARKER_ICON_PATH, null);
    }
            
    
    public String getColouredMarkerIconAsDataUri(String sourcePath, String nextColour) {
        try (InputStream is = VaadinService.getCurrent()
                        .getResourceAsStream("/" + DEFAULT_MARKER_ICON_PATH)) {
            BufferedImage img = ImageIO.read(is);
            BufferedImage coloredImg = new BufferedImage(img.getWidth(), img.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Color targetColor = Color.decode(nextColour);
            
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int rgb = img.getRGB(x, y);
                    if (rgb == -1) continue;         // ignore white
                    Color color = new Color(rgb, true);
                    if (color.getAlpha() > 0) {
                        Color pixelColor = new Color(targetColor.getRed(),
                                targetColor.getGreen(), targetColor.getBlue(),
                                color.getAlpha());
                        coloredImg.setRGB(x, y, pixelColor.getRGB());
                    }
                }
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(coloredImg, "png", os);
            byte[] bytes = os.toByteArray();

            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "data:image/png;base64," + base64;
        }
        catch (Exception e) {
            return null;                         // will default to standard icon
        }
    }


    private boolean isNearEdge(LatLng click, List<LatLng> vertices) {
        for (int i = 0; i < vertices.size(); i++) {
            LatLng a = vertices.get(i);
            LatLng b = vertices.get((i + 1) % vertices.size());

            double d = distancePointToSegmentMeters(click, a, b);
            if (d <= edgeToleranceMeters) {
                return true;
            }
        }
        return false;
    }

    
    private static double distancePointToSegmentMeters(LatLng p, LatLng a, LatLng b) {

        // Convert to meters using local projection
        double latRad = Math.toRadians(p.getLat());
        double metersPerDegLat = 111_320.0;
        double metersPerDegLon = metersPerDegLat * Math.cos(latRad);

        double px = p.getLng() * metersPerDegLon;
        double py = p.getLat() * metersPerDegLat;

        double ax = a.getLng() * metersPerDegLon;
        double ay = a.getLat() * metersPerDegLat;

        double bx = b.getLng() * metersPerDegLon;
        double by = b.getLat() * metersPerDegLat;

        double dx = bx - ax;
        double dy = by - ay;

        if (dx == 0 && dy == 0) {      // a == b
            return Math.hypot(px - ax, py - ay);
        }

        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        double cx = ax + t * dx;
        double cy = ay + t * dy;

        return Math.hypot(px - cx, py - cy);
    }


    public void getVisibleMapWidthMeters(Consumer<Double> callback) {
        try {
            _map.getBounds().whenComplete((b, e) -> {
                if (b == null || e != null) {
                    return;
                }
                callback.accept(getVisibleMapWidthMeters(b));
            });
        }
        catch (Exception e) {
            // safely ignore
        }
    }    
    
    private double getVisibleMapWidthMeters(LatLngBounds bounds) {
        double centerLat = (bounds.getSouthWest().getLat() + bounds.getNorthEast().getLat()) / 2.0;

        LatLng west = new LatLng(centerLat, bounds.getSouthWest().getLng());
        LatLng east = new LatLng(centerLat, bounds.getNorthEast().getLng());

        return distanceInMeters(west, east);
    }


    private List<LatLng> sortClockwise(List<LatLng> points) {
        LatLng c = getCentroid(points);

        points.sort((a, b) -> {
            double angleA = Math.atan2(
                    a.getLat() - c.getLat(),
                    a.getLng() - c.getLng()
            );
            double angleB = Math.atan2(
                    b.getLat() - c.getLat(),
                    b.getLng() - c.getLng()
            );

            // Clockwise order
            return Double.compare(angleB, angleA);
        });

        return points;
    }


    private List<LatLng> recenterPolygon(List<LatLng> points, LatLng targetCenter) {
        LatLng currentCenter = getCentroid(points);
        double dLat = targetCenter.getLat() - currentCenter.getLat();
        double dLon = targetCenter.getLng() - currentCenter.getLng();
        List<LatLng> result = new ArrayList<>(points.size());

        for (LatLng p : points) {
            result.add(new LatLng(p.getLat() + dLat, p.getLng() + dLon));
        }

        return result;
    }


    public List<GeoCoordinate> centerPolygon(List<GeoCoordinate> points,
                                               GeoCoordinate targetCenter) {
        List<LatLng> result = recenterPolygon(toLatLngList(points), toLatLng(targetCenter));
        return toGeoCoordinateList(result);
    }


    private boolean latLngEquals(LatLng p1, LatLng p2) {
        if (p1 == p2) return true;
        if (p1 == null || p2 == null) return false;

        double epsilon = 0.000001; // Roughly 10cm precision
        return Math.abs(p1.getLat() - p2.getLat()) < epsilon &&
                Math.abs(p1.getLng() - p2.getLng()) < epsilon;
    }
    

    private boolean verticesEqual(List<LatLng> list1, List<LatLng> list2) {
        if (list1 == list2) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;

        // Compare each point in order
        for (int i = 0; i < list1.size(); i++) {
            if (!latLngEquals(list1.get(i), list2.get(i))) {
                return false;
            }
        }

        return true;
    }

    
    private void erasePolygonFromBrowser(int ref) {
        _map.getElement().executeJs(
            "var map = this._leafletMap || this.map; " +
            "map.eachLayer(l => { if(l.options && l.options.className === 'polygon-" +
                    ref + "') map.removeLayer(l); });"
        );
    }


    private String getCircleScript(int ref) {
        return "const self = this; const refId = " + ref + "; " +
               "const glue = () => { " +
               "  const map = self._leafletMap || self.map; if (!map) return; " +
               "  let mDrag, mResize, circle; " +

               "  map.eachLayer(l => { " +
               "    if (l.options) { " +
               "      if (l.options.title === 'drag-marker-' + refId) mDrag = l; " +
               "      if (l.options.title === 'resize-marker-' + refId) mResize = l; " +
               "      if (l.options.className === 'circle-' + refId) { " +
               "         if (circle) map.removeLayer(l); /* Kill duplicates */ " +
               "         else circle = l; " +
               "      } " +
               "      if (l.options && l.options.title) { " +
               "        setTimeout(() => { " +
               "          const el = l.getElement(); " +
               "          if (el) el.removeAttribute('title');" +
               "        }, 100);" +
               "      } " +
               "    } " +
               "  }); " +
               "  if (mDrag && circle) { " +
               "    const tooltip = circle.getTooltip(); " +
               "    /* Suppress browser hover text */ " +
               "    [mDrag, mResize].forEach(m => { if(m && m.getElement()) m.getElement().removeAttribute('title'); }); " +

               "    mDrag.off('drag').on('drag', e => { " +
               "      const newPos = e.latlng; " +
               "      circle.setLatLng(newPos); " +
               "      if (tooltip) tooltip.setLatLng(newPos); " +
               "      /* Keep resize marker on the edge (offset) */ " +
               "      if (mResize) { " +
               "        const oldC = circle.getLatLng(); " +
               "        const rPos = mResize.getLatLng(); " +
               "        mResize.setLatLng([newPos.lat + (rPos.lat - oldC.lat), newPos.lng + (rPos.lng - oldC.lng)]); " +
               "      } " +
               "    }); " +

               "    if (mResize) { " +
               "      mResize.off('drag').on('drag', e => { " +
               "        const d = map.distance(circle.getLatLng(), e.latlng); " +
               "        circle.setRadius(d); " +
               "        if (tooltip) tooltip.setLatLng(circle.getLatLng()); " +
               "      }); " +
               "    } " +
               "  } " +
               "}; " +

               "if (!self['_c_sub_' + refId]) { " +
               "  const map = self._leafletMap || self.map; " +
               "  map.on('layeradd', (e) => { " +
               "    if (e.layer.options && e.layer.options.className === 'circle-' + refId) setTimeout(glue, 50); " +
               "  }); " +
               "  self['_c_sub_' + refId] = true; " +
               "} " +
               "setTimeout(glue, 200);" ;
    }


    private String getPolygonScript(int ref, boolean isRectangle) {
        return "const self = this; const refId = " + ref + "; " +
               "const glue = () => { " +
               "  const map = self._leafletMap || self.map; if (!map) return; " +
               "  let mDrag, poly, mResize = []; " +
               "  map.eachLayer(l => { " +
               "    if (l.options) { " +
               "      if (l.options.title === 'drag-marker-' + refId) mDrag = l; " +
               "      if (l.options.title && l.options.title.startsWith('resizeV-marker-' + refId + '-')) { " +
               "         const parts = l.options.title.split('-'); " +
               "         l._idx = parseInt(parts[parts.length - 1]); " +
               "         mResize.push(l); " +
               "      } " +
               "      if (l.options.className === 'polygon-' + refId) poly = l; " +
               "      if (l.options && l.options.title) { " +
               "        setTimeout(() => { " +
               "          const el = l.getElement(); " +
               "          if (el) el.removeAttribute('title');" +
               "        }, 100);" +
               "      } " +
               "    } " +
               "  }); " +
               "  if (mDrag && poly) { " +
               "    const tooltip = poly.getTooltip();" +
               "    mDrag.off('drag dragstart'); " + // CLEAR ALL OLD
               "    mDrag.on('dragstart', e => { mDrag._lastPos = e.target.getLatLng(); }); " +
               "    mDrag.on('drag', e => { " +
               "      const newPos = e.latlng; " +
               "      const lastPos = mDrag._lastPos || mDrag.getLatLng(); " +
               "      const dLat = newPos.lat - lastPos.lat; " +
               "      const dLng = newPos.lng - lastPos.lng; " +
               "      " +
               "      let latlngs = poly.getLatLngs(); " +
               "      /* Leaflet Polygons can be [[[p,p]]] or [[p,p]]. We flatten to the first ring. */ " +
               "      let ring = Array.isArray(latlngs[0]) ? latlngs[0] : latlngs; " +
               "      " +
               "      ring.forEach(ll => { ll.lat += dLat; ll.lng += dLng; }); " +
               "      poly.setLatLngs(latlngs); " +
               "      " +
               "      mResize.forEach(m => { " +
               "        let p = m.getLatLng(); " +
               "        if(p) m.setLatLng([p.lat + dLat, p.lng + dLng]); " +
               "      }); " +
               "      mDrag._lastPos = newPos; " +
               "      if (tooltip) {" +
               "        tooltip.setLatLng(e.latlng);" +
               "      }" +
               "    }); " +
               "    mResize.forEach(m => { " +
               "      m.off('drag').on('drag', e => { " +
               "        let latlngs = poly.getLatLngs(); " +
               "        let ring = Array.isArray(latlngs[0]) ? latlngs[0] : latlngs; " + // Handle nesting
               "        " +
               "        if (" + isRectangle + ") { " +
               "          const i = m._idx; " +
               "          const next = (i + 1) % 4; " +
               "          const prev = (i + 3) % 4; " +
               "          " +
               "          /* Corner i is moving. Adjust neighbors to stay rectangular */ " +
               "          /* Vertices are typically: 0:TopLeft, 1:TopRight, 2:BottomRight, 3:BottomLeft */ " +
               "          if (i % 2 === 0) { " +
               "             ring[next].lat = e.latlng.lat; " +
               "             ring[prev].lng = e.latlng.lng; " +
               "          } else { " +
               "             ring[next].lng = e.latlng.lng; " +
               "             ring[prev].lat = e.latlng.lat; " +
               "          } " +
               "        } " +
               "        " +
               "        ring[m._idx] = e.latlng; " +
               "        poly.setLatLngs(latlngs); " +
               "        " +
               "        /* 1. CALCULATE NEW CENTROID MANUALLY */ " +
               "        let sumLat = 0, sumLng = 0; " +
               "        ring.forEach(ll => { sumLat += ll.lat; sumLng += ll.lng; }); " +
               "        const newCenter = [sumLat / ring.length, sumLng / ring.length]; " +
               "        " +
               "        /* 2. MOVE DRAG MARKER & TOOLTIP */ " +
               "        if (mDrag) { " +
               "           mDrag.setLatLng(newCenter); " +
               "           mDrag._lastPos = L.latLng(newCenter[0], newCenter[1]); " +
               "        } " +
               "        const tt = poly.getTooltip(); " +
               "        if (tt) tt.setLatLng(newCenter); " +
               "        " +
               "        /* Visually move the other markers so they don't get left behind */ " +
               "        if (" + isRectangle + ") { " +
               "           mResize.forEach((rm, idx) => { rm.setLatLng(ring[idx]); }); " +
               "        } " +
               "      }); " +
               "    }); " +
               "  } " +
               "}; " +
               "const map = self._leafletMap || self.map; " +
               "if (map && !self['_poly_sub_' + refId]) { " +
               "  map.on('layeradd', (e) => { " +
               "    /* Only trigger glue if the added layer is relevant to this ref */ " +
               "    if (e.layer.options && (e.layer.options.className === 'polygon-' + refId || (e.layer.options.title && e.layer.options.title.includes('-' + refId)))) { " +
               "       setTimeout(glue, 50); " +
               "    } " +
               "  }); " +
               "  self['_poly_sub_' + refId] = true; " +
               "} " +
               "setTimeout(glue, 200);" ;
    }
    
}
