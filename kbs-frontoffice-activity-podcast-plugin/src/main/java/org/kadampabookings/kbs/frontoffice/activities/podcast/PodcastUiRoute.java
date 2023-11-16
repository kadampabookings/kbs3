package org.kadampabookings.kbs.frontoffice.activities.podcast;

import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.routing.uirouter.UiRoute;
import dev.webfx.stack.routing.uirouter.impl.UiRouteImpl;
import org.kadampabookings.kbs.frontoffice.activities.podcast.routing.PodcastRouting;

public class PodcastUiRoute extends UiRouteImpl {

    public PodcastUiRoute() {
        super(uiRoute());
    }

    public static UiRoute<?> uiRoute() {
        return UiRoute.create(PodcastRouting.getPath()
                , false
                , PodcastActivity::new
                , ViewDomainActivityContextFinal::new
        );
    }
}
