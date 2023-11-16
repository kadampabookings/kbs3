package org.kadampabookings.kbs.frontoffice.operations.routes.news;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.routing.uirouter.operations.RoutePushRequest;
import dev.webfx.stack.ui.operation.HasOperationCode;
import org.kadampabookings.kbs.frontoffice.activities.news.routing.NewsRouting;

public class RouteToNewsRequest extends RoutePushRequest implements HasOperationCode {

    public RouteToNewsRequest(BrowsingHistory browsingHistory) {
        super(NewsRouting.getPath(), browsingHistory);
    }

    @Override
    public Object getOperationCode() {
        return "RouteToNews";
    }
}
