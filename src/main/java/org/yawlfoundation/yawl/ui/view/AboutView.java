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
import org.yawlfoundation.yawl.ui.util.BuildInformation;
import org.yawlfoundation.yawl.ui.util.UiUtil;
import org.yawlfoundation.yawl.util.StringUtil;

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

    private static final String DEFAULT_VERSION = "5.0";

    public AboutView() {
        super();

        add(createHeader("About This Version"));

        add(createLayout());
        setSizeFull();
    }

    
    @Override
    Component createLayout() {
        BuildInformation buildInformation = new BuildInformation();
        return new VerticalLayout(getCopyright(buildInformation),
                new H5("Major Components"),
                buildGrid(buildInformation),
                getVaadinCredit(buildInformation)
        );
    }


    private Grid<BuildDetails> buildGrid(BuildInformation buildInformation) {
        Grid<BuildDetails> grid = new Grid<>();
        grid.setItems(getItems(buildInformation));
        grid.addColumn(BuildDetails::getService).setHeader(UiUtil.bold("Component"));
        grid.addColumn(BuildDetails::getVersion).setHeader(UiUtil.bold("Version"));
        grid.addColumn(BuildDetails::getNumber).setHeader(UiUtil.bold("Build Number"));
        grid.addColumn(BuildDetails::getDate).setHeader(UiUtil.bold("Build Date"));
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setAllRowsVisible(true);    // sets height to number of rows
        return grid;
    }


    private List<BuildDetails> getItems(BuildInformation buildInformation) {
        List<BuildDetails> items = new ArrayList<>();
        items.add(getBuildDetails(getEngineClient(), "YAWL Engine"));
        items.add(getBuildDetails(getResourceClient(), "Resource Service"));

        BuildDetails workletDetails = getBuildDetails(getWorkletClient(), "Worklet Service");
        if (workletDetails != null) {
            items.add(getBuildDetails(getWorkletClient(), "Worklet Service"));
        }
        addStaticItems(items, buildInformation);
        return items;
    }


    private void addStaticItems(List<BuildDetails> items, BuildInformation buildInformation) {
        items.add(new BuildDetails("YAWL UI (this component)",
                buildInformation.getUIProperties().asMap()));
        items.add(new BuildDetails("Mail Service",
                buildInformation.getMailServiceProperties().asMap()));
        items.add(new BuildDetails("Web Service Invoker",
                buildInformation.getInvokerServiceProperties().asMap()));
        items.add(new BuildDetails("Document Store",
                buildInformation.getDocStoreProperties().asMap()));
    }


    private VerticalLayout getCopyright(BuildInformation buildInformation) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        
        Span span = new Span(String.format("YAWL Version %s. Copyright (c) 2004-%d ",
                buildInformation.getUIProperties().version, LocalDate.now().getYear()));
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


    private Span getVaadinCredit(BuildInformation buildInformation) {
        Span span = new Span("The YAWL UI is built on the ");
        span.add(link("https://vaadin.com/", "Vaadin Framework"));
        span.add(String.format(" (version %s).", buildInformation.get("vaadin.version")));
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

        String getDate() {
            String date = get("BuildDate");
            return StringUtil.isNullOrEmpty(date) ? "N/A" : date;
        }

        String getNumber() {
            String number = get("BuildNumber");
            return StringUtil.isNullOrEmpty(number) ? "N/A" : number;
        }

        String getVersion() {
            String version = get("Version");
            return StringUtil.isNullOrEmpty(version) ? DEFAULT_VERSION : version;
        }

        String get(String key) {
            String value = detailsMap.get(key);
            return value != null ? value : "";
        }
    }
}
