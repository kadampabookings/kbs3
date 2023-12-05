package org.kadampabookings.kbs.frontoffice.activities.podcasts;

import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.routing.uirouter.UiRoute;
import dev.webfx.stack.routing.uirouter.impl.UiRouteImpl;
import org.kadampabookings.kbs.frontoffice.activities.podcasts.routing.PodcastsRouting;

public class PodcastsUiRoute extends UiRouteImpl {

    public PodcastsUiRoute() {
        super(uiRoute());
    }

    public static UiRoute<?> uiRoute() {
        return UiRoute.create(PodcastsRouting.getPath()
                , false
                , PodcastsActivity::new
                , ViewDomainActivityContextFinal::new
        );
    }
}
