package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.routing.uirouter.UiRoute;
import dev.webfx.stack.routing.uirouter.impl.UiRouteImpl;
import org.kadampabookings.kbs.frontoffice.activities.news.routing.NewsRouting;

public class NewsUiRoute extends UiRouteImpl {

    public NewsUiRoute() {
        super(uiRoute());
    }

    public static UiRoute<?> uiRoute() {
        return UiRoute.create(NewsRouting.getPath()
                , false
                , NewsActivity::new
                , ViewDomainActivityContextFinal::new
        );
    }
}
