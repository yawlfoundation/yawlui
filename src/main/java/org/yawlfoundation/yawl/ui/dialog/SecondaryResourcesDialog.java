package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.yawlfoundation.yawl.resourcing.resource.AbstractResourceAttribute;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.nonhuman.NonHumanCategory;
import org.yawlfoundation.yawl.resourcing.resource.nonhuman.NonHumanResource;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.Prompt;
import org.yawlfoundation.yawl.ui.component.ResourceList;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.util.XNode;

import java.io.IOException;
import java.util.*;

/**
 * @author Michael Adams
 * @date 10/11/2022
 */
public class SecondaryResourcesDialog extends AbstractDialog {

    private final List<Participant> _participants;
    private final List<AbstractResourceAttribute> _roles;
    private final List<NonHumanResource> _resources;
    private final List<NonHumanCategory> _categories;
    private final List<String> _categoryItems;
    private final MultiSelectListBox<Participant> _participantList;
    private final MultiSelectListBox<AbstractResourceAttribute> _roleList;
    private final MultiSelectListBox<NonHumanResource> _resourceList;
    private final MultiSelectListBox<String> _categoryList;
    private final ResourceList<String> _selectedList;
    private final SelectionSet _selectionSet = new SelectionSet();
    private final Button _okButton = new Button("OK");

    public SecondaryResourcesDialog(ResourceClient resClient, String wirID) {
        super();

        _participants = loadParticipants(resClient);
        _roles = loadRoles(resClient);
        _resources = loadResources(resClient);
        _categories = loadCategories(resClient);
        _categoryItems = generateCategoryItems(_categories);

        _selectedList = createSelectedList();
        _participantList = createParticipantList(_participants);
        _roleList = createRoleList(_roles);
        _resourceList = createResourceList(_resources);
        _categoryList = createCategoryList(_categoryItems);

        loadCurrentResources(resClient, wirID);
        addSelectionListeners();

        setHeader("Select Secondary Resources for Item " + wirID, false);
        addComponent(createContent());
        createButtons();
        setWidth("800px");
    }


    public Button getOkButton() { return _okButton; }

    public XNode getSelections() { return _selectionSet.toXML(); }

    
    private MultiSelectListBox<Participant> createParticipantList(List<Participant> items) {
        MultiSelectListBox<Participant> listbox = new MultiSelectListBox<>();
        listbox.setItems(items);
        listbox.setItemLabelGenerator(Participant::getFullName);
        configure(listbox);
        return listbox;
    }


    private MultiSelectListBox<AbstractResourceAttribute> createRoleList(
            List<AbstractResourceAttribute> items) {
        MultiSelectListBox<AbstractResourceAttribute> listbox = new MultiSelectListBox<>();
        listbox.setItems(items);
        listbox.setItemLabelGenerator(AbstractResourceAttribute::getName);
        configure(listbox);
        return listbox;
    }


    private MultiSelectListBox<NonHumanResource> createResourceList(
            List<NonHumanResource> items) {
        MultiSelectListBox<NonHumanResource> listbox = new MultiSelectListBox<>();
        listbox.setItems(items);
        listbox.setItemLabelGenerator(NonHumanResource::getName);
        configure(listbox);
        return listbox;
    }

    
    private MultiSelectListBox<String> createCategoryList(List<String> items) {
        MultiSelectListBox<String> listbox = new MultiSelectListBox<>();
        listbox.setItems(items);
        configure(listbox);
        return listbox;
    }


    private List<String> generateCategoryItems(List<NonHumanCategory> catList) {
        List<String> items = new ArrayList<>();
        catList.forEach(cat -> {
            items.add(cat.getName());
            cat.getSubCategoryNames().forEach(subcat -> {
                if (!subcat.equals("None")) {
                    items.add(cat.getName() + " -> " + subcat);
                }
            });
        });
        Collections.sort(items);
        return items;
    }


    private void addSelectionListeners() {
        _participantList.addSelectionListener(e ->
                _selectionSet.setParticipants(e.getAllSelectedItems()));
        _roleList.addSelectionListener(e ->
                _selectionSet.setRoles(e.getAllSelectedItems()));
        _resourceList.addSelectionListener(e ->
                _selectionSet.setResources(e.getAllSelectedItems()));
        _categoryList.addSelectionListener(e ->
                _selectionSet.setCategories(e.getAllSelectedItems()));
    }

