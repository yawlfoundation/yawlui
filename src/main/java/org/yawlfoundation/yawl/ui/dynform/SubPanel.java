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

import java.util.List;
import java.util.Optional;

/**
 * a panel layout with a few extra members
 *
 * @author Michael Adams
 * Date: 26/02/2008
 */

public class SubPanel extends VerticalLayout implements Cloneable {

    private final DynFormLayout _form;
    private String name ;

    private SubPanelController controller;
    private final DynFormFactory _factory;

    // 'occurs' buttons
    private ActionIcon btnPlus;
    private ActionIcon btnMinus;


    // Constructor //
    public SubPanel(DynFormFactory factory) {
        super();
        _form = new DynFormLayout();
        _factory = factory;
        getStyle().set("margin-top", "10px");
    }


    public void addHeader(H5 header) { add(header); }

    public String getName() { return name; }

    public void setName(String name) {this.name = name;}

    public ActionIcon getBtnPlus() { return btnPlus; }

    public void setBtnPlus(ActionIcon btnPlus) { this.btnPlus = btnPlus; }

    public ActionIcon getBtnMinus() { return btnMinus; }

    public void setBtnMinus(ActionIcon btnMinus) { this.btnMinus = btnMinus; }

    public SubPanelController getController() { return controller; }

    public void setController(SubPanelController controller) {
        this.controller = controller;
    }

    public boolean isChoicePanel() {
        return getName().equals("choicePanel");
    }

    public boolean isEmpty() {
        return ! getChildren().findAny().isPresent();
    }

    public void addContent(List<Component> components) {
        _form.add(components);
        add(_form);
    }

    public DynFormLayout getForm() { return _form; }


    public double calculateHeight() {
        return DynFormLayout.HEADING_HEIGHT + getForm().calculateHeight();
    }

    /***************************************************************************/

     public void assignStyle() {

        // set user-defined background colour (if given)
        String backColour = controller.getUserDefinedBackgroundColour();
        if (backColour != null) {
            getStyle().set("background-color", backColour);
        }
        
        setClassName(controller.getSubPanelStyleClass());
    }


    public void addOccursButtons() {
        addComponentAsFirst(createOccursButtons());
    }


    public SubPanel clone() throws CloneNotSupportedException {
        super.clone();
        SubPanel cloned = new SubPanel(_factory);
        cloned.setName(name);
        cloned.setController(controller);
        this.getStyle().getNames().forEach(name ->
                cloned.getStyle().set(name, this.getStyle().get(name)));
        cloned.setClassName(this.getClassName());
        return cloned;
    }


    public void enableOccursButtons(boolean enable) {
        if (btnPlus != null) btnPlus.setEnabled(enable);
        if (btnMinus != null) btnMinus.setEnabled(enable);
    }


    private JustifiedButtonLayout createOccursButtons() {
        btnPlus = createAddOccursButton();
        btnMinus = createRemoveOccursButton();
        return new JustifiedButtonLayout(btnPlus, btnMinus);
    }


    private ActionIcon createAddOccursButton() {
        ActionIcon icon = new ActionIcon(VaadinIcon.PLUS, ActionIcon.GREEN, "Add", e -> {
            SubPanel newPanel = new SubPanelCloner().clone(this, _factory);
            DynFormLayout parent = getContainer();
            if (parent != null) {
                parent.addComponentAtIndex(controller.getPanelIndex(this), newPanel);
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
