package org.kadampabookings.kbs.frontoffice.activities.home;

import dev.webfx.extras.panes.ColumnsPane;
import dev.webfx.kit.util.properties.ObservableLists;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.modality.base.frontoffice.utility.page.FOPageUtil;
import one.modality.base.shared.entities.Event;

import java.util.Collections;

final class HomeActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin {

    private final ObservableList<Event> festivals = FXCollections.observableArrayList();

    @Override
    protected void startLogic() {
        // Creating our own entity store to hold the loaded data without interfering with other activities
        EntityStore entityStore = EntityStore.create(getDataSourceModel()); // Activity datasource model is available at this point
        entityStore.<Event>executeQuery("select name,type.name,startDate,endDate from Event where type in (?, ?, ?) and name not like '%Online%' order by startDate desc limit 3",
                FestivalType.SPRING_FESTIVAL.getTypeId(), FestivalType.SUMMER_FESTIVAL.getTypeId(), FestivalType.FALL_FESTIVAL.getTypeId())
            .onFailure(Console::log)
            .onSuccess(events -> UiScheduler.runInUiThread(() -> {
                Collections.reverse(events);
                festivals.setAll(events); // events.reversed() is Java 21
            }));
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

        Label festivalsHeaderLabel = I18nControls.newLabel(HomeI18nKeys.FestivalsHeader);
        festivalsHeaderLabel.getStyleClass().setAll("festivals-header");

        Hyperlink moreEventsLabel = I18nControls.newHyperlink(HomeI18nKeys.MoreEvents);
        moreEventsLabel.getStyleClass().setAll("more-events");

        VBox container = new VBox(64,
            festivalsHeaderLabel,
            columnsPane,
            moreEventsLabel,
            HomePodcastsView.createView(getHistory())
        );
        container.setAlignment(Pos.TOP_CENTER);

        ObservableLists.bindConverted(columnsPane.getChildren(), festivals, festival ->
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
