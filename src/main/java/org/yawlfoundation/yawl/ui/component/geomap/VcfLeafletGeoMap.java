package org.yawlfoundation.yawl.ui.component.geomap;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import org.vaadin.addons.componentfactory.leaflet.LeafletMap;
import org.vaadin.addons.componentfactory.leaflet.layer.InteractiveLayer;
import org.vaadin.addons.componentfactory.leaflet.layer.groups.LayerGroup;
import org.vaadin.addons.componentfactory.leaflet.layer.map.options.DefaultMapOptions;
import org.vaadin.addons.componentfactory.leaflet.layer.map.options.MapOptions;
import org.vaadin.addons.componentfactory.leaflet.layer.raster.TileLayer;
import org.vaadin.addons.componentfactory.leaflet.layer.ui.marker.Marker;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    
    private final LeafletMap _map;
    private final LayerGroup _markers;
    private final Map<InteractiveLayer, Marker> _dragMarkerMap = new HashMap<>();
    private final Map<InteractiveLayer, Marker> _resizeMarkerMap = new HashMap<>();
    private final Map<InteractiveLayer, List<Marker>> _vertexMarkerMap = new HashMap<>();
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
    public int drawMarker(GeoCoordinate coordinate, boolean draggable) {
        LatLng point = toLatLng(coordinate);
        Marker marker = new Marker(point);
        marker.setDraggable(draggable);
        marker.setIcon(getNextMarkerIcon());

        marker.onMouseMove(event -> announceMove(marker, event.getLatLng()));
        marker.onMouseUp(event -> announceMove(marker, event.getLatLng()));

        marker.addTo(_markers);
        int ref = addOverlay(marker);
        centerAndZoom();
        return ref;
    }


    @Override
    public synchronized void updateMarker(int ref, GeoCoordinate coordinate) {
        Marker marker = (Marker) getOverlay(ref);
        marker.setLatLng(toLatLng(coordinate));
        centerAndZoom();
    }

    
    @Override
    public void removeMarker(int ref) { }

    
    @Override
    public int drawCircle(GeoCoordinate coordinate, double radius, boolean draggable) {
        LatLng center = toLatLng(coordinate);
        Circle circle = drawCircle(center, radius, getNextColour());
        final int ref = addOverlay(circle);
        circle.addTo(_map);
        _map.getUI().ifPresent(ui -> ui.access(_map::invalidateSize));

        if (draggable) {
            Marker dragMarker = createDraggableMarker(center, "drag");
            LatLng resizePos = offsetByMeters(center, 0, radius);
            Marker resizeMarker = createDraggableMarker(resizePos, "resize");

            AtomicBoolean dragging = new AtomicBoolean(false);
            AtomicBoolean resizing = new AtomicBoolean(false);
            final long[] last = { 0 };
            final int[] count = { 0 };

            dragMarker.onMouseDown(event -> dragging.set(true));
            resizeMarker.onMouseDown(event -> resizing.set(true));

            _map.onMouseMove(event -> {
                long now = System.currentTimeMillis();
                if (++count[0] % MOUSE_MOVE_STEP == 0 || now - last[0] > MOUSE_MOVE_MIN_MS) {
                    last[0] = now;

                    if (dragging.get()) {
                        Circle refCircle = (Circle) getOverlay(ref);
                        drag(refCircle, event.getLatLng());
                        announceMove((Circle) getOverlay(ref), event.getLatLng(), ref);
                    }
                    if (resizing.get()) {
                        Circle refCircle = (Circle) getOverlay(ref);
                        resize(refCircle, event.getLatLng());
                        announceMove((Circle) getOverlay(ref), event.getLatLng(), ref);
                    }
                }
            });

            _map.onMouseUp(event -> {
                if (dragging.get()) {
                    Circle refCircle = (Circle) getOverlay(ref);
                    drag(refCircle, event.getLatLng());           // one last time
                    finaliseCircleMove(ref, true);
                    dragging.set(false);
                }
                if (resizing.get()) {
                    Circle refCircle = (Circle) getOverlay(ref);
                    resize(refCircle, event.getLatLng());
                    finaliseCircleMove(ref, true);
                    resizing.set(false);
                }
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
    public synchronized void updateCircle(int ref, GeoCoordinate coordinate, double radius) {
        Circle oldCircle = (Circle) getOverlay(ref);
        LatLng oldCenter = oldCircle.getLatlng();
        LatLng updatedCenter = toLatLng(coordinate);

        if (Objects.equals(updatedCenter.getLat(), oldCenter.getLat()) &&
                Objects.equals(updatedCenter.getLng(), oldCenter.getLng())) {

            // center unchanged, must be a radius update
            resize(oldCircle, oldCenter, radius);
        }
        else {
            drag(oldCircle, updatedCenter);
        }
        finaliseCircleMove(ref, false);
    }


    @Override
    public void removeCircle(int ref) { }

    
    private void finaliseCircleMove(int ref, boolean announce) {
        Circle circle = (Circle) getOverlay(ref);
        if (announce) {
            announceMove(circle, circle.getLatlng(), ref);
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
            announceMove(polygon, vertices, ref);
        }

        _dragMarkerMap.get(polygon).setLatLng(getCentroid(vertices));
        updateResizeMarkerPositions(resizeMarkers, vertices);
        _map.getUI().ifPresent(ui -> ui.access(_map::invalidateSize));
    }


    @Override
    public int drawRectangle(GeoCoordinate topLeft, GeoCoordinate bottomRight, boolean draggable) {
        return drawPolygon(toRectangleVertices(topLeft, bottomRight), true, draggable);
    }

    
    @Override
    public void updateRectangle(int ref, GeoCoordinate topLeft, GeoCoordinate bottomRight) {
        updatePolygon(ref, toRectangleVertices(topLeft, bottomRight));
    }


    @Override
    public void removeRectangle(int ref) { }


    @Override
    public int drawPolygon(List<GeoCoordinate> coordinates, boolean draggable) {
        return drawPolygon(coordinates, false, draggable);
    }


    private int drawPolygon(List<GeoCoordinate> coordinates, boolean isRectangle, boolean draggable) {
        List<LatLng> vertices = sortClockwise(toLatLngList(coordinates));
        Polygon polygon = drawPolygon(vertices, getNextColour());
        int ref = addOverlay(polygon);
        polygon.addTo(_map);
        _map.getUI().ifPresent(ui -> ui.access(_map::invalidateSize));

        if (draggable) {
            AtomicReference<LatLng> centroid = new AtomicReference<>(getCentroid(vertices));
            Marker dragMarker = createDraggableMarker(centroid.get(), "drag");
            List<Marker> resizeMarkers = createResizeMarkers(vertices);

            AtomicBoolean dragging = new AtomicBoolean(false);
            AtomicBoolean resizing = new AtomicBoolean(false);
            AtomicInteger resizingMarkerIndex = new AtomicInteger(-1);
            final long[] last = { 0 };
            final int[] count = { 0 };

            dragMarker.onMouseDown(event -> {
                resetPolygonVertices(ref, vertices);
                centroid.set(getCentroid(vertices));
                dragging.set(true);
            });

            for (int i = 0; i < resizeMarkers.size(); i++) {
                Marker m = resizeMarkers.get(i);
                int index = i;
                m.onMouseDown(event -> {
                    resetPolygonVertices(ref, vertices);
                    centroid.set(getCentroid(vertices));
                    resizing.set(true);
                    resizingMarkerIndex.set(index);
                });
            }

            _map.onMouseMove(event -> {
                long now = System.currentTimeMillis();
                if (++count[0] % MOUSE_MOVE_STEP == 0 || now - last[0] > MOUSE_MOVE_MIN_MS) {
                    last[0] = now;

                    if (dragging.get()) {
                        Polygon refPolygon = (Polygon) getOverlay(ref);
                        LatLng newCentroid = event.getLatLng();
                        List<LatLng> moved = updateVerticesOnMove(vertices, centroid, newCentroid);
                        drag(refPolygon, moved);
                        announceMove((Polygon) getOverlay(ref), moved, ref);
                        vertices.clear();
                        vertices.addAll(moved);
                        centroid.set(newCentroid);
                    }
                    if (resizing.get()) {
                        Polygon refPolygon = (Polygon) getOverlay(ref);
                        LatLng newVertex = event.getLatLng();
                        List<LatLng> updated = updateVerticesOnResize(vertices,
                                resizingMarkerIndex.get(), newVertex, isRectangle);
                        resize(refPolygon, updated);
                        announceMove(refPolygon, updated, ref);
                        vertices.clear();
                        vertices.addAll(updated);
                        centroid.set(getCentroid(vertices));
                    }
                }
            });

            _map.onMouseUp(event -> {
                if (dragging.get()) {
                    Polygon refPolygon = (Polygon) getOverlay(ref);
                    LatLng newCentroid = event.getLatLng();
                    List<LatLng> moved = updateVerticesOnMove(vertices, centroid, newCentroid);
                    drag(refPolygon, moved);
                    vertices.clear();
                    vertices.addAll(moved);
                    centroid.set(newCentroid);
                    finalisePolygonMove(ref, vertices, resizeMarkers, true);
                    dragging.set(false);
                }
                if (resizing.get()) {
                    Polygon refPolygon = (Polygon) getOverlay(ref);
                    LatLng newVertex = event.getLatLng();
                    List<LatLng> updated = updateVerticesOnResize(vertices,
                            resizingMarkerIndex.get(), newVertex, isRectangle);
                    resize(refPolygon, updated);
                    vertices.clear();
                    vertices.addAll(updated);
                    centroid.set(getCentroid(vertices));
                    finalisePolygonMove(ref, vertices, resizeMarkers, true);
                    resizing.set(false);
                }
            });

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


    public synchronized void updatePolygon(int ref, List<GeoCoordinate> coordinates) {
       Polygon polygon = (Polygon) getOverlay(ref);
       List<LatLng> vertices = sortClockwise(toLatLngList(coordinates));
       resize(polygon, vertices);
       finalisePolygonMove(ref, vertices, _vertexMarkerMap.get(getOverlay(ref)), false);
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
       return new ArrayList<>(array.get(0));
   }


    private void announceMove(Marker marker, LatLng movedTo) {
        if (hasMoved(marker.getLatLng(), movedTo)) {
            GeoMapOverlayMovedEvent moveEvent = new GeoMapOverlayMovedEvent(
                    toGeoCoordinate(movedTo), getOverlayRef(marker));
            _moveListeners.forEach(l -> l.overlayMoved(moveEvent));
        }
    }

    private void announceMove(Circle circle, LatLng movedTo, int ref) {
        GeoMapOverlayMovedEvent moveEvent = new GeoMapOverlayMovedEvent(
                toGeoCoordinate(movedTo), circle.getRadius(), ref);
        _moveListeners.forEach(l -> l.overlayMoved(moveEvent));
    }


    private void announceMove(Polygon polygon, List<LatLng> movedTo, int ref) {
        GeoMapOverlayMovedEvent moveEvent = new GeoMapOverlayMovedEvent(
               toGeoCoordinateList(movedTo), ref);
        _moveListeners.forEach(l -> l.overlayMoved(moveEvent));
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


    private Marker createDraggableMarker(LatLng location, String iconName) {
        Marker marker = new Marker(location);
        marker.setDraggable(true);

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


    private List<Marker> createResizeMarkers(List<LatLng> vertices) {
        List<Marker> markers = new ArrayList<>();
        for (LatLng vertex : vertices) {
            markers.add(createDraggableMarker(vertex, "resizeV"));
        }

        // add some spares for dynamically-user-added vertices
        for (int i = 0; i < 20; i++) {
            markers.add(createDraggableMarker(null, "resizeV"));
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
            resizedCircle.addTo(_map);
            int ref = updateOverlay(circle, resizedCircle);
            centerAndZoom();
            updateMarkerMaps(circle, resizedCircle);
            return ref;
        }
    }


    private int resize(Polygon polygon, List<LatLng> vertices) {
        synchronized (MOUSE_EVENT_MUTEX) {
            polygon.remove();
            Polygon resizedPolygon = drawPolygon(vertices, polygon.getColor());
            resizedPolygon.addTo(_map);
            int ref = updateOverlay(polygon, resizedPolygon);
            centerAndZoom();
            updateMarkerMaps(polygon, resizedPolygon);
            return ref;
        }
    }

    
    private int drag(Circle circle, LatLng movedTo) {
        synchronized (MOUSE_EVENT_MUTEX) {
            circle.remove();
            Circle draggedCircle = drawCircle(movedTo, circle.getRadius(), circle.getColor());
            draggedCircle.addTo(_map);
            int ref = updateOverlay(circle, draggedCircle);
            centerAndZoom();
            updateMarkerMaps(circle, draggedCircle);
            return ref;
        }
    }


    private int drag(Polygon polygon, List<LatLng> vertices) {
        synchronized (MOUSE_EVENT_MUTEX) {
            polygon.remove();
            Polygon draggedPolygon = drawPolygon(vertices, polygon.getColor());
            draggedPolygon.addTo(_map);
            int ref = updateOverlay(polygon, draggedPolygon);
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
            if (overlay instanceof Marker) {
                Marker marker = (Marker) overlay;
                points.add(toGeoCoordinate(marker.getLatLng()));
            }
            else if (overlay instanceof Circle) {
                Circle circle = (Circle) overlay;
                GeoCoordinate centerGeo = toGeoCoordinate(circle.getLatlng());
                GeoBounds bounds = getBounds(centerGeo, circle.getRadius());
                points.addAll(bounds.getPoints());
            }
            else if (overlay instanceof Polygon) {
                Polygon polygon = (Polygon) overlay;
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


    private Icon getNextMarkerIcon() {
        Icon defaultIcon = new Icon("icons/marker-icon.png");
        try {
            String nextColour = getNextColour();
            if (Objects.equals(nextColour, DEFAULT_COLOR)) {
                return defaultIcon;
            }
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream is = classLoader.getResourceAsStream("../../icons/marker-icon.png");
            BufferedImage img = ImageIO.read(is);
            BufferedImage coloredImg = new BufferedImage(img.getWidth(), img.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Color targetColor = Color.decode(nextColour);
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int rgb = img.getRGB(x, y);
                    Color color = new Color(rgb, true);
                    if (color.getAlpha() > 0) {
                        Color pixelColor = new Color(targetColor.getRed(),
                                targetColor.getGreen(), targetColor.getBlue(), color.getAlpha());
                        coloredImg.setRGB(x, y, pixelColor.getRGB());
                    }
                }
            }

            StreamResource iconResource = new StreamResource(
                    "temp-marker-icon.png",
                    () -> {
                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(coloredImg, "png", baos);
                            return new ByteArrayInputStream(baos.toByteArray());
                        }
                        catch (Exception e) {
                            return null;
                        }
                    }
            );

            URI uri = VaadinSession.getCurrent()
                    .getResourceRegistry()
                    .registerResource(iconResource).getResourceUri();

            return new Icon(uri.toString());
            
        }
        catch (Exception e) {
            return defaultIcon;
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
    
}
