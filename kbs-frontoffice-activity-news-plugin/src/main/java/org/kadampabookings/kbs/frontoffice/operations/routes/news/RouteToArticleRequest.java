package org.kadampabookings.kbs.frontoffice.operations.routes.news;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.routing.uirouter.operations.RoutePushRequest;
import org.kadampabookings.kbs.frontoffice.activities.news.routing.ArticleRouting;

public class RouteToArticleRequest extends RoutePushRequest {

    public RouteToArticleRequest(Object articleId, BrowsingHistory browsingHistory) {
        super(ArticleRouting.getArticlePath(articleId), browsingHistory);
    }

}
