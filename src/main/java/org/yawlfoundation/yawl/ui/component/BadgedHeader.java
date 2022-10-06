package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * @author Michael Adams
 * @date 23/9/2022
 */
public class BadgedHeader<T extends HtmlContainer> extends HorizontalLayout {

    private final T _title;
    private final Span _badge;

    public BadgedHeader(T title, int count) {
        super();
        setPadding(false);
        setMargin(false);
        _title = title;
        _badge = createBadge(count);
        add(_title, _badge);
    }


    public void update(int count) {
        _badge.getElement().setText(String.valueOf(count));
    }

    
    private Span createBadge(int value) {
   		Span badge = new Span(String.valueOf(value));
   		badge.getElement().getThemeList().add("badge small contrast");
   		badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
   		return badge;
   	}

}
