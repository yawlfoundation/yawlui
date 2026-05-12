package org.yawlfoundation.yawl.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;

@Theme("common-theme")
@StyleSheet("styles.css")
public class AppShell implements AppShellConfigurator {

    // called on startup to set the app's favicon and stylesheet
    @Override
    public void configurePage(AppShellSettings settings) {
  //      settings.addLink("stylesheet", "./styles/shared-styles.css");
  //      settings.addLink("stylesheet", "frontend://styles/shared-styles.css");
        settings.addFavIcon(Inline.Position.PREPEND,
                "icon", "icons/favicon.png", "32x32");
    }

}

