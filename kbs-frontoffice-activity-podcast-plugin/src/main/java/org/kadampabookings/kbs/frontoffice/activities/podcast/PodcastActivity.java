package org.kadampabookings.kbs.frontoffice.activities.podcast;

import dev.webfx.extras.util.control.ControlUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.orm.dql.DqlStatement;
import dev.webfx.stack.orm.reactive.entities.entities_to_objects.IndividualEntityToObjectMapper;
import dev.webfx.stack.orm.reactive.entities.entities_to_objects.ReactiveObjectsMapper;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import one.modality.base.frontoffice.utility.GeneralUtility;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.shared.entities.Podcast;
import org.kadampabookings.kbs.frontoffice.activities.podcast.views.PodcastView;

public final class PodcastActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin {

    private final BorderPane homeContainer = new BorderPane();
    private final VBox pageContainer = new VBox(); // The main container inside the vertical scrollbar
    private final VBox podcastsContainer = new VBox(20);
    public final IntegerProperty podcastsLimitProperty = new SimpleIntegerProperty(5);

    @Override
    public Node buildUi() {

        Label podcastLabel = GeneralUtility.createLabel("podcastLabel", Color.web(StyleUtility.MAIN_BLUE), 21);
        VBox.setMargin(podcastLabel, new Insets(50, 0, 50, 0));

        pageContainer.setAlignment(Pos.CENTER);
        Insets containerMargins = new Insets(30, 20, 10, 20);
        VBox.setMargin(podcastsContainer, containerMargins);
        pageContainer.getChildren().setAll(
                podcastLabel,
                podcastsContainer
        );

        FXProperties.runOnPropertiesChange(() -> {
            double width = pageContainer.getWidth();
            GeneralUtility.screenChangeListened(width);
        }, pageContainer.widthProperty());

        // Setting a max width for big desktop screens
        pageContainer.setMaxWidth(1200); // Similar value as our website

        // Embedding the page in a ScrollPane. The page itself is embedded in a BorderPane in order to keep the page
        // centered when it reaches its max width (without the BorderPane, the ScrollPane would position it on left).
        ScrollPane scrollPane = ControlUtil.createVerticalScrollPane(new BorderPane(pageContainer));

        homeContainer.setCenter(scrollPane);

        scrollPane.vvalueProperty().addListener((observable, oldValue, vValue) -> {
            if (vValue.doubleValue() >= scrollPane.getVmax() * 0.999)
                podcastsLimitProperty.set(podcastsLimitProperty.get() + 5);
        });

        return homeContainer;
    }

    @Override
    protected void startLogic() {

        ReactiveObjectsMapper.<Podcast, Node>createPushReactiveChain(this)
                .always("{class: 'Podcast', fields: 'channel, channelPodcastId, date, title, excerpt, imageUrl, audioUrl, durationMillis', orderBy: 'date desc, id desc'}")
                .always(podcastsLimitProperty, limit -> DqlStatement.limit("?", limit))
                .setIndividualEntityToObjectMapperFactory(IndividualEntityToObjectMapper.createFactory(PodcastView::new, PodcastView::setPodcast, PodcastView::getView))
                .storeMappedObjectsInto(podcastsContainer.getChildren())
                .start();
    }

}
