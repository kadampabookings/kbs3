package org.kadampabookings.kbs.frontoffice.activities.home;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.i18n.HasI18nKey;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.stack.routing.uirouter.UiRoute;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.uirouter.impl.UiRouteImpl;
import dev.webfx.stack.routing.uirouter.operations.RoutePushRequest;
import dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter;
import dev.webfx.stack.ui.operation.HasOperationCode;

public final class FrontOfficeHomeRouting {

    private final static String PATH = "/home";
    private final static String OPERATION_CODE = "RouteToHome";

    public static String getPath() {
        return PATH;
    }

    public static class HomeUiRoute extends UiRouteImpl {

        public HomeUiRoute() {
            super(uiRoute());
        }

        public static UiRoute<?> uiRoute() {
            return UiRoute.create(FrontOfficeHomeRouting.getPath()
                    , false
                    , FrontOfficeHomeActivity::new
                    , ViewDomainActivityContextFinal::new
            );
        }
    }

    public static class RouteToHomeRequest extends RoutePushRequest implements HasOperationCode, HasI18nKey {

        public RouteToHomeRequest(BrowsingHistory browsingHistory) {
            super(getPath(), browsingHistory);
        }

        @Override
        public Object getOperationCode() {
            return OPERATION_CODE;
        }

        @Override
        public Object getI18nKey() {
            return FrontOfficeHomeI18nKeys.Home;
        }
    }

    public static class RouteToHomeRequestEmitter implements RouteRequestEmitter {

        @Override
        public RouteRequest instantiateRouteRequest(UiRouteActivityContext context) {
            return new RouteToHomeRequest(context.getHistory());
        }
    }
}
