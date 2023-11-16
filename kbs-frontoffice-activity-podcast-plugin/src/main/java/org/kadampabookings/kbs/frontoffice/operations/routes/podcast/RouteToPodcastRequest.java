package org.kadampabookings.kbs.frontoffice.operations.routes.podcast;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.routing.uirouter.operations.RoutePushRequest;
import dev.webfx.stack.ui.operation.HasOperationCode;
import org.kadampabookings.kbs.frontoffice.activities.podcast.routing.PodcastRouting;

public class RouteToPodcastRequest extends RoutePushRequest implements HasOperationCode {

    public RouteToPodcastRequest(BrowsingHistory browsingHistory) {
        super(PodcastRouting.getPath(), browsingHistory);
    }

    @Override
    public Object getOperationCode() {
        return "RouteToPodcast";
    }
}
