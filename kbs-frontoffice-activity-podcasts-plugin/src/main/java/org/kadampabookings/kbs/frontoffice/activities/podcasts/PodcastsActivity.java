package org.kadampabookings.kbs.frontoffice.activities.podcasts;

import dev.webfx.extras.panes.*;
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
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import one.modality.base.client.activity.ModalityButtonFactoryMixin;
import one.modality.base.client.mainframe.dialogarea.fx.FXMainFrameDialogArea;
import one.modality.base.client.tile.Tab;
import one.modality.base.client.tile.TabsBar;
import one.modality.base.frontoffice.utility.GeneralUtility;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.shared.entities.Podcast;
import one.modality.base.shared.entities.Teacher;
import one.modality.base.shared.entities.Topic;
import one.modality.base.shared.entities.impl.TeacherImpl;

public final class PodcastsActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin, ModalityButtonFactoryMixin {

    private static final Teacher FAVORITE_TAB_VIRTUAL_TEACHER = new TeacherImpl(EntityId.create(Teacher.class), null);

    private final VBox pageContainer = new VBox(); // The main container inside the vertical scrollbar
    private final VBox podcastsContainer = new VBox(20);
    public final IntegerProperty podcastsLimitProperty = new SimpleIntegerProperty(5);
    private final ObjectProperty<Teacher> teacherProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Topic> topicProperty = new SimpleObjectProperty<>();
    private final BooleanProperty virtuousTopicProperty = new SimpleBooleanProperty();

    @Override
    public Node buildUi() {

        Label podcastsLabel = GeneralUtility.createLabel("podcastsLabel", Color.web(StyleUtility.MAIN_ORANGE),  true, 21);
        podcastsLabel.setContentDisplay(ContentDisplay.TOP);
        podcastsLabel.setTextAlignment(TextAlignment.CENTER);

        Label alsoAvailableOnLabel = GeneralUtility.createLabel("alsoAvailableOn", Color.web(StyleUtility.MAIN_ORANGE),  false, 8);
        FlexPane podcastsChannelsPane = new FlexPane(
                createPodcastsChannelButton("Spotify",       "https://open.spotify.com/show/5QPCFEyZz74nOHZbQr1B4z"),
                createPodcastsChannelButton("ApplePodcasts", "https://podcasts.apple.com/us/podcast/living-clarity/id1719104184"),
                createPodcastsChannelButton("AmazonMusic",   "https://music.amazon.co.uk/podcasts/d64fa9da-7c91-4ee8-84e7-f05de30fdb2c/living-clarity"),
                createPodcastsChannelButton("PocketCasts",   "https://pca.st/9yuq0l0p"));
        podcastsChannelsPane.setMaxWidth(Double.MAX_VALUE);
        podcastsChannelsPane.setHorizontalSpace(10);
        podcastsChannelsPane.setVerticalSpace(10);
        podcastsChannelsPane.setDistributeRemainingRowSpace(true);

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
        teacherButton.setMinWidth(Region.USE_PREF_SIZE);
        teacherButton.setMaxWidth(Region.USE_PREF_SIZE);
        ScalePane scaledTeacherButton = new ScalePane(ScaleMode.FIT_WIDTH, teacherButton);
        scaledTeacherButton.setCanShrink(false);

        teacherProperty.bind(teacherButtonSelector.selectedItemProperty());

        TabsBar<Boolean> topicTabsBar = new TabsBar<>(this, virtuousTopicProperty::set);
        topicTabsBar.setTabs(
                createTopicTab(topicTabsBar, "nonVirtuousTopic", false),
                createTopicTab(topicTabsBar, "virtuousTopic", true)
        );
        ColumnsPane topicTabsPane = new ColumnsPane();
        topicTabsPane.getChildren().setAll(topicTabsBar.getTabs());
        topicTabsPane.setOnMouseClicked(Event::consume);

        Text allTopicText = new Text("All");
        MonoPane allTopicPane = new MonoPane(allTopicText);
        allTopicPane.setMaxWidth(Double.MAX_VALUE);
        allTopicPane.setMinHeight(40);
        allTopicPane.setOnMouseClicked(e -> topicProperty.set(null));
        allTopicPane.setCursor(Cursor.HAND);
        Text topicPrefixText = I18n.bindI18nProperties(new Text(), "topic");
        topicPrefixText.setFill(Color.GRAY);
        EntityButtonSelector<Topic> topicButtonSelector = new EntityButtonSelector<Topic>(
                "{class: 'Topic', alias: 't', columns: 'name', where: 'teaching', orderBy: 'id'}",
                this, FXMainFrameDialogArea::getDialogArea, getDataSourceModel()
        ) { // Overriding the button content to add the "Teacher" prefix text
            private final BorderPane bp = new BorderPane();

            @Override
            protected Node getOrCreateButtonContentFromSelectedItem() {
                return new HBox(10, topicPrefixText, super.getOrCreateButtonContentFromSelectedItem());
            }

            @Override
            protected Region getOrCreateDialogContent() {
                bp.setTop(new VBox(allTopicPane, topicTabsPane));
                bp.setCenter(super.getOrCreateDialogContent());
                return bp;
            }
        }.always(virtuousTopicProperty, virtuous -> DqlStatement.where("virtuous=?", virtuous));
        // Creating a virtual teacher named "All" that will be used to select all teachers
        store = topicButtonSelector.getStore();
        Topic allTopic = store.createEntity(Topic.class);
        topicButtonSelector.setVisualNullEntity(allTopic);
        // Binding teacher name with "all" i18n key
        StringProperty allTopicNameProperty; // keeping a reference to prevent GC
        I18n.bindI18nTextProperty(allTopicNameProperty = new SimpleStringProperty() {
            @Override
            protected void invalidated() {
                allTopic.setName(get()); // Updating teacher name
                topicButtonSelector.updateButtonContentFromSelectedItem(); // Refreshing button content in case it was the selected teacher
            }
        }, "all");

        Button topicButton = topicButtonSelector.getButton();
        topicButton.getProperties().put("dontGarbage", allTopicNameProperty);
        topicButton.setMinWidth(Region.USE_PREF_SIZE);
        topicButton.setMaxWidth(Region.USE_PREF_SIZE);
        ScalePane scaledTopicButton = new ScalePane(ScaleMode.FIT_WIDTH, topicButton);
        scaledTopicButton.setCanShrink(false);

        topicProperty.bindBidirectional(topicButtonSelector.selectedItemProperty());

        // Setting a max width for big desktop screens
        pageContainer.setMaxWidth(1200); // Similar value as our website
        pageContainer.setAlignment(Pos.CENTER);
        BorderPane.setMargin(pageContainer, new Insets(0, 20, 0, 20)); // Global page padding
        VBox.setMargin(podcastsLabel, new Insets(20, 0, 20, 0));
        VBox.setMargin(podcastsChannelsPane, new Insets(20, 0, 20, 0));
        VBox.setMargin(separatorLine, new Insets(10, 0, 40, 0));
        VBox.setMargin(podcastsContainer, new Insets(40, 0, 10, 0));

        FlexPane buttonsFlexPane = new FlexPane(scaledTeacherButton, scaledTopicButton);
        buttonsFlexPane.setHorizontalSpace(10);
        buttonsFlexPane.setVerticalSpace(10);
        buttonsFlexPane.setDistributeRemainingRowSpace(true);

        pageContainer.getChildren().setAll(
                podcastsLabel,
                alsoAvailableOnLabel,
                podcastsChannelsPane,
                separatorLine,
                buttonsFlexPane,
                podcastsContainer
        );

        FXProperties.runOnPropertiesChange(() -> {
            double width = pageContainer.getWidth();
            GeneralUtility.screenChangeListened(width);
            // Setting the teacher button max scale proportionally to the width but always between 1 & 2.5
            double scale = Math.max(1, Math.min(width / 600, 2.5));
            scaledTeacherButton.setMaxScale(scale);
            scaledTopicButton.setMaxScale(scale);
            // Also the space above and below
            VBox.setMargin(separatorLine, new Insets(10, 0, 40 * scale, 0));
            VBox.setMargin(podcastsContainer, new Insets(40 * scale, 0, 10, 0));
        }, pageContainer.widthProperty());

        // Embedding the page in a ScrollPane. The page itself is embedded in a BorderPane in order to keep the page
        // centered when it reaches its max width (without the BorderPane, the ScrollPane would position it on left).
        ScrollPane scrollPane = ControlUtil.createVerticalScrollPane(new BorderPane(pageContainer));

        // Automatically increasing the podcast list when the user scroll down up to bottom
        scrollPane.vvalueProperty().addListener((observable, oldValue, vValue) -> {
            int currentLimit = podcastsLimitProperty.get();
            if (vValue.doubleValue() >= scrollPane.getVmax() * 0.999 && podcastsContainer.getChildren().size() == currentLimit)
                podcastsLimitProperty.set(currentLimit + 5);
        });

        return scrollPane;
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
        return button;
    }

