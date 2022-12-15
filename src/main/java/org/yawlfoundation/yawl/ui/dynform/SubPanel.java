/*
 * Copyright (c) 2004-2020 The YAWL Foundation. All rights reserved.
 * The YAWL Foundation is a collaboration of individuals and
 * organisations who are committed to improving workflow technology.
 *
 * This file is part of YAWL. YAWL is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation.
 *
 * YAWL is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with YAWL. If not, see <http://www.gnu.org/licenses/>.
 */

package org.yawlfoundation.yawl.ui.dynform;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.yawlfoundation.yawl.ui.layout.JustifiedButtonLayout;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.util.List;
import java.util.Optional;

/**
 * A vertical layout that consists of an optional button bar (plus and minus),
 * an optional header (H5) and a form (DynFormLayout).
 *
 * @author Michael Adams
 * Date: 26/02/2008  (extended for v5 01/10/2022)
 */

public class SubPanel extends VerticalLayout implements Cloneable {

    public static final double HEADING_HEIGHT = 46.39;               // includes spacing
    public static final double OCCURS_BAR_HEIGHT = 22.0;
    
    private final DynFormLayout _form;
    private final String _name;
    private String label;

    private SubPanelController controller;
    private final DynFormFactory _factory;

    // 'occurs' buttons
    private ActionIcon btnPlus;
    private ActionIcon btnMinus;


    // Constructor //
    public SubPanel(DynFormFactory factory, String name) {
        super();
        _form = new DynFormLayout(name);
        _factory = factory;
        _name = name;
        getStyle().set("margin-top", "10px");
        getStyle().set("border", "1px solid lightgray");
    }


    public void addHeader(H5 header) { if (header != null) add(header); }


    public H5 getHeader() {
        return (H5) getChildren().filter(child ->
                child instanceof H5).findFirst().orElse(null);
    }

    public boolean hasHeader() { return getHeader() != null; }


    public String getName() { return _name; }

    public void setLabel(String label) { this.label = label; }

    public String getLabel() { return label != null ? label : getName(); }

    public ActionIcon getBtnPlus() { return btnPlus; }

    public ActionIcon getBtnMinus() { return btnMinus; }

    public SubPanelController getController() { return controller; }

    public void setController(SubPanelController controller) {
        this.controller = controller;
    }
    

    public boolean isEmpty() {
        return ! _form.getChildren().findAny().isPresent();
    }

    public void addContent(List<Component> components) {
        _form.add(components);
        add(_form);
    }

    public DynFormLayout getForm() { return _form; }


    public double calculateHeight() {
        long headers = getChildren().filter(c -> c instanceof H5).count();    // 0 or 1
        double occurs = controller.canVaryOccurs() ? OCCURS_BAR_HEIGHT : 0;
        return headers * HEADING_HEIGHT + occurs + getForm().calculateHeight();
    }


    public void addOccursButtons() {
        addComponentAsFirst(createOccursButtons());
    }


    public SubPanel clone() throws CloneNotSupportedException {
        super.clone();
        SubPanel cloned = new SubPanel(_factory, _name);
        this.getStyle().getNames().forEach(name ->
                cloned.getStyle().set(name, this.getStyle().get(name)));
        controller.addSubPanel(cloned);
        return cloned;
    }


    // initial enablement from panel creation
    public void enableOccursButtons(boolean enable) {
        if (btnPlus != null) btnPlus.setEnabled(enable);
        if (btnMinus != null) btnMinus.setEnabled(enable);
    }


    private JustifiedButtonLayout createOccursButtons() {
        btnPlus = createAddOccursButton();
        btnMinus = createRemoveOccursButton();
        JustifiedButtonLayout buttonLayout = new JustifiedButtonLayout(btnPlus, btnMinus);
        buttonLayout.setWidthFull();
        return buttonLayout;
    }


    private ActionIcon createAddOccursButton() {
        ActionIcon icon = new ActionIcon(VaadinIcon.PLUS, ActionIcon.GREEN, "Add", e -> {
            SubPanel newPanel = new SubPanelCloner().clone(this, _factory);
            DynFormLayout parent = getContainer();
            if (parent != null) {
                int indexAfterThis = controller.getPanelIndex(this) + 1;
                if (indexAfterThis == controller.getSubPanels().size()) {
                    parent.add(newPanel);       // add as last
                }
                else {
                    parent.addComponentAtIndex(indexAfterThis, newPanel);  // add under this
                }
                UiUtil.setFocus(newPanel);
            }
        });
        icon.setSize("12px");
        return icon;
    }


    private ActionIcon createRemoveOccursButton() {
        ActionIcon icon = new ActionIcon(VaadinIcon.CLOSE_SMALL, ActionIcon.RED,
                        "Remove", e -> {
            DynFormLayout parent = getContainer();
            if (parent != null) {
                parent.remove(this);
                controller.removeSubPanel(this);
                _factory.removeOrphanedControllers(this);
            }
        });
        icon.setSize("12px");
        return icon;
    }


    private DynFormLayout getContainer() {
        Optional<Component> parent = getParent();
        if (parent.isPresent()) {
            Component container = parent.get();
            if (container instanceof DynFormLayout) {
                return (DynFormLayout) container;
            }
        }
        return null;
    }

}
