package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.routing.uirouter.UiRoute;
import dev.webfx.stack.routing.uirouter.impl.UiRouteImpl;
import dev.webfx.stack.routing.uirouter.operations.RoutePushRequest;
import one.modality.base.client.util.routing.ModalityRoutingUtil;

/**
 * @author Bruno Salmon
 */
public final class ArticleRouting {

    private final static String PATH = "/news/:articleId";

    public static String getPath() {
        return PATH;
    }

    public static String getArticlePath(Object articleId) {
        return ModalityRoutingUtil.interpolateParamInPath(":articleId", Entities.getPrimaryKey(articleId), PATH);
    }

    public static class ArticleUiRoute extends UiRouteImpl {

        public ArticleUiRoute() {
            super(uiRoute());
        }

        public static UiRoute<?> uiRoute() {
            return UiRoute.create(ArticleRouting.getPath()
                    , false
                    , ArticleActivity::new
                    , ViewDomainActivityContextFinal::new
            );
        }
    }

    public static class RouteToArticleRequest extends RoutePushRequest {

        public RouteToArticleRequest(Object articleId, BrowsingHistory browsingHistory) {
            super(getArticlePath(articleId), browsingHistory);
        }

    }
}