    private static Tab createTopicTab(TabsBar<Boolean> tabsBar, String text, boolean virtuous) {
        Tab tab = tabsBar.createTab(text, virtuous);
        tab.setPadding(new Insets(5));
        tab.setTextFill(Color.GRAY);
        return tab;
    }

    @Override
    protected void startLogic() {
        ReactiveObjectsMapper.<Podcast, Node>createPushReactiveChain(this)
                .always("{class: 'Podcast', fields: 'channel, channelPodcastId, date, title, excerpt, imageUrl, audioUrl, durationMillis', where: 'durationMillis != null', orderBy: 'date desc, id desc'}")
                .always(podcastsLimitProperty, limit -> DqlStatement.limit("?", limit))
                .ifNotNull(teacherProperty, teacher -> teacher == FAVORITE_TAB_VIRTUAL_TEACHER ? DqlStatement.whereFieldIn("id", FXFavoritePodcasts.getFavoritePodcastIds().toArray()) : DqlStatement.where("teacher = ?", teacher))
                .ifNotNull(topicProperty, topic -> { String searchLike = "%" + topic.getName().toLowerCase() + "%"; return DqlStatement.where("lower(title) like ? or lower(excerpt) like ?", searchLike, searchLike); })
                .setIndividualEntityToObjectMapperFactory(IndividualEntityToObjectMapper.createFactory(PodcastView::new, PodcastView::setPodcast, PodcastView::getView))
                .storeMappedObjectsInto(podcastsContainer.getChildren())
                .start();
    }

}
