package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.stack.routing.uirouter.UiRoute;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.uirouter.impl.UiRouteImpl;
import dev.webfx.stack.routing.uirouter.operations.RoutePushRequest;
import dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter;
import dev.webfx.stack.ui.operation.HasOperationCode;

public final class NewsRouting {

    private final static String PATH = "/news";
    private final static String OPERATION_CODE = "RouteToNews";

    public static String getPath() {
        return PATH;
    }

    public static class NewsUiRoute extends UiRouteImpl {

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

    public static class RouteToNewsRequest extends RoutePushRequest implements HasOperationCode {

        public RouteToNewsRequest(BrowsingHistory browsingHistory) {
            super(getPath(), browsingHistory);
        }

        @Override
        public Object getOperationCode() {
            return OPERATION_CODE;
        }
    }

    public static class RouteToNewsRequestEmitter implements RouteRequestEmitter {

        @Override
        public RouteRequest instantiateRouteRequest(UiRouteActivityContext context) {
            return new RouteToNewsRequest(context.getHistory());
        }
    }
}
