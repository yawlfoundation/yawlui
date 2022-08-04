package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.yawlfoundation.yawl.engine.interfce.SpecificationData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Adams
 * @date 29/7/2022
 */
public class SpecInfoDialog extends AbstractDialog {

    public SpecInfoDialog(SpecificationData specData) {
        super("Specification Info");
        addGrid(specData);
        addCloseButton();
        setWidth("580px");
    }


    private void addGrid(SpecificationData specData) {
        List<Pair<String, String>> infoList = new ArrayList<>();
        infoList.add(new ImmutablePair<>("Name", specData.getSpecURI()));
        infoList.add(new ImmutablePair<>("Version", specData.getSpecVersion()));
        infoList.add(new ImmutablePair<>("Description", specData.getDocumentation()));
        infoList.add(new ImmutablePair<>("Identifier", specData.getSpecIdentifier()));
        infoList.add(new ImmutablePair<>("Meta-title", specData.getMetaTitle()));
        infoList.add(new ImmutablePair<>("Schema Version", specData.getSchemaVersion().toString()));
        infoList.add(new ImmutablePair<>("Author", specData.getAuthors()));
        infoList.add(new ImmutablePair<>("Root Net", specData.getRootNetID()));
        infoList.add(new ImmutablePair<>("Status", specData.getStatus()));

        Grid<Pair<String, String>> grid = new Grid<>();
        grid.addColumn(Pair::getLeft).setAutoWidth(true);
        grid.addColumn(Pair::getRight).setAutoWidth(true);
        grid.setItems(infoList);
        grid.setAllRowsVisible(true);
        addComponent(grid);
    }


    private void addCloseButton() {
        Button closeBtn = new Button("Close", e -> close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getButtonBar().add(closeBtn);
    }

}
