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

import org.yawlfoundation.yawl.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a set of 'cloned' panels on a dynamic form
 *
 * Author: Michael Adams
 * Date: 21/02/2008
 */

public class SubPanelController {

    private long _minOccurs ;
    private long _maxOccurs ;
    private int _depthlevel ;             // the nested level of this panel set
    private String _name;
    private final String _bgColour;             // user-defined colours via extended attributes
    private final String _altBgColour;

    // a list of instances of the same sub-panel
    private final List<SubPanel> _panelList = new ArrayList<>();

    
    public SubPanelController(SubPanel panel, long minOccurs, long maxOccurs, int level,
                              String udBgColour, String udAltBgColour) {
        _panelList.add(panel);
        _minOccurs = minOccurs;
        _maxOccurs = maxOccurs;
        _depthlevel = level;
        _name = panel.getName();
        _bgColour = udBgColour;
        _altBgColour = udAltBgColour;
        setBackgroundColour(panel);
    }

    
    private void setMinOccurs(long minOccurs) { _minOccurs = minOccurs; }

    private void setMaxOccurs(long maxOccurs) { _maxOccurs = maxOccurs; }

    private void setDepthlevel(int depthlevel) { _depthlevel = depthlevel; }

    public int getDepthlevel() { return _depthlevel; }


    public String getName() { return _name; }

    public void setName(String name) { _name = name; }


    public List<SubPanel> getSubPanels() { return _panelList; }

    public boolean hasPanel(SubPanel panel) {
        return _panelList.contains(panel);
    }

    public int getPanelIndex(SubPanel panel) { return _panelList.indexOf(panel); }


    /** @return the int value of the min or max Occurs string for this panel set */
    public static int convertOccurs(String occurs) {
        if (occurs == null) {
            return 1;
        }
        if (occurs.equals("unbounded")) {
            return Integer.MAX_VALUE;
        }
        return StringUtil.strToInt(occurs, 1);
    }


    /** @return the appropriate colour for this depth level */
    public String getBackgroundColour() {
        if (_depthlevel % 2 == 0) {
            return _altBgColour != null ? _altBgColour : "var(--lumo-shade-5pct)";
        }
        return _bgColour != null ? _bgColour : "white";
    }


    /** @return true if this subpanel can appear more times that it currently is */
    public boolean canVaryOccurs() {
        return ((_maxOccurs > 1) && (_minOccurs < _maxOccurs));
    }


    /**
     * Adds a new, cloned subpanel to this controller
     * 
     * @param newPanel the subpanel to add
     * @return the outermost containing subpanel of the one added 
     */
    public void addSubPanel(SubPanel newPanel) {
        _panelList.add(newPanel);
        newPanel.setController(this);
        setBackgroundColour(newPanel);
    }

    /**
     * Remove a subpanel from the set
     * @param oldPanel the panel to remove
     * @return the outermost containing subpanel of the one removed
     */
    public void removeSubPanel(SubPanel oldPanel) {
        _panelList.remove(oldPanel);
        setOccursButtonsEnablement();
    }

    
    /** enable/disable the occurs buttons as required */
    public void setOccursButtonsEnablement() {
        int currOccurs = _panelList.size();
        _panelList.forEach(panel -> {
            panel.getBtnMinus().setEnabled(currOccurs > 1 && currOccurs > _minOccurs);
            panel.getBtnPlus().setEnabled(currOccurs < _maxOccurs);
        });
    }


    /** clone this controller */
    public SubPanelController clone() throws CloneNotSupportedException {
        SubPanelController clone = (SubPanelController) super.clone();
        clone.setMaxOccurs(_maxOccurs);
        clone.setMinOccurs(_minOccurs);
        clone.setDepthlevel(_depthlevel);
        return clone;
    }


    /**
     * Adds a subpanel to the set without making any surrounding adjustments to
     * screen coordinates
     *
     * @param panel the panel to add
     */
    public void storeSubPanel(SubPanel panel) {
        _panelList.add(panel) ;
        panel.setController(this);
        setBackgroundColour(panel);
    }


    // set bg colour if required
    private void setBackgroundColour(SubPanel panel) {
        String backColour = getBackgroundColour();
        if (backColour != null) {
            panel.getStyle().set("background-color", backColour);
        }

    }

}