    private ResourceList<String> createSelectedList() {
        ResourceList<String> list = new ResourceList<>("Selected Resources",
                new ArrayList<>());
        list.setHeight("510px");
        list.suppressAddAction();
        list.addRemoveButtonListener(e -> _selectionSet.removeItem(list.getSelected()));
        return list;
    }


    private FormLayout createContent() {
        FormLayout leftSide = new FormLayout();
        leftSide.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        leftSide.add(createListLayout("Roles", _roleList), 1);
        leftSide.add(createListLayout("Categories", _categoryList), 1);
        leftSide.add(createSpacedListLayout("Participants", _participantList), 1);
        leftSide.add(createListLayout("Assets", _resourceList), 1);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.add(leftSide, 2);
        form.add(_selectedList, 1);

        return form;
    }


    private <T> VerticalLayout createListLayout(String title, MultiSelectListBox<T> listbox) {
        VerticalLayout layout = new VerticalLayout(new Prompt(title), listbox);
        layout.setSpacing(false);
        layout.setPadding(false);
        return layout;
    }


    private <T> VerticalLayout createSpacedListLayout(String title,
                                                      MultiSelectListBox<T> listbox) {
        VerticalLayout layout = createListLayout(title, listbox);
        layout.getStyle().set("margin-top", "15px");
        return layout;
    }



    private void createButtons() {
        getButtonBar().getStyle().set("margin-top", "10px");
        getButtonBar().add(new Button("Cancel", event -> close()));
        _okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getButtonBar().add(_okButton);
    }


    private List<Participant> loadParticipants(ResourceClient client) {
        try {
            return client.getParticipants();
        }
        catch (ResourceGatewayException | IOException e) {
            Announcement.warn("Failed to retrieve participant list from engine: "
                    + e.getMessage());
        }
        return Collections.emptyList();
    }


    private List<AbstractResourceAttribute> loadRoles(ResourceClient client) {
        try {
            return client.getRoles();
        }
        catch (ResourceGatewayException | IOException e) {
            Announcement.warn("Failed to retrieve role list from engine: "
                    + e.getMessage());
        }
        return Collections.emptyList();
    }


    private List<NonHumanResource> loadResources(ResourceClient client) {
        try {
            return client.getNonHumanResources();
        }
        catch (ResourceGatewayException | IOException e) {
            Announcement.warn("Failed to retrieve assets list from engine: "
                    + e.getMessage());
        }
        return Collections.emptyList();
    }


    private List<NonHumanCategory> loadCategories(ResourceClient client) {
        try {
            return client.getNonHumanCategories();
        }
        catch (ResourceGatewayException | IOException e) {
            Announcement.warn("Failed to retrieve asset categories list from engine: "
                    + e.getMessage());
        }
        return Collections.emptyList();
    }


    private void loadCurrentResources(ResourceClient client, String itemID) {
        try {
            XNode secondary = client.getSecondaryResources(itemID);
            if (secondary == null) return;

            for (XNode node : secondary.getChildren()) {
                String id = node.getText();
                switch (node.getName()) {
                    case "participant" : initParticipant(id); break;
                    case "role" : initRole(id); break;
                    case "nonHumanResource" : initResource(id); break;
                    case "nonHumanCategory" : {
                        String subCat = node.getAttributeValue("subcategory");
                        initCategory(id, subCat);
                        break;
                    }
                }
            }
            _selectionSet.setParticipants(_participantList.getSelectedItems());
            _selectionSet.setRoles(_roleList.getSelectedItems());
            _selectionSet.setResources(_resourceList.getSelectedItems());
            _selectionSet.setCategories(_categoryList.getSelectedItems());
        }
        catch (ResourceGatewayException | IOException e) {
            Announcement.warn("Failed to retrieve current resources for work item: " +
                    e.getMessage());
        }
    }


    private void initParticipant(String id) {
        for (Participant p : _participants) {
            if (p.getID().equals(id)) {
                _participantList.select(p);
            }
        }
    }


