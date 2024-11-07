package org.kadampabookings.kbs.frontoffice.activities.podcasts;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.stack.routing.uirouter.UiRoute;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.uirouter.impl.UiRouteImpl;
import dev.webfx.stack.routing.uirouter.operations.RoutePushRequest;
import dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter;
import dev.webfx.stack.ui.operation.HasOperationCode;

public final class PodcastsRouting {

    private final static String PATH = "/podcasts";
    private final static String OPERATION_CODE = "RouteToPodcasts";

    public static String getPath() {
        return PATH;
    }

    public static class PodcastsUiRoute extends UiRouteImpl {

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

    public static class RouteToPodcastsRequest extends RoutePushRequest implements HasOperationCode {

        public RouteToPodcastsRequest(BrowsingHistory browsingHistory) {
            super(getPath(), browsingHistory);
        }

        @Override
        public Object getOperationCode() {
            return OPERATION_CODE;
        }
    }

    public static class RouteToPodcastsRequestEmitter implements RouteRequestEmitter {

        @Override
        public RouteRequest instantiateRouteRequest(UiRouteActivityContext context) {
            return new RouteToPodcastsRequest(context.getHistory());
        }
    }
}
