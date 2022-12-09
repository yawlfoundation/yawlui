package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.resourcing.resource.nonhuman.NonHumanCategory;
import org.yawlfoundation.yawl.resourcing.resource.nonhuman.NonHumanResource;
import org.yawlfoundation.yawl.resourcing.resource.nonhuman.NonHumanSubCategory;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.NonHumanCategoryDialog;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Michael Adams
 * @date 8/9/2022
 */
public class NonHumanCategorySubView extends AbstractGridView<NonHumanCategory> {

    public NonHumanCategorySubView() {
        super();
        build();
    }


    @Override
    List<NonHumanCategory> getItems() {
        try {
            return getResourceClient().getNonHumanCategories();
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.warn(
                    "Failed to retrieve list of Non-human categories from engine : %s",
                    e.getMessage());
        }
        return Collections.emptyList();
    }


    @Override
    String getTitle() {
        return "";
    }


    @Override
    protected boolean showHeader() {
        return false;
    }


    @Override
    void addColumns(Grid<NonHumanCategory> grid) {
        grid.addColumn(NonHumanCategory::getName).setHeader(UiUtil.bold("Name"));
        grid.addColumn(NonHumanCategory::getDescription)
                .setHeader(UiUtil.bold("Description"));

        grid.addComponentColumn(this::getSubCategoriesComponent)
                .setHeader(UiUtil.bold("Sub-categories"));
        grid.addComponentColumn(this::getMembersComponent)
                .setHeader(UiUtil.bold("Members"));
    }


    @Override
    void configureComponentColumns(Grid<NonHumanCategory> grid) {
        // no component cols
    }

    @Override
    void addItemActions(NonHumanCategory item, ActionRibbon ribbon) {
        ActionIcon editIcon = ribbon.add(VaadinIcon.PENCIL, "Edit", event -> {
            NonHumanCategoryDialog dialog = new NonHumanCategoryDialog(getLoadedItems(),
                    getMembers(item.getID()), item);
            dialog.getOkButton().addClickListener(e -> {
                if (dialog.validate()) {
                    dialog.updateService();
                    dialog.close();
                    refresh();
                }
            });
            dialog.open();
            ribbon.reset();
        });

        // empty space to left to align with footer buttons
        editIcon.insertBlank();

        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Remove", event -> {
            removeCategory(item);
            ribbon.reset();
            refresh();
       });
    }


    @Override
    void addFooterActions(ActionRibbon ribbon) {
        ribbon.add(createAddAction(event -> {
            NonHumanCategoryDialog dialog = new NonHumanCategoryDialog(
                    getLoadedItems(), null, null);
            dialog.getOkButton().addClickListener(e -> {
                if (dialog.validate()) {
                    dialog.updateService();
                    dialog.close();
                    refresh();
                }
            });
            dialog.open();
            ribbon.reset();
        }));

        ribbon.add(createMultiDeleteAction(
                event -> {
                    getGrid().getSelectedItems().forEach(this::removeCategory);
                    ribbon.reset();
                    refresh();
                }));
        
        ribbon.add(createRefreshAction());
    }


    private Component getMembersComponent(NonHumanCategory item) {
        List<NonHumanResource> resList = getMembers(item.getID());
        if (resList.size() > 1) {
            return buildMemberCombo(resList);
        }
        if (resList.size() == 1) {
            return new Label(resList.get(0).getName());
        }
        else {
            return new Label();
        }
    }


    private Component getSubCategoriesComponent(NonHumanCategory item) {
        List<NonHumanSubCategory> subCatList = new ArrayList<>(item.getSubCategories());
        if (subCatList.size() > 1) {
             return buildSubCategoryCombo(subCatList);
         }
         if (subCatList.size() == 1) {
             return new Label(subCatList.get(0).getName());
         }
         else {
             return new Label();
         }
    }


    private ComboBox<NonHumanResource> buildMemberCombo(List<NonHumanResource> resList) {
        resList.sort(Comparator.comparing(NonHumanResource::getName));
        ComboBox<NonHumanResource> comboBox = new ComboBox<>();
        comboBox.setItems(resList);
        comboBox.setItemLabelGenerator(NonHumanResource::getName);
        comboBox.setPlaceholder(resList.size() + " resources");
        comboBox.addValueChangeListener(e -> {
            comboBox.setValue(null);
            comboBox.setPlaceholder(resList.size() + " resources");
        });

        comboBox.getElement().getStyle().set("padding", "0");
        comboBox.getElement().setAttribute("theme", "transparent");
        return comboBox;
    }


    private ComboBox<NonHumanSubCategory> buildSubCategoryCombo(
            List<NonHumanSubCategory> subCatList) {
         subCatList.sort(Comparator.comparing(NonHumanSubCategory::getName));
         ComboBox<NonHumanSubCategory> comboBox = new ComboBox<>();
         comboBox.setItems(subCatList);
         comboBox.setItemLabelGenerator(NonHumanSubCategory::getName);
         comboBox.setPlaceholder(subCatList.size() + " sub-categories");
         comboBox.addValueChangeListener(e -> {
             comboBox.setValue(null);
             comboBox.setPlaceholder(subCatList.size() + " sub-categories");
         });

         comboBox.getElement().getStyle().set("padding", "0");
         comboBox.getElement().setAttribute("theme", "transparent");
         return comboBox;
     }


    private List<NonHumanResource> getMembers(String categoryID) {
        try {
            return getResourceClient().getNonHumanCategoryMembers(
                    categoryID, null);
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error("Failed to get members for category: %s",
                    e.getMessage());
            return Collections.emptyList();
        }
    }


    private void removeCategory(NonHumanCategory category) {
        try {
            if (getResourceClient().removeNonHumanCategory(category)) {
                Announcement.success("Removed category: " + category.getName());
            }
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error("Failed to remove category '%s': %s",
                    category.getName(), e.getMessage());
        }
    }

}
