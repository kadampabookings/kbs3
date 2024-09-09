package org.kadampabookings.kbs.frontoffice.activities.books;

import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.routing.uirouter.UiRoute;
import dev.webfx.stack.routing.uirouter.impl.UiRouteImpl;
import org.kadampabookings.kbs.frontoffice.activities.books.routing.BooksRouting;

public class BooksUiRoute extends UiRouteImpl {

    public BooksUiRoute() {
        super(uiRoute());
    }

    public static UiRoute<?> uiRoute() {
        return UiRoute.create(BooksRouting.getPath()
                , false
                , BooksActivity::new
                , ViewDomainActivityContextFinal::new
        );
    }
}
