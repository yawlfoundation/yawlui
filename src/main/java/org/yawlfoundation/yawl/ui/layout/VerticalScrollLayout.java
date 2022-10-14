/*
 * Copyright (c) 2022 Queensland University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.yawlfoundation.yawl.ui.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * @author Michael Adams
 * @date 26/5/21
 */
public class VerticalScrollLayout extends VerticalLayout {
    private VerticalLayout _content;

    public VerticalScrollLayout() {
        preparePanel();
        prepareContent(null);
    }

    public VerticalScrollLayout(VerticalLayout content) {
        preparePanel();
        prepareContent(null);
    }

    public VerticalScrollLayout(Component... children){
        this();
        this.add(children);
    }

    private void preparePanel() {
        setWidth("100%");
//        setHeight("100%");
        getStyle().set("overflow", "auto");
    }

    private void prepareContent(VerticalLayout content) {
        _content = content != null ? content : new VerticalLayout();
        _content.getStyle().set("display", "block");
        _content.setWidth("100%");
        _content.setPadding(false);
        super.add(_content);
    }

    public VerticalLayout getContent(){
        return _content;
    }

    @Override
    public void add(Component... components){
        _content.add(components);
    }

    @Override
    public void remove(Component... components){
        _content.remove(components);
    }

    @Override
    public void removeAll(){
        _content.removeAll();
    }

    @Override
    public void addComponentAsFirst(Component component) {
        _content.addComponentAtIndex(0, component);
    }
}
