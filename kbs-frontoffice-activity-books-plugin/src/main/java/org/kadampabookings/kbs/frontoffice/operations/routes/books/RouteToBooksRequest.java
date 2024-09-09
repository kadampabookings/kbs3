package org.kadampabookings.kbs.frontoffice.operations.routes.books;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.routing.uirouter.operations.RoutePushRequest;
import dev.webfx.stack.ui.operation.HasOperationCode;
import org.kadampabookings.kbs.frontoffice.activities.books.routing.BooksRouting;

public class RouteToBooksRequest extends RoutePushRequest implements HasOperationCode {

    public RouteToBooksRequest(BrowsingHistory browsingHistory) {
        super(BooksRouting.getPath(), browsingHistory);
    }

    @Override
    public Object getOperationCode() {
        return "RouteToBooks";
    }
}
