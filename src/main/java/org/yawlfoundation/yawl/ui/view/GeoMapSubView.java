package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.geomap.GeoCoordinate;
import org.yawlfoundation.yawl.ui.component.geomap.VcfLeafletGeoMap;
import org.yawlfoundation.yawl.ui.dynform.DynForm;
import org.yawlfoundation.yawl.ui.dynform.DynFormLayout;
import org.yawlfoundation.yawl.ui.dynform.SubPanel;
import org.yawlfoundation.yawl.ui.listener.*;
import org.yawlfoundation.yawl.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * @author Michael Adams
 * @date 12/12/25
 */
public class GeoMapSubView extends AbstractView
        implements DynFormContentChangeListener, GeoMapOverlayMoveListener, GeoMapDoubleClickListener {

    private final VcfLeafletGeoMap _map = new VcfLeafletGeoMap();
    private final List<SubPanel> _geoPanels;
    private final Map<DynFormLayout, Integer> _layout2OverlayMap = new HashMap<>();
    private final Map<DynFormLayout, List<SubPanel>> _layout2ChildPanelMap = new HashMap<>();

    private boolean _updatingOverlays = false;

    public GeoMapSubView(DynForm form) {
        super();
        form.addGeoTypeChangeListener(this);
        _map.addMoveListener(this);
        _map.addDoubleClickListener(this);
        _geoPanels = gatherGeoPanels(form.getSubPanels());
        setSizeFull();
    }


    @Override
    Component createLayout() {
        Component map = _map.getMap();
        expand(map);
        initOverlays();
        return map;
    }


    @Override
    public void formContentChanged(DynFormContentChangedEvent event) {
        if (event.eventType() == DynFormContentChangedEvent.EventType.SUB_PANEL_ADDED) {
            SubPanel subPanel = event.getSubPanel();
            if (subPanel.getForm().isGeoDataType()) {
                _layout2OverlayMap.put(event.getSubPanel().getForm(), -1);
                _geoPanels.add(subPanel);
            }
        }
        else if (event.eventType() == DynFormContentChangedEvent.EventType.SUB_PANEL_REMOVED) {
            SubPanel subPanel = event.getSubPanel();
            DynFormLayout layout = subPanel.getForm();
            if (isVertexPanel(subPanel)) {

                // required because when we get the change event the subpanel is
                // already detached
                for (DynFormLayout key : _layout2ChildPanelMap.keySet()) {
                    List<SubPanel> childPanels = _layout2ChildPanelMap.get(key);
                    if (childPanels.contains(subPanel)) {
                        childPanels.remove(subPanel);
                        layout = key;
                        break;
                    }
                }
            }
            int ref = _layout2OverlayMap.remove(layout);
            if (ref != -1) {
                _map.removeOverlay(ref);
                _geoPanels.remove(subPanel);
            }
            if (isVertexPanel(subPanel)) {
                drawPolygon(layout);
            }
        }
        else {                          // value change
            if (_updatingOverlays) {    // if caused by dragging an overlay, ignore
                return;
            }
            if (!validateChangedValue(event.getFieldName(), event.getNewValue())) {
                return;
            }
            String layoutName = event.getVarName() != null ?
                    event.getVarName() : event.getPanelName();
            for (DynFormLayout layout : _layout2OverlayMap.keySet()) {
                if (layout.getName().equals(layoutName)) {
                    drawOverlay(layout);
                    break;
                }
            }
        }
    }


    @Override
    public void overlayMoved(GeoMapOverlayMovedEvent event) {
        _updatingOverlays = true;
        for (DynFormLayout layout : _layout2OverlayMap.keySet()) {
            int ref = _layout2OverlayMap.get(layout);
            if (ref == event.getRef()) {
                switch (event.getOverlayType()) {
                    case Marker: updateMarkerValues(layout, event.getPoint()); break;
                    case Circle: updateCircleValues(layout, event.getPoint(), event.getRadius()); break;
                    case Polygon: updatePolygonValues(layout, event.getPoints(), true); break;
                }
            }
        }
        _updatingOverlays = false;
    }

    @Override
    public void mapDoubleClick(GeoMapDoubleClickEvent event) {
        _updatingOverlays = true;
        DynFormLayout layout = findFirstEmptyLayout();
        if (layout != null) {
            GeoCoordinate coordinate = new GeoCoordinate(event.getLatitude(), event.getLongitude());
            switch (layout.getDataType()) {
                case "YGeoLatLongType":
                case "YGeoLatLongListType": {
                    updateMarkerValues(layout, coordinate);
                    drawMarker(layout);
                } break;
                case "YGeoCircleType" :
                case "YGeoCircleListType" : {
                    getReasonableWidthForNewOverlay(meters -> {
                        updateCircleValues(layout, coordinate, meters / 2);
                        drawCircle(layout);
                    });
                } break;
                case "YGeoRectType" :
                case "YGeoRectListType" : {
                    handleRectDblClick(layout, coordinate);
                } break;
                case "YGeoPolygonType" :
                case "YGeoPolygonListType" : {
                    handlePolygonDblClick(event, coordinate);
                } break;
            }
 //           _map.zoomOut();              // undo the dblClick zoom in
        }
        _updatingOverlays = false;
    }

    public void invalidateSize() {
        _map.invalidateSize();
    }


    private void updateMarkerValues(DynFormLayout layout, GeoCoordinate location) {
        updateForm(layout, location);
    }


    private void updateCircleValues(DynFormLayout layout, GeoCoordinate centre, double radius) {
        SubPanel panel = extractSubPanel(layout);
        updateForm(panel.getForm(), centre);
        List<TextField> fields = extractFields(layout);
        fields.get(0).setValue(String.valueOf(radius));
    }


    private void updatePolygonValues(DynFormLayout layout,
                                     List<GeoCoordinate> vertices,
                                     boolean isDragging) {
        List<SubPanel> subPanels = extractSubPanels(layout);
        if (layout.getDataType().contains("Rect")) {     // rectangle
            updateForm(subPanels.get(0).getForm(), vertices.get(0));      // top left
            updateForm(subPanels.get(1).getForm(), vertices.get(2));      // bot right
        }
        else {
            int i = 0;
            for (SubPanel subPanel : subPanels) {
                DynFormLayout subLayout = subPanel.getForm();
                if (isDragging && isBlankForm(subLayout)) {
                    continue;
                }
                updateForm(subLayout, vertices.get(i++));
            }
        }
    }

    
    private void updateForm(DynFormLayout layout, GeoCoordinate location) {
        for (TextField field : extractFields(layout)) {
            if (field.getLabel().equals("latitude")) {
                field.setValue(String.valueOf(location.lat()));
            }
            if (field.getLabel().equals("longitude")) {
                field.setValue(String.valueOf(location.normalisedLon()));
            }
        }
    }


    private void handleRectDblClick(DynFormLayout layout, GeoCoordinate coordinate) {
        getReasonableWidthForNewOverlay(meters -> {
            List<GeoCoordinate> coordinates = new ArrayList<>();
            double negMeters = meters * -1;
            coordinates.add(offsetByMeters(coordinate, meters, negMeters));
            coordinates.add(offsetByMeters(coordinate, meters, meters));
            coordinates.add(offsetByMeters(coordinate, negMeters, meters));
            coordinates.add(offsetByMeters(coordinate, negMeters, negMeters));
            updatePolygonValues(layout, coordinates, false);
            drawRectangle(layout);
        });
    }

    private void handlePolygonDblClick(GeoMapDoubleClickEvent event, GeoCoordinate coordinate) {
        boolean isNewPolygon = event.getType() == GeoMapDoubleClickEvent.EventType.OnNewLocation;
        final DynFormLayout layout = findFirstEmptyPolygonLayout(isNewPolygon);
        if (layout != null) {
            if (isNewPolygon) {
                int vertexCount = extractSubPanels(layout).size();
                List<GeoCoordinate> coordinates = new ArrayList<>();
                getReasonableWidthForNewOverlay(meters -> {
                    double min = meters * -1;
                    double max = meters;
                    for (int i = 0; i < vertexCount; i++) {
                        double north = ThreadLocalRandom.current().nextDouble(min, max + 1);
                        double east = ThreadLocalRandom.current().nextDouble(min, max + 1);
                        coordinates.add(offsetByMeters(coordinate, north, east));
                    }
                    List<GeoCoordinate> centeredCoordinates = _map.centerPolygon(coordinates, coordinate);
                    updatePolygonValues(layout, centeredCoordinates, false);
                    drawPolygon(layout);
                });
            }
            else {     // new vertex
                double offset = _map.getEdgeToleranceMeters();
                updateForm(layout, offsetByMeters(coordinate, offset, offset));
                _layout2OverlayMap.remove(layout);
                drawPolygon(layout.getParentLayout());
            }
        }
    }


    private int getOverlayRef(DynFormLayout layout) {
        if (_layout2OverlayMap.containsKey(layout)) {
            return _layout2OverlayMap.get(layout);
        }
        return -1;
    }
    
    
    // a marker is presented as a layout with two subfields
    private void drawMarker(DynFormLayout layout) {
        List<TextField> fields = extractFields(layout);
        GeoCoordinate coordinate = extractCoordinate(fields);
        int ref = getOverlayRef(layout);
        if (validateCoordinate(coordinate)) {
            if (ref == -1) {
                boolean draggable = ! fields.get(0).isReadOnly();
                ref = _map.drawMarker(coordinate, draggable);
             }
            else {
                _map.updateMarker(ref, coordinate);
            }
        }
        _layout2OverlayMap.put(layout, ref);
    }


    // a circle is presented as a subpanel with a field and two subfields
    private void drawCircle(DynFormLayout layout) {
        SubPanel panel = extractSubPanel(layout);
        GeoCoordinate coordinate = extractCoordinate(panel);
        int ref = getOverlayRef(layout);
        if (validateCoordinate(coordinate)) {
            List<TextField> fields = extractFields(layout);
            double radius = StringUtil.strToDouble(fields.get(0).getValue(), 0);
            if (radius > 0) {
                if (ref == -1) {       // new circle
                    boolean draggable = ! fields.get(0).isReadOnly();
                    ref = _map.drawCircle(coordinate, radius, draggable);
                }
                else {
                    _map.updateCircle(ref, coordinate, radius);
                }
            }
            else {
                Announcement.show("Radius must be greater than 0");
            }
        }
        _layout2OverlayMap.put(layout, ref);
    }


    // a rectangle is presented as two subpanels, each with two fields
    private void drawRectangle(DynFormLayout layout) {
        List<SubPanel> subPanels = extractSubPanels(layout);
        GeoCoordinate coordinate1 = extractCoordinate(subPanels.get(0));
        GeoCoordinate coordinate2 = extractCoordinate(subPanels.get(1));
        int ref = getOverlayRef(layout);
        if (validateCoordinate(coordinate1) && validateCoordinate(coordinate2)) {
            if (ref == -1) {
                boolean draggable = ! isReadOnly(subPanels.get(0));
                ref = _map.drawRectangle(coordinate1, coordinate2, draggable);
            }
            else {
                _map.updateRectangle(ref, coordinate1, coordinate2);
            }
        }
        _layout2OverlayMap.put(layout, ref);
    }


    // a polygon is presented as three or more subpanels, each with two fields
    private void drawPolygon(DynFormLayout layout) {
        List<GeoCoordinate> coordinateList = new ArrayList<>();
        List<SubPanel> subPanels = extractSubPanels(layout);
        _layout2ChildPanelMap.put(layout, subPanels);
        boolean draggable = true;
        for (SubPanel subPanel : subPanels) {
            GeoCoordinate coordinate = extractCoordinate(subPanel);
            if (! validateCoordinate(coordinate)) {
                continue;
            }
            coordinateList.add(coordinate);
            draggable = ! isReadOnly(subPanel);
        }
        if (coordinateList.isEmpty()) {
            _layout2OverlayMap.put(layout, -1);
            return;
        }
        int ref = getOverlayRef(layout);
        if (ref == -1) {
            ref = _map.drawPolygon(coordinateList, draggable);
        }
        else {
            _map.updatePolygon(ref, coordinateList);
        }
        _layout2OverlayMap.put(layout, ref);
    }


    private List<SubPanel> gatherGeoPanels(List<SubPanel> subPanels) {
        List<SubPanel> geoPanels = new ArrayList<>();
        for (SubPanel subPanel : subPanels) {
            DynFormLayout layout = subPanel.getForm();
            if (layout.isGeoDataType()) {
                if (layout.isGeoDataListType()) {
                    List<SubPanel> childPanels = extractSubPanels(layout);  // a List type
                    for (SubPanel child : childPanels) {
                        _layout2OverlayMap.put(child.getForm(), -1);
                        geoPanels.add(child);
                    }
                }
                else {                                             // a non list type
                    _layout2OverlayMap.put(layout, -1);
                    geoPanels.add(subPanel);
                }
            }
        }
        return geoPanels;
    }


    private DynFormLayout findFirstEmptyLayout() {
        List<DynFormLayout> layouts = getEmptyLayouts();
        for (SubPanel subPanel : _geoPanels) {
             DynFormLayout layout = subPanel.getForm();
             if (layouts.contains(layout)) {
                 return layout;
            }
        }
         return null;
    }

    // there are 2 kinds of empty polygon layout: 1 is an added vertex to an
    // existing polygon, the other is a whole polygon added to a List type
    private DynFormLayout findFirstEmptyPolygonLayout(boolean fullLayout) {
        List<DynFormLayout> layouts = getEmptyLayouts();
        for (SubPanel subPanel : _geoPanels) {
            DynFormLayout layout = subPanel.getForm();
            if (! layout.getDataType().contains("Polygon")) {
                continue;
            }
            if (layouts.contains(layout)) {
                if (fullLayout && ! extractSubPanels(layout).isEmpty()) {
                    return layout;
                }
                else if (!fullLayout && extractSubPanels(layout).isEmpty()) {
                    return layout;
                }
            }
        }
        return null;
    }

    
    private void initOverlays() {
        for (SubPanel subPanel : _geoPanels) {
            drawOverlay(subPanel.getForm());
        }
    }


    private void drawOverlay(DynFormLayout layout) {
        switch (layout.getDataType()) {
            case "YGeoLatLongType":
            case "YGeoLatLongListType" : drawMarker(layout); break;
            case "YGeoCircleType" :
            case "YGeoCircleListType" : drawCircle(layout); break;
            case "YGeoRectType" :
            case "YGeoRectListType" : drawRectangle(layout); break;
            case "YGeoPolygonType" :
            case "YGeoPolygonListType" : drawPolygon(layout); break;
        }
    }

    
    private GeoCoordinate extractCoordinate(SubPanel subPanel) {
        List<TextField> fieldList = extractFields(subPanel.getForm());
        return extractCoordinate(fieldList);
    }

    
    private GeoCoordinate extractCoordinate(List<TextField> fields) {
        double lat = Double.MAX_VALUE;
        double lon = Double.MAX_VALUE;
        for (TextField field : fields) {
            if (field.getLabel().equals("latitude")) {
                lat = StringUtil.strToDouble(field.getValue(), Double.MAX_VALUE);
            }
            if (field.getLabel().equals("longitude")) {
                lon = StringUtil.strToDouble(field.getValue(), Double.MAX_VALUE);
            }
        }
        return new GeoCoordinate(lat, lon);
    }


    private List<TextField> extractFields(DynFormLayout layout) {
        List<TextField> fieldList = new ArrayList<>();
        layout.getChildren().filter(component -> (component instanceof TextField))
               .forEach(p -> fieldList.add((TextField) p));
        return fieldList;
    }

    
    private SubPanel extractSubPanel(DynFormLayout layout) {
        return (SubPanel) layout.getChildren().filter(
                child -> (child instanceof SubPanel)).findAny().orElse(null);
    }


    private List<SubPanel> extractSubPanels(DynFormLayout layout) {
        List<SubPanel> subPanels = new ArrayList<>();
        layout.getChildren().filter(component -> (component instanceof SubPanel))
               .forEach(p -> subPanels.add((SubPanel) p));
        return subPanels;
    }


    private boolean isBlankForm(DynFormLayout layout) {
        for (TextField field : extractFields(layout)) {
            if (! field.isEmpty()) {
                return false;
            }
        }
        return true;
    }



    private GeoCoordinate offsetByMeters(GeoCoordinate origin, double northMeters,
                                         double eastMeters) {
        double R = 6378137.0;  // Earth radius (meters)

        double dLat = northMeters / R;
        double dLon = eastMeters / (R * Math.cos(Math.toRadians(origin.lat())));

        double newLat = origin.lat() + Math.toDegrees(dLat);
        double newLon = origin.lon() + Math.toDegrees(dLon);

        return new GeoCoordinate(newLat, newLon);
    }

    
    private boolean isReadOnly(SubPanel subPanel) {
        List<TextField> fieldList = extractFields(subPanel.getForm());
        return fieldList.get(0).isReadOnly();
    }


    private boolean isVertexPanel(SubPanel subPanel) {
        return "vertex".equals(subPanel.getLabel());
    }


    private void getReasonableWidthForNewOverlay(Consumer<Double> callback) {
        _map.getVisibleMapWidthMeters(width ->
                callback.accept(width / 4)
        );
    }

    
    private List<DynFormLayout> getEmptyLayouts() {
        List<DynFormLayout> layouts = new ArrayList<>();
        for (DynFormLayout layout : _layout2OverlayMap.keySet()) {
            if (_layout2OverlayMap.get(layout) == -1) {
                layouts.add(layout);
            }
        }
        return layouts;
    }


    private boolean validateCoordinate(GeoCoordinate coordinate) {
        if (coordinate == null) {
            return false;
        }
        try {
            coordinate.validate();
        }
        catch (IllegalArgumentException e) {
//            Announcement.show(e.getMessage());
            return false;
        }
        return true;
    }


    private boolean validateChangedValue(String field, String value) {
        if (field == null && value == null) {    // a subpanel has been removed = valid
            return true;
        }
        if (field.equals("radius")) {
            boolean valid = (StringUtil.strToDouble(value, -1) > 0);
            if (! valid) {
                Announcement.warn("Radius must be a positive value");
            }
            return valid;
        }
        GeoCoordinate test = null;
        if (field.equals("latitude")) {
            test = new GeoCoordinate(value, "0");
        }
        else if (field.equals("longitude")) {
            test = new GeoCoordinate("0", value);
        }
        if (test != null) {
            try {
                test.validate();
            }
            catch (IllegalArgumentException e) {
                Announcement.warn(e.getMessage());
                return false;
            }
        }
        return true;
    }
    
}
