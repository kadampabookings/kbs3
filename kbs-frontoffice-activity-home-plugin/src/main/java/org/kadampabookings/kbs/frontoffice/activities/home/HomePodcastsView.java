package org.kadampabookings.kbs.frontoffice.activities.home;

import dev.webfx.extras.panes.ColumnsPane;
import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.i18n.controls.I18nControls;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.kadampabookings.kbs.frontoffice.activities.podcasts.PodcastsRouting;

/**
 * @author Bruno Salmon
 */
final class HomePodcastsView {

    static Node createView(BrowsingHistory browsingHistory) {

        Label podcastsHeaderLabel = I18nControls.newLabel(FrontOfficeHomeI18nKeys.PodcastsHeader);
        podcastsHeaderLabel.getStyleClass().setAll("podcasts-header");

        Button seeAllButton = I18nControls.newButton("See all");
        seeAllButton.setPrefSize(240, 48);
        seeAllButton.setOnAction(e -> {
            new PodcastsRouting.RouteToPodcastsRequest(browsingHistory).execute();
        });

        ColumnsPane columnsPane = new ColumnsPane(16, 16,
            PodcastsCard.createCard(FrontOfficeHomeI18nKeys.PodcastsCard1Title, FrontOfficeHomeI18nKeys.PodcastsCard1Content),
            PodcastsCard.createCard(FrontOfficeHomeI18nKeys.PodcastsCard2Title, FrontOfficeHomeI18nKeys.PodcastsCard2Content),
            PodcastsCard.createCard(FrontOfficeHomeI18nKeys.PodcastsCard3Title, FrontOfficeHomeI18nKeys.PodcastsCard3Content),
            PodcastsCard.createCard(FrontOfficeHomeI18nKeys.PodcastsCard4Title, FrontOfficeHomeI18nKeys.PodcastsCard4Content),
            PodcastsCard.createCard(FrontOfficeHomeI18nKeys.PodcastsCard5Title, FrontOfficeHomeI18nKeys.PodcastsCard5Content),
            PodcastsCard.createCard(FrontOfficeHomeI18nKeys.PodcastsCard6Title, FrontOfficeHomeI18nKeys.PodcastsCard6Content)
        );
        columnsPane.setFixedColumnCount(3);

        VBox container = new VBox(88,
            podcastsHeaderLabel,
            columnsPane,
            seeAllButton
        );
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(88));
        container.getStyleClass().setAll("home-podcasts");

        return container;
    }
}