    private void initRole(String id) {
        for (AbstractResourceAttribute r : _roles) {
            if (r.getID().equals(id)) {
                _roleList.select(r);
            }
        }
    }


    private void initResource(String id) {
        for (NonHumanResource r : _resources) {
            if (r.getID().equals(id)) {
                _resourceList.select(r);
            }
        }
    }


    private void initCategory(String id, String subcat) {
        String catName = "";
        for (NonHumanCategory cat : _categories) {
            if (cat.getID().equals(id)) {
                catName = cat.getName();
                break;
            }
        }
        if (subcat != null) {
            catName += " -> " + subcat;
        }
        for (String s : _categoryItems) {
            if (s.equals(catName)) {
                _categoryList.select(s);
                break;
            }
        }
    }


    private <T> void configure(MultiSelectListBox<T> listbox) {
        listbox.setWidth("250px");
        listbox.setHeight("210px");
        listbox.getElement().getStyle().set("overflow-y", "scroll");
        listbox.getElement().getStyle().set("flex-grow", "1");
        listbox.getElement().getStyle().set("border", "1px solid lightgray");
    }



    class SelectionSet {
        
        Map<String, Participant> pMap = new HashMap<>();
        Map<String, AbstractResourceAttribute> rMap = new HashMap<>();
        Map<String, NonHumanResource> nMap = new HashMap<>();
        Set<String> cSet = new HashSet<>();

        void setParticipants(Set<Participant> set) {
            pMap.clear();
            set.forEach(p -> pMap.put(p.getFullName(), p));
            refreshSelecteditems();
        }

        void setRoles(Set<AbstractResourceAttribute> set) {
            rMap.clear();
            set.forEach(r -> rMap.put(r.getName(), r));
            refreshSelecteditems();
        }

        void setResources(Set<NonHumanResource> set) {
            nMap.clear();
            set.forEach(n -> nMap.put(n.getName(), n));
            refreshSelecteditems();
        }

        void setCategories(Set<String> set) {
            cSet.clear();
            cSet.addAll(set);
            refreshSelecteditems();
        }

        void removeItem(String item) {
            if (deselectParticipant(item) || deselectRole(item) ||
                    deselectResource(item) || deselectCategory(item)) {
                refreshSelecteditems();
            }
        }

        boolean deselectParticipant(String label) {
            Participant p = pMap.remove(label);
            if (p != null) {
                _participantList.deselect(p);
            }
            return p != null;
        }

        boolean deselectRole(String label) {
            AbstractResourceAttribute r = rMap.remove(label);
            if (r != null) {
                _roleList.deselect(r);
            }
            return r != null;
        }

        boolean deselectResource(String label) {
            NonHumanResource n = nMap.remove(label);
            if (n != null) {
                _resourceList.deselect(n);
            }
            return n != null;
        }

        boolean deselectCategory(String label) {
            if (cSet.remove(label)) {
                _categoryList.deselect(label);
                return true;
            }
            return false;
        }

        void refreshSelecteditems() {
            List<String> labelList = new ArrayList<>();
            labelList.addAll(pMap.keySet());
            labelList.addAll(rMap.keySet());
            labelList.addAll(nMap.keySet());
            labelList.addAll(cSet);
            Collections.sort(labelList);
            _selectedList.refresh(labelList);
        }

        XNode toXML() {
            XNode node = new XNode("secondary");
            pMap.values().forEach(p -> node.addChild("participant", p.getID()));
            rMap.values().forEach(r -> node.addChild("role", r.getID()));
            nMap.values().forEach(n -> node.addChild("nonHumanResource", n.getID()));
            cSet.forEach(c -> {
                String cat = c;
                String subcat = null;
                 if (c.contains("->")) {
                     String[] parts = c.split(" -> ");
                     cat = parts[0];
                     subcat = parts[1];
                 }    
                 XNode child = node.addChild("nonHumanCategory", findCategoryID(cat));
                 if (subcat != null) {
                     child.addAttribute("subcategory", subcat);
                 }
            });
            return node.hasChildren() ? node : null;
        }

        String findCategoryID(String catName) {
            for (NonHumanCategory cat : _categories) {
                if (cat.getName().equals(catName)) {
                    return cat.getID();
                }
            }
            return null;
        }
    }

}
