package org.kadampabookings.kbs.frontoffice.activities.home;

import dev.webfx.extras.panes.ColumnsPane;
import dev.webfx.kit.util.properties.ObservableLists;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.modality.base.frontoffice.utility.page.FOPageUtil;
import org.kadampabookings.kbs.client.festivaltypes.FXFestivals;

final class FrontOfficeHomeActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin {

    @Override
    protected void startLogic() {
        FXFestivals.init();
    }

    @Override
    public Node buildUi() {
        ColumnsPane columnsPane = new ColumnsPane();
        columnsPane.setMaxWidth(Double.MAX_VALUE);
        columnsPane.setHgap(40);

/*
        Hyperlink newsLink     = routingHyperlink(NewsRouting.RouteToNewsRequest::new);
        Hyperlink podcastsLink = routingHyperlink(PodcastsRouting.RouteToPodcastsRequest::new);
        Hyperlink booksLink    = routingHyperlink(BooksRouting.RouteToBooksRequest::new);
*/

        Label festivalsHeaderLabel = I18nControls.newLabel(FrontOfficeHomeI18nKeys.FestivalsHeader);
        festivalsHeaderLabel.getStyleClass().setAll("festivals-header");

        Hyperlink moreEventsLabel = I18nControls.newHyperlink(FrontOfficeHomeI18nKeys.MoreEvents);
        moreEventsLabel.getStyleClass().setAll("more-events");

        VBox container = new VBox(64,
            festivalsHeaderLabel,
            columnsPane,
            moreEventsLabel,
            HomePodcastsView.createView(getHistory())
        );
        container.setAlignment(Pos.TOP_CENTER);

        ObservableLists.bindConverted(columnsPane.getChildren(), FXFestivals.lastFestivals(), festival ->
            new FestivalThumbnail(festival).getView());

        FOPageUtil.restrictToMaxPageWidth(columnsPane);

        return FOPageUtil.applyTopBottomPagePadding(container);
    }

/*
    private Hyperlink routingHyperlink(Function<BrowsingHistory, ?> routeRequestFactory) {
        Hyperlink hyperlink = ActionBinder.newActionHyperlink(RoutingActions.newRoutingAction(routeRequestFactory, this));
        FXProperties.setEvenIfBound(hyperlink.visibleProperty(), true);
        FXProperties.setEvenIfBound(hyperlink.disableProperty(), false);
        return hyperlink;
    }
*/

}
