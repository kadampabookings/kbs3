package org.kadampabookings.kbs.frontoffice.activities.podcast;

import dev.webfx.extras.panes.FlexPane;
import dev.webfx.extras.util.control.ControlUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.orm.dql.DqlStatement;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.reactive.entities.entities_to_objects.IndividualEntityToObjectMapper;
import dev.webfx.stack.orm.reactive.entities.entities_to_objects.ReactiveObjectsMapper;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import one.modality.base.client.tile.Tab;
import one.modality.base.client.tile.TabsBar;
import one.modality.base.frontoffice.utility.GeneralUtility;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.shared.entities.Podcast;
import one.modality.base.shared.entities.Teacher;
import org.kadampabookings.kbs.frontoffice.activities.podcast.views.PodcastView;

public final class PodcastActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin, one.modality.base.client.activity.ModalityButtonFactoryMixin {

    private final BorderPane homeContainer = new BorderPane();
    private final VBox pageContainer = new VBox(); // The main container inside the vertical scrollbar
    private final VBox podcastsContainer = new VBox(20);
    public final IntegerProperty podcastsLimitProperty = new SimpleIntegerProperty(5);
    private final ObjectProperty<Teacher> teacherProperty = new SimpleObjectProperty<>();

    @Override
    public Node buildUi() {

        FlexPane tabsPane = new FlexPane();
        EntityStore.create(DataSourceModelService.getDefaultDataSourceModel())
                .<Teacher>executeQuery("select name from Teacher t where (select count(1) from Podcast where teacher = t) > 10 order by id")
                .onFailure(Console::log)
                .onSuccess(teachers -> Platform.runLater(() -> {
                    TabsBar<Teacher> tabsBar = new TabsBar<>(PodcastActivity.this, teacherProperty::set);
                    tabsBar.setTabs(createTeacherTab(tabsBar, null));
                    tabsBar.addTabs(Collections.map(teachers, t -> createTeacherTab(tabsBar, t)));
                    tabsPane.getChildren().setAll(tabsBar.getTabs());
                }));

        Label podcastLabel = GeneralUtility.createLabel("podcastLabel", Color.web(StyleUtility.MAIN_BLUE), 21);
        VBox.setMargin(podcastLabel, new Insets(50, 0, 50, 0));

        pageContainer.setAlignment(Pos.CENTER);
        Insets containerMargins = new Insets(30, 20, 10, 20);
        VBox.setMargin(podcastsContainer, containerMargins);
        pageContainer.getChildren().setAll(
                podcastLabel,
                tabsPane,
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

    private static Tab createTeacherTab(TabsBar<Teacher> tabsBar, Teacher teacher) {
        Tab tab = tabsBar.createTab(teacher == null ? "All" : teacher.getName(), teacher);
        tab.setPadding(new Insets(5));
        return tab;
    }

    @Override
    protected void startLogic() {

        ReactiveObjectsMapper.<Podcast, Node>createPushReactiveChain(this)
                .always("{class: 'Podcast', fields: 'channel, channelPodcastId, date, title, excerpt, imageUrl, audioUrl, durationMillis', orderBy: 'date desc, id desc'}")
                .always(podcastsLimitProperty, limit -> DqlStatement.limit("?", limit))
                .ifNotNull(teacherProperty, teacher -> DqlStatement.where("teacher = ?", teacher))
                .setIndividualEntityToObjectMapperFactory(IndividualEntityToObjectMapper.createFactory(PodcastView::new, PodcastView::setPodcast, PodcastView::getView))
                .storeMappedObjectsInto(podcastsContainer.getChildren())
                .start();
    }

}
