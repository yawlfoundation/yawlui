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
    private long _currOccurs ;            // current display count (min <= curr <= max)
    private int _depthlevel ;             // the nested level of this panel set
    private String _name;
    private String _bgColour;             // user-defined colours via extended attributes
    private String _altBgColour;

    // the list of subpanel instances (i.e. all instances of the same subpanel)
    private List<SubPanel> _panelList = new ArrayList<>();

    public SubPanelController() {}

    public SubPanelController(SubPanel panel, long minOccurs, long maxOccurs, int level,
                              String udBgColour, String udAltBgColour) {
        _panelList.add(panel);
        _minOccurs = minOccurs;
        _maxOccurs = maxOccurs;
        _depthlevel = level;
        _currOccurs = 1;
        _name = panel.getName();
        _bgColour = udBgColour;
        _altBgColour = udAltBgColour;
    }


    /********************************************************************************/

    // Getters & Setters //

    public long getMinOccurs() { return _minOccurs; }

    public void setMinOccurs(long minOccurs) { _minOccurs = minOccurs; }

    public void setMinOccurs(String minOccurs) { _minOccurs = convertOccurs(minOccurs); }


    public long getMaxOccurs() { return _maxOccurs; }

    public void setMaxOccurs(long maxOccurs) { _maxOccurs = maxOccurs; }

    public void setMaxOccurs(String maxOccurs) { _maxOccurs = convertOccurs(maxOccurs); }    


    public long getCurrOccurs() { return _currOccurs; }

    public void setCurrOccurs(long currOccurs) { _currOccurs = currOccurs; }


    public int getDepthlevel() { return _depthlevel; }

    public void setDepthlevel(int depthlevel) { _depthlevel = depthlevel; }


    public String getName() { return _name; }

    public void setName(String name) { _name = name; }


    public String getAltBgColour() { return _altBgColour; }

    public void setAltBgColour(String colour) { _altBgColour = colour; }


    public String getBgColour() { return _bgColour; }

    public void setBgColour(String colour) { _bgColour = colour; }

    

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


    /** @return the appropriate style class for this depthlevel */
    public String getSubPanelStyleClass() {
        return (_depthlevel % 2 == 0) ? "dynformSubPanelAlt" : "dynformSubPanel";
    }


    /** @return the user-defined bg colour (if any) for this depthlevel */
    public String getUserDefinedBackgroundColour() {
        return (_depthlevel % 2 == 0) ? _altBgColour : _bgColour;        
    }


    /** @return true if this subpanel can appear more times that it currently is */
    public boolean canVaryOccurs() {
        return ((_maxOccurs > 1) && (_minOccurs < _maxOccurs));
    }


    
    public void assignStyleToSubPanels(int maxLevel) {
        _panelList.forEach(SubPanel::assignStyle);
    }


    /**
     * Adds a new, cloned subpanel to this controller
     * 
     * @param newPanel the subpanel to add
     * @return the outermost containing subpanel of the one added 
     */
    public void addSubPanel(SubPanel newPanel) {
        _currOccurs++;
        _panelList.add(newPanel);

        // enable/disable buttons as required
        setOccursButtonsEnablement();
    }

    /**
     * Remove a subpanel from the set
     * @param oldPanel the panel to remove
     * @return the outermost containing subpanel of the one removed
     */
    public void removeSubPanel(SubPanel oldPanel) {
        _currOccurs--;
        _panelList.remove(oldPanel);
        setOccursButtonsEnablement();
    }

    
    /** enable/disable the occurs buttons as required */
    public void setOccursButtonsEnablement() {
        _currOccurs = _panelList.size();
        _panelList.forEach(panel -> {
            panel.getBtnMinus().setEnabled(_currOccurs > 1 && _currOccurs > _minOccurs);
            panel.getBtnPlus().setEnabled(_currOccurs < _maxOccurs);
        });
    }


    /** clone this controller */
    public SubPanelController clone() throws CloneNotSupportedException {
        SubPanelController clone = (SubPanelController) super.clone();
        clone.setCurrOccurs(_currOccurs);
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
    }


}
