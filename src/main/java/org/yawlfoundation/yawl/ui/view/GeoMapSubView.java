package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.geomap.GeoCoordinate;
import org.yawlfoundation.yawl.ui.component.geomap.VcfLeafletGeoMap;
import org.yawlfoundation.yawl.ui.dynform.DynForm;
import org.yawlfoundation.yawl.ui.dynform.DynFormLayout;
import org.yawlfoundation.yawl.ui.dynform.SubPanel;
import org.yawlfoundation.yawl.ui.listener.DynFormContentChangeListener;
import org.yawlfoundation.yawl.ui.listener.DynFormContentChangedEvent;
import org.yawlfoundation.yawl.ui.listener.GeoMapOverlayMoveListener;
import org.yawlfoundation.yawl.ui.listener.GeoMapOverlayMovedEvent;
import org.yawlfoundation.yawl.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Adams
 * @date 12/12/25
 */
public class GeoMapSubView extends AbstractView
        implements DynFormContentChangeListener, GeoMapOverlayMoveListener {

    private final VcfLeafletGeoMap _map = new VcfLeafletGeoMap();
    private final DynForm _form;
    private final List<SubPanel> _geoPanels;
    private final Map<DynFormLayout, Integer> _layout2OverlayMap = new HashMap<>();

    private boolean _updatingOverlays = false;

    public GeoMapSubView(DynForm form) {
        super();
        _form = form;
        _form.addGeoTypeChangeListener(this);
        _map.addMoveListener(this);
        _geoPanels = gatherGeoPanels();
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
        if (_updatingOverlays) {
            return;
        }
        if (! validateChangedValue(event.getFieldName(), event.getNewValue())) {
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


    @Override
    public void overlayMoved(GeoMapOverlayMovedEvent event) {
        _updatingOverlays = true;
        for (DynFormLayout layout : _layout2OverlayMap.keySet()) {
            int ref = _layout2OverlayMap.get(layout);
            if (ref == event.getRef()) {
                switch (event.getOverlayType()) {
                    case Marker: updateMarkerValues(layout, event.getPoint()); break;
                    case Circle: updateCircleValues(layout, event.getPoint(), event.getRadius()); break;
                    case Polygon: updatePolygonValues(layout, event.getPoints());
                }
            }
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


    private void updatePolygonValues(DynFormLayout layout, List<GeoCoordinate> vertices) {
        List<SubPanel> subPanels = extractSubPanels(layout);
        if (subPanels.size() == 2) {     // rectangle
            updateForm(subPanels.get(0).getForm(), vertices.get(0));      // top left
            updateForm(subPanels.get(1).getForm(), vertices.get(2));      // bot right
        }
        else {
            int i = 0;
            for  (SubPanel subPanel : subPanels) {
                updateForm(subPanel.getForm(), vertices.get(i++));
            }
        }
    }

    
    private void updateForm(DynFormLayout layout, GeoCoordinate location) {
        for (TextField field : extractFields(layout)) {
            if (field.getLabel().equals("latitude")) {
                field.setValue(String.valueOf(location.lat()));
            }
            if (field.getLabel().equals("longitude")) {
                field.setValue(String.valueOf(location.lon()));
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
        int ref = getOverlayRef(layout);;
        if (validateCoordinate(coordinate)) {
            if (ref == -1) {
                ref = _map.drawMarker(coordinate);
                _layout2OverlayMap.put(layout, ref);
            }
            else {
                _map.updateMarker(ref, coordinate);
            }
        }
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
                    ref = _map.drawCircle(coordinate, radius);
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
                ref = _map.drawRectangle(coordinate1, coordinate2);
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
        for (SubPanel subPanel : extractSubPanels(layout)) {
            GeoCoordinate coordinate = extractCoordinate(subPanel);
            if (! validateCoordinate(coordinate)) {
                continue;
            }
            coordinateList.add(coordinate);
        }
        if (coordinateList.isEmpty()) {
            _layout2OverlayMap.put(layout, -1);
            return;
        }
        int ref = getOverlayRef(layout);
        if (ref == -1) {
            ref = _map.drawPolygon(coordinateList);
        }
        else {
            _map.updatePolygon(ref, coordinateList);
        }
        _layout2OverlayMap.put(layout, ref);
    }


    private List<SubPanel> gatherGeoPanels() {
        List<SubPanel> geoPanels = new ArrayList<>();
        for (SubPanel subPanel : _form.getSubPanels()) {
            DynFormLayout layout = subPanel.getForm();
            if (layout.isGeoDataType()) {
                geoPanels.add(subPanel);
            }
        }
        return geoPanels;
    }


    private void initOverlays() {
        for (SubPanel subPanel : _geoPanels) {
            drawOverlay(subPanel.getForm());
        }
    }
    

    private void drawOverlay(DynFormLayout layout) {
        switch (layout.getDataType()) {
           case "YGeoLatLongType": drawMarker(layout); break;
           case "YGeoCircleType" : drawCircle(layout); break;
           case "YGeoRectType" : drawRectangle(layout); break;
           case "YGeoPolygonType" : drawPolygon(layout); break;
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


    private boolean validateCoordinate(GeoCoordinate coordinate) {
        if (coordinate == null) {
            return false;
        }
        try {
            coordinate.validate();
        }
        catch (IllegalArgumentException e) {
            Announcement.show(e.getMessage());
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
