package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.routing.uirouter.UiRoute;
import dev.webfx.stack.routing.uirouter.impl.UiRouteImpl;
import org.kadampabookings.kbs.frontoffice.activities.news.routing.ArticleRouting;

public class ArticleUiRoute extends UiRouteImpl {

    public ArticleUiRoute() {
        super(uiRoute());
    }

    public static UiRoute<?> uiRoute() {
        return UiRoute.create(ArticleRouting.getPath()
                , false
                , ArticleActivity::new
                , ViewDomainActivityContextFinal::new
        );
    }
}
