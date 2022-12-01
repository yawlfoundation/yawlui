package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.yawlfoundation.yawl.ui.service.AbstractClient;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.service.WorkletClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Adams
 * @date 29/11/2022
 */
public class AboutView extends AbstractView {

    private static final String VERSION = "5.0";
    private static final String DEFAULT_BUILD_DATE = "2022/11/29 08.09";

    public AboutView(ResourceClient resClient, EngineClient engClient,
                     WorkletClient wsClient) {
        super(resClient, engClient, wsClient);

        add(createHeader("About This Version"));
        add(createLayout());
        setSizeFull();
    }

    
    @Override
    Component createLayout() {
        return new VerticalLayout(getCopyright(), new H5("Major Components"),
                buildGrid(), getVaadinCredit());
    }


    private Grid<BuildDetails> buildGrid() {
        Grid<BuildDetails> grid = new Grid<>();
        grid.setItems(getItems());
        grid.addColumn(BuildDetails::getService).setHeader(UiUtil.bold("Component"));
        grid.addColumn(BuildDetails::getVersion).setHeader(UiUtil.bold("Version"));
        grid.addColumn(BuildDetails::getNumber).setHeader(UiUtil.bold("Build Number"));
        grid.addColumn(BuildDetails::getDate).setHeader(UiUtil.bold("Build Date"));
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setAllRowsVisible(true);    // sets height to number of rows
        return grid;
    }


    private List<BuildDetails> getItems() {
        List<BuildDetails> items = new ArrayList<>();
        items.add(getBuildDetails(getEngineClient(), "YAWL Engine"));
        items.add(getBuildDetails(getResourceClient(), "Resource Service"));

        BuildDetails workletDetails = getBuildDetails(getWorkletClient(), "Worklet Service");
        if (workletDetails != null) {
            items.add(getBuildDetails(getWorkletClient(), "Worklet Service"));
        }
        addStaticItems(items);
        return items;
    }


    private void addStaticItems(List<BuildDetails> items) {
        items.add(new BuildDetails("Mail Service", DEFAULT_BUILD_DATE,
                VERSION, "181"));
        items.add(new BuildDetails("Web Service Invoker", DEFAULT_BUILD_DATE,
                VERSION, "301"));
        items.add(new BuildDetails("Document Store", DEFAULT_BUILD_DATE,
                VERSION, "124"));
    }


    private VerticalLayout getCopyright() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        
        Span span = new Span(String.format("YAWL Version %s. Copyright (c) 2004-%d ",
                VERSION, LocalDate.now().getYear()));
        span.add(link("https://yawlfoundation.github.io/", "The YAWL Foundation"));
        span.add(". All rights reserved.");
        layout.add(span);

        span = new Span("YAWL is free software: you can" +
                        " redistribute it and/or modify it under the terms of the ");
        span.add(link("https://www.gnu.org/licenses/lgpl-3.0.html",
                "GNU Lesser General Public License"));
        span.add(" as published by the Free Software Foundation.");
        layout.add(span);

        return layout;
    }


    private Span getVaadinCredit() {
        Span span = new Span("The YAWL UI is built on the ");
        span.add(link("https://vaadin.com/", "Vaadin Framework"));
        span.add(" (version 23.2.9).");
        return span;
    }


    private Anchor link(String href, String text) {
        return new Anchor(href, text, AnchorTarget.BLANK);
    }


    private BuildDetails getBuildDetails(AbstractClient client, String name) {
        try {
            return new BuildDetails(name, client.getBuildProperties());
        }
        catch (IOException e) {
            return null;
        }
    }


    class BuildDetails {

        final String service;
        final Map<String, String> detailsMap;

        BuildDetails(String name, Map<String, String> map) {
            service = name;
            detailsMap = map;
        }

        BuildDetails(String name, String date, String version, String number) {
            this(name, Map.of("BuildDate", date, "Version", version,
                    "BuildNumber", number));
        }

        String getService() { return service; }

        String getDate() { return get("BuildDate"); }

        String getNumber() { return get("BuildNumber"); }

        String getVersion() { return get("Version"); }

        String get(String key) {
            String value = detailsMap.get(key);
            return value != null ? value : "";
        }
    }
}
