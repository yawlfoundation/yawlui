package org.yawlfoundation.yawl.ui.util;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;

import java.util.*;

/**
 *
 * @author Michael Adams
 * @date 8/5/2026
 */
public class TNodeParser {

    private final Map<String, Element> complexTypes = new HashMap<>();
    private final Namespace ns = Namespace.getNamespace("xs",
            "http://www.w3.org/2001/XMLSchema");


    public TNode parse(Element root) {
        return parse(root, findRootTypeName(root));
    }


    public TNode parse(Element root, String rootTypeName) {

        // 1. Index all global complexTypes by name
        for (Element child : root.getChildren("complexType", ns)) {
            complexTypes.put(child.getAttributeValue("name"), child);
        }

        // 2. Start recursive build from the root type
        return resolveType(rootTypeName, rootTypeName);
    }


    private TNode resolveType(String label, String typeName) {
        String typeUnprefixed = typeName.contains(":") ? typeName.split(":")[1] : typeName;
        TNode node = new TNode(label, typeUnprefixed);
        Element complex = complexTypes.get(typeUnprefixed);

        if (complex != null) {
            // Look for <xs:sequence> or <xs:all>
            Element compositor = complex.getChild("sequence", ns);
            if (compositor == null) compositor = complex.getChild("all", ns);

            if (compositor != null) {
                for (Element childEl : compositor.getChildren("element", ns)) {
                    String childName = childEl.getAttributeValue("name");
                    String childType = childEl.getAttributeValue("type");

                    // Recursive call to handle nested types
                    node.addChild(childName, resolveType(childName, childType));
                }
            }
        }
        return node;
    }


    private String findRootTypeName(Element root) {
        Set<String> userDefinedTypes = new HashSet<>();
        Set<String> referencedTypes = new HashSet<>();

        // 1. Get all names of defined complexTypes at the root level
        List<Element> children = root.getChildren("complexType", ns);
        for (Element child : children) {
            userDefinedTypes.add(child.getAttributeValue("name"));
        }

        // 2. Scan for all type references in children
        for (Element child : children) {
            Iterator<Element> descendants = child.getDescendants(Filters.element());
            while (descendants.hasNext()) {
                Element desc = descendants.next();
                String typeRef = desc.getAttributeValue("type");
                if (typeRef != null) {
                    String typeUnprefixed = typeRef.contains(":") ?
                            typeRef.split(":")[1] : typeRef;
                    referencedTypes.add(typeUnprefixed);
                }
            }
        }

        // 3. Root = Defined - Referenced
        userDefinedTypes.removeAll(referencedTypes);

        // Return the first one (usually there is only one true root)
        return userDefinedTypes.isEmpty() ? null : userDefinedTypes.iterator().next();
    }
    
}


