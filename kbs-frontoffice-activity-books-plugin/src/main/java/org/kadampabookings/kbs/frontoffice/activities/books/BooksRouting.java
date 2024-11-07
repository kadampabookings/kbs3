package org.kadampabookings.kbs.frontoffice.activities.books;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.stack.routing.uirouter.UiRoute;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.uirouter.impl.UiRouteImpl;
import dev.webfx.stack.routing.uirouter.operations.RoutePushRequest;
import dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter;
import dev.webfx.stack.ui.operation.HasOperationCode;

public final class BooksRouting {

    private static final String PATH = "/books";
    private final static String OPERATION_CODE = "RouteToBooks";

    public static String getPath() {
        return PATH;
    }

    public static class BooksUiRoute extends UiRouteImpl {

        public BooksUiRoute() {
            super(uiRoute());
        }

        public static UiRoute<?> uiRoute() {
            return UiRoute.create(BooksRouting.getPath()
                    , false
                    , BooksActivity::new
                    , ViewDomainActivityContextFinal::new
            );
        }
    }

    public static class RouteToBooksRequest extends RoutePushRequest implements HasOperationCode {

        public RouteToBooksRequest(BrowsingHistory browsingHistory) {
            super(getPath(), browsingHistory);
        }

        @Override
        public Object getOperationCode() {
            return OPERATION_CODE;
        }
    }

    public static class RouteToBooksRequestEmitter implements RouteRequestEmitter {

        @Override
        public RouteRequest instantiateRouteRequest(UiRouteActivityContext context) {
            return new RouteToBooksRequest(context.getHistory());
        }
    }
}
