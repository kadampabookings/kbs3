package org.kadampabookings.kbs.frontoffice.activities.home;

import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter;
import org.kadampabookings.kbs.frontoffice.operations.routes.home.RouteToHomeRequest;

public class RouteToHomeRequestEmitter implements RouteRequestEmitter {

    public RouteToHomeRequestEmitter() {
        System.out.println();
    }

    @Override
    public RouteRequest instantiateRouteRequest(UiRouteActivityContext context) {
        return new RouteToHomeRequest(context.getHistory());
    }
}
