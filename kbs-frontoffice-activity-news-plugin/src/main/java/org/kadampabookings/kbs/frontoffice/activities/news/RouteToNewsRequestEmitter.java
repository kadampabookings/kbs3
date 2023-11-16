package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter;
import org.kadampabookings.kbs.frontoffice.operations.routes.news.RouteToNewsRequest;

public class RouteToNewsRequestEmitter implements RouteRequestEmitter {

    @Override
    public RouteRequest instantiateRouteRequest(UiRouteActivityContext context) {
        return new RouteToNewsRequest(context.getHistory());
    }
}
