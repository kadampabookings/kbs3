package org.kadampabookings.kbs.frontoffice.activities.podcasts;

import dev.webfx.extras.panes.FlexPane;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.panes.ScaleMode;
import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.util.control.ControlUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.browser.Browser;
import dev.webfx.platform.console.Console;
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
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
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

        Label alsoAvailableOnLabel = GeneralUtility.createLabel("alsoAvailableOn", Color.web(StyleUtility.MAIN_ORANGE),  false, 8);
        FlexPane podcastsChannelPane = new FlexPane(
                createPodcastsChannelButton("Spotify", "https://open.spotify.com/show/5QPCFEyZz74nOHZbQr1B4z"),
                createPodcastsChannelButton("ApplePodcasts", "https://podcasts.apple.com/us/podcast/living-clarity/id1719104184"),
                createPodcastsChannelButton("AmazonMusic", "https://music.amazon.co.uk/podcasts/d64fa9da-7c91-4ee8-84e7-f05de30fdb2c/living-clarity"),
                createPodcastsChannelButton("PocketCasts", "https://pca.st/9yuq0l0p"));
        podcastsChannelPane.setMaxWidth(Double.MAX_VALUE);
        podcastsChannelPane.setHorizontalSpace(10);
        podcastsChannelPane.setVerticalSpace(10);

        Region separatorLine = new Region();
        separatorLine.setBackground(Background.fill(Color.web(StyleUtility.MAIN_ORANGE)));
        separatorLine.setMinHeight(1);
        separatorLine.setPrefWidth(Double.MAX_VALUE);

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
        VBox.setMargin(podcastsLabel, new Insets(20, 0, 20, 0));
        VBox.setMargin(podcastsChannelPane, new Insets(20, 0, 20, 0));
        VBox.setMargin(separatorLine, new Insets(10, 0, 40, 0));
        VBox.setMargin(podcastsContainer, new Insets(40, 0, 10, 0));

        pageContainer.getChildren().setAll(
                podcastsLabel,
                alsoAvailableOnLabel,
                podcastsChannelPane,
                separatorLine,
                scaledTeacherButton,
                podcastsContainer
        );

        FXProperties.runOnPropertiesChange(() -> {
            double width = pageContainer.getWidth();
            GeneralUtility.screenChangeListened(width);
            // Setting the teacher button max scale proportionally to the width but always between 1 & 2.5
            double scale = Math.max(1, Math.min(width / 600, 2.5));
            scaledTeacherButton.setMaxScale(scale);
            // Also the space above and below
            VBox.setMargin(separatorLine, new Insets(10, 0, 40 * scale, 0));
            VBox.setMargin(podcastsContainer, new Insets(40 * scale, 0, 10, 0));
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

    private Node createPodcastsChannelButton(String i18nKey, String url) {
        Label button = GeneralUtility.setupLabeled(new Label(), i18nKey, Color.BLACK, FontWeight.SEMI_BOLD, 12);
        button.setMinSize(200, 45);
        button.setPrefSize(240, 45);
        button.setMaxSize(240, 45);
        button.setPadding(new Insets(5, 10, 5, 10));
        button.setGraphicTextGap(10);
        CornerRadii radii = new CornerRadii(8);
        button.setBackground(new Background(new BackgroundFill(Color.WHITE, radii, null)));
        button.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, radii, BorderStroke.DEFAULT_WIDTHS)));
        button.setOnMouseClicked(e -> {
            try {
                Browser.launchExternalBrowser(url);
            } catch (Exception ex) {
                Console.log(e);
            }
        });
        button.setCursor(Cursor.HAND);
        // Embedding the button in an extensible mono pane (because the button itself is not)
        MonoPane monoPane = new MonoPane(button);
        monoPane.setMaxWidth(Double.MAX_VALUE); // Will be extended by the flex pane
        return monoPane;
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
