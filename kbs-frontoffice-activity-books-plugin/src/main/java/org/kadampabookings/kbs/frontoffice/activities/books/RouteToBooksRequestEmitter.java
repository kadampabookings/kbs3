package org.kadampabookings.kbs.frontoffice.activities.books;

import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter;
import org.kadampabookings.kbs.frontoffice.operations.routes.books.RouteToBooksRequest;

public class RouteToBooksRequestEmitter implements RouteRequestEmitter {

    @Override
    public RouteRequest instantiateRouteRequest(UiRouteActivityContext context) {
        return new RouteToBooksRequest(context.getHistory());
    }
}
