package org.kadampabookings.kbs.frontoffice.activities.podcasts;

import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter;
import org.kadampabookings.kbs.frontoffice.operations.routes.podcasts.RouteToPodcastsRequest;

public class RouteToPodcastsRequestEmitter implements RouteRequestEmitter {

    @Override
    public RouteRequest instantiateRouteRequest(UiRouteActivityContext context) {
        return new RouteToPodcastsRequest(context.getHistory());
    }
}
