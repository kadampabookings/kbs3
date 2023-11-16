package org.kadampabookings.kbs.frontoffice.activities.podcast;

import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter;
import org.kadampabookings.kbs.frontoffice.operations.routes.podcast.RouteToPodcastRequest;

public class RouteToPodcastRequestEmitter implements RouteRequestEmitter {

    @Override
    public RouteRequest instantiateRouteRequest(UiRouteActivityContext context) {
        return new RouteToPodcastRequest(context.getHistory());
    }
}
