package org.kadampabookings.kbs.frontoffice.operations.routes.podcasts;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.routing.uirouter.operations.RoutePushRequest;
import dev.webfx.stack.ui.operation.HasOperationCode;
import org.kadampabookings.kbs.frontoffice.activities.podcasts.routing.PodcastsRouting;

public class RouteToPodcastsRequest extends RoutePushRequest implements HasOperationCode {

    public RouteToPodcastsRequest(BrowsingHistory browsingHistory) {
        super(PodcastsRouting.getPath(), browsingHistory);
    }

    @Override
    public Object getOperationCode() {
        return "RouteToPodcasts";
    }
}
