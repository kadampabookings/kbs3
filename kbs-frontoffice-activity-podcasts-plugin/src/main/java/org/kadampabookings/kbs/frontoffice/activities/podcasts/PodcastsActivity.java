package org.kadampabookings.kbs.frontoffice.activities.podcasts;

import dev.webfx.extras.panes.ScaleMode;
import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.util.control.ControlUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.orm.dql.DqlStatement;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.controls.entity.selector.EntityButtonSelector;
import dev.webfx.stack.orm.reactive.entities.entities_to_objects.IndividualEntityToObjectMapper;
import dev.webfx.stack.orm.reactive.entities.entities_to_objects.ReactiveObjectsMapper;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import one.modality.base.client.activity.ModalityButtonFactoryMixin;
import one.modality.base.client.mainframe.dialogarea.fx.FXMainFrameDialogArea;
import one.modality.base.frontoffice.utility.GeneralUtility;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.shared.entities.Podcast;
import one.modality.base.shared.entities.Teacher;
import one.modality.base.shared.entities.impl.TeacherImpl;

public final class PodcastsActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin, ModalityButtonFactoryMixin {

    private static final Teacher FAVORITE_TAB_VIRTUAL_TEACHER = new TeacherImpl(EntityId.create(Teacher.class), null);

    private final BorderPane homeContainer = new BorderPane();
    private final VBox pageContainer = new VBox(); // The main container inside the vertical scrollbar
    private final VBox podcastsContainer = new VBox(20);
    public final IntegerProperty podcastsLimitProperty = new SimpleIntegerProperty(5);
    private final ObjectProperty<Teacher> teacherProperty = new SimpleObjectProperty<>();

    @Override
    public Node buildUi() {

        Label podcastsLabel = GeneralUtility.createLabel("podcastsLabel", Color.web(StyleUtility.MAIN_ORANGE),  true, 21);
        podcastsLabel.setContentDisplay(ContentDisplay.TOP);

        Text teacherPrefixText = I18n.bindI18nProperties(new Text(), "teacher");
        teacherPrefixText.setFill(Color.GRAY);
        EntityButtonSelector<Teacher> teacherButtonSelector = new EntityButtonSelector<Teacher>(
                "{class: 'Teacher', alias: 't', columns: 'name', where: '(select count(1) from Podcast where teacher = t) > 0', orderBy: 'id'}",
                this, FXMainFrameDialogArea::getDialogArea, getDataSourceModel()
        ) { // Overriding the button content to add the "Teacher" prefix text
            @Override
            protected Node getOrCreateButtonContentFromSelectedItem() {
                return new HBox(10, teacherPrefixText, super.getOrCreateButtonContentFromSelectedItem());
            }
        }.appendNullEntity(true); // Also adding null entity that will represent all teachers
        // Creating a virtual teacher named "All" that will be used to select all teachers
        EntityStore store = teacherButtonSelector.getStore();
        Teacher allTeacher = store.createEntity(Teacher.class);
        teacherButtonSelector.setVisualNullEntity(allTeacher);
        // Binding teacher name with "all" i18n key
        StringProperty allTeacherNameProperty; // keeping a reference to prevent GC
        I18n.bindI18nTextProperty(allTeacherNameProperty = new SimpleStringProperty() {
            @Override
            protected void invalidated() {
                allTeacher.setName(get()); // Updating teacher name
                teacherButtonSelector.updateButtonContentFromSelectedItem(); // Refreshing button content in case it was the selected teacher
            }
        }, "all");

        Button teacherButton = teacherButtonSelector.getButton();
        teacherButton.getProperties().put("dontGarbage", allTeacherNameProperty);
        teacherButton.setMaxWidth(Region.USE_PREF_SIZE);
        ScalePane scaledTeacherButton = new ScalePane(ScaleMode.FIT_WIDTH, teacherButton);

        teacherProperty.bind(teacherButtonSelector.selectedItemProperty());

        // Setting a max width for big desktop screens
        pageContainer.setMaxWidth(1200); // Similar value as our website
        pageContainer.setAlignment(Pos.CENTER);
        BorderPane.setMargin(pageContainer, new Insets(0, 20, 0, 20)); // Global page padding
        VBox.setMargin(podcastsLabel, new Insets(20, 0, 40, 0));
        VBox.setMargin(podcastsContainer, new Insets(80, 0, 10, 0));
        pageContainer.getChildren().setAll(
                podcastsLabel,
                scaledTeacherButton,
                podcastsContainer
        );

        FXProperties.runOnPropertiesChange(() -> {
            double width = pageContainer.getWidth();
            GeneralUtility.screenChangeListened(width);
            // Setting the teacher button max scale proportionally to the width but always between 1 & 2.5
            scaledTeacherButton.setMaxScale(Math.max(1, Math.min(width / 600, 2.5)));
        }, pageContainer.widthProperty());

        // Embedding the page in a ScrollPane. The page itself is embedded in a BorderPane in order to keep the page
        // centered when it reaches its max width (without the BorderPane, the ScrollPane would position it on left).
        ScrollPane scrollPane = ControlUtil.createVerticalScrollPane(new BorderPane(pageContainer));
        homeContainer.setCenter(scrollPane);
        // Automatically increasing the podcast list when the user scroll down up to bottom
        scrollPane.vvalueProperty().addListener((observable, oldValue, vValue) -> {
            if (vValue.doubleValue() >= scrollPane.getVmax() * 0.999)
                podcastsLimitProperty.set(podcastsLimitProperty.get() + 5);
        });

        return homeContainer;
    }

    @Override
    protected void startLogic() {
        ReactiveObjectsMapper.<Podcast, Node>createPushReactiveChain(this)
                .always("{class: 'Podcast', fields: 'channel, channelPodcastId, date, title, excerpt, imageUrl, audioUrl, durationMillis', where: 'durationMillis != null', orderBy: 'date desc, id desc'}")
                .always(podcastsLimitProperty, limit -> DqlStatement.limit("?", limit))
                .ifNotNull(teacherProperty, teacher -> teacher == FAVORITE_TAB_VIRTUAL_TEACHER ? DqlStatement.whereFieldIn("id", FXFavoritePodcasts.getFavoritePodcastIds().toArray()) : DqlStatement.where("teacher = ?", teacher))
                .setIndividualEntityToObjectMapperFactory(IndividualEntityToObjectMapper.createFactory(PodcastView::new, PodcastView::setPodcast, PodcastView::getView))
                .storeMappedObjectsInto(podcastsContainer.getChildren())
                .start();
    }

}
