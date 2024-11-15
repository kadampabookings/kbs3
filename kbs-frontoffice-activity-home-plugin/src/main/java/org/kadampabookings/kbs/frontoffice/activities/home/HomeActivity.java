package org.kadampabookings.kbs.frontoffice.activities.home;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.ui.action.ActionBinder;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.VBox;
import one.modality.base.client.application.RoutingActions;
import org.kadampabookings.kbs.frontoffice.activities.books.BooksRouting;
import org.kadampabookings.kbs.frontoffice.activities.news.NewsRouting;
import org.kadampabookings.kbs.frontoffice.activities.podcasts.PodcastsRouting;

import java.util.function.Function;

final class HomeActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin {

    @Override
    public Node buildUi() {
        Hyperlink newsLink     = routingHyperlink(NewsRouting.RouteToNewsRequest::new);
        Hyperlink podcastsLink = routingHyperlink(PodcastsRouting.RouteToPodcastsRequest::new);
        Hyperlink booksLink    = routingHyperlink(BooksRouting.RouteToBooksRequest::new);
        VBox container = new VBox(30, newsLink, podcastsLink, booksLink);
        container.setAlignment(Pos.CENTER);
        return container;
    }

    private Hyperlink routingHyperlink(Function<BrowsingHistory, ?> routeRequestFactory) {
        Hyperlink hyperlink = ActionBinder.newActionHyperlink(RoutingActions.newRoutingAction(routeRequestFactory, this));
        FXProperties.setEvenIfBound(hyperlink.visibleProperty(), true);
        FXProperties.setEvenIfBound(hyperlink.disableProperty(), false);
        return hyperlink;
    }

}
