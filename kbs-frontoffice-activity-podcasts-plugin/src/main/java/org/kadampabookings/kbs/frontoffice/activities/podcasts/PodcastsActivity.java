package org.kadampabookings.kbs.frontoffice.activities.podcasts;

import dev.webfx.extras.carousel.Carousel;
import dev.webfx.extras.panes.*;
import dev.webfx.extras.switches.Switch;
import dev.webfx.extras.util.animation.Animations;
import dev.webfx.extras.util.control.ControlUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.browser.Browser;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.orm.dql.DqlStatement;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.controls.entity.selector.EntityButtonSelector;
import dev.webfx.stack.orm.reactive.entities.dql_to_entities.ReactiveEntitiesMapper;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
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
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import one.modality.base.client.activity.ModalityButtonFactoryMixin;
import one.modality.base.client.mainframe.fx.FXMainFrameDialogArea;
import one.modality.base.client.tile.Tab;
import one.modality.base.client.tile.TabsBar;
import one.modality.base.frontoffice.mainframe.fx.FXCollapseFooter;
import one.modality.base.frontoffice.utility.GeneralUtility;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.shared.entities.Podcast;
import one.modality.base.shared.entities.Teacher;
import one.modality.base.shared.entities.Topic;
import one.modality.base.shared.entities.Video;
import one.modality.base.shared.entities.impl.TeacherImpl;
import org.kadampabookings.kbs.frontoffice.mediaview.VideoView;

import java.time.LocalDate;

public final class PodcastsActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin, ModalityButtonFactoryMixin {

    private static final double MAX_PAGE_WIDTH = 1200; // Similar value to website
    private static final int INITIAL_LIMIT = 5;
    private static final Teacher FAVORITE_TAB_VIRTUAL_TEACHER = new TeacherImpl(EntityId.create(Teacher.class), null);

    private final VBox pageContainer = new VBox(); // The main container inside the vertical scrollbar
    private final VBox podcastsContainer = new VBox(20);
    private final ObservableList<Podcast> podcastsFeed = FXCollections.observableArrayList();
    private Podcast lastLoadedPodcast;
    private final ObjectProperty<Podcast> lastLoadedPodcastProperty = new SimpleObjectProperty<>();
    private final VBox videosContainer = new VBox(20);
    private final ObservableList<Video> videosFeed = FXCollections.observableArrayList();
    private Video lastLoadedVideo;
    private final ObjectProperty<Video> lastLoadedVideoProperty = new SimpleObjectProperty<>();
    private final Carousel carousel = new Carousel(podcastsContainer, videosContainer);
    private final Label videosLabel = I18nControls.bindI18nProperties(new Label(), "videos");
    private final Switch videosSwitch = new Switch();

    private final ObjectProperty<Teacher> teacherProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Topic> topicProperty = new SimpleObjectProperty<>();
    private final BooleanProperty virtuousTopicProperty = new SimpleBooleanProperty();
    private Timeline backgroundTimeline;
    private double lastTopOffset;
    private boolean opaqueBlack;

    @Override
    public Node buildUi() {
        Label podcastsLabel = GeneralUtility.createLabel("podcastsLabel", StyleUtility.MAIN_ORANGE_COLOR);
        podcastsLabel.setContentDisplay(ContentDisplay.TOP);
        podcastsLabel.setTextAlignment(TextAlignment.CENTER);

        Label alsoAvailableOnLabel = GeneralUtility.createLabel("alsoAvailableOn", StyleUtility.MAIN_ORANGE_COLOR);
        Label[] podcastsChannelButtons = {
            createPodcastsChannelButton("Spotify", "https://open.spotify.com/show/5QPCFEyZz74nOHZbQr1B4z"),
            createPodcastsChannelButton("ApplePodcasts", "https://podcasts.apple.com/us/podcast/living-clarity/id1719104184"),
            createPodcastsChannelButton("AmazonMusic", "https://music.amazon.co.uk/podcasts/d64fa9da-7c91-4ee8-84e7-f05de30fdb2c/living-clarity"),
            createPodcastsChannelButton("PocketCasts", "https://pca.st/9yuq0l0p")
        };
        FlexPane podcastsChannelsPane = new FlexPane(podcastsChannelButtons);
        podcastsChannelsPane.setMaxWidth(Double.MAX_VALUE);
        podcastsChannelsPane.setHorizontalSpace(10);
        podcastsChannelsPane.setVerticalSpace(10);
        podcastsChannelsPane.setDistributeRemainingRowSpace(true);

        Region lineSeparator = GeneralUtility.createOrangeLineSeparator();

        HBox switchBox = new HBox(5, videosLabel, videosSwitch);
        switchBox.setMinWidth(Region.USE_PREF_SIZE);
        switchBox.setMaxWidth(Region.USE_PREF_SIZE);
        switchBox.setAlignment(Pos.CENTER);
        ScalePane scaledSwitchBox = new ScalePane(ScaleMode.FIT_HEIGHT, switchBox);
        scaledSwitchBox.setCanShrink(false);

        Text teacherPrefixText = I18n.bindI18nProperties(new Text(), "teacher");
        teacherPrefixText.setFill(Color.GRAY);
        EntityButtonSelector<Teacher> teacherButtonSelector = new EntityButtonSelector<Teacher>(
            "{class: 'Teacher', alias: 't', columns: 'name', orderBy: 'id'}",
            this, FXMainFrameDialogArea::getDialogArea, getDataSourceModel()
        ) { // Overriding the button content to add the "Teacher" prefix text
            @Override
            protected Node getOrCreateButtonContentFromSelectedItem() {
                return new HBox(10, teacherPrefixText, super.getOrCreateButtonContentFromSelectedItem());
            }
        }
            .ifFalse(videosSwitch.selectedProperty(), DqlStatement.where("(select count(1) from Podcast where teacher = t) > 0"))
            .ifTrue(videosSwitch.selectedProperty(), DqlStatement.where("(select count(1) from Video where teacher = t) > 0"))
            .appendNullEntity(true); // Also adding null entity that will represent all teachers
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
        pageContainer.setMaxWidth(MAX_PAGE_WIDTH); // Similar value as our website
        pageContainer.setAlignment(Pos.CENTER);
        BorderPane.setMargin(pageContainer, new Insets(0, 20, 0, 20)); // Global page padding
        VBox.setMargin(podcastsLabel, new Insets(20, 0, 20, 0));
        VBox.setMargin(podcastsChannelsPane, new Insets(20, 0, 20, 0));
        VBox.setMargin(lineSeparator, new Insets(10, 0, 40, 0));
        VBox.setMargin(carousel.getContainer(), new Insets(40, 0, 10, 0));

        FlexPane filterBar = new FlexPane(scaledTeacherButton, /*scaledTopicButton,*/ scaledSwitchBox);
        filterBar.setHorizontalSpace(10);
        filterBar.setVerticalSpace(10);
        filterBar.setAlignment(Pos.CENTER);
        filterBar.setDistributeRemainingRowSpace(true);
        VBox.setMargin(filterBar, new Insets(20));

        carousel.setShowingDots(false);

        pageContainer.getChildren().setAll(
            podcastsLabel,
            alsoAvailableOnLabel,
            podcastsChannelsPane,
            lineSeparator,
            filterBar,
            carousel.getContainer()
        );

        FXProperties.runOnPropertiesChange(() -> {
            double width = pageContainer.getWidth();
            double fontFactor = GeneralUtility.computeFontFactor(width);
            GeneralUtility.setLabeledFont(podcastsLabel, StyleUtility.TEXT_FAMILY, FontWeight.BOLD, fontFactor * 21);
            GeneralUtility.setLabeledFont(alsoAvailableOnLabel, StyleUtility.TEXT_FAMILY, FontWeight.NORMAL, fontFactor * 8);
            for (Label button : podcastsChannelButtons)
                GeneralUtility.setLabeledFont(button, StyleUtility.TEXT_FAMILY, FontWeight.SEMI_BOLD, fontFactor * StyleUtility.MAIN_TEXT_SIZE);
            // Setting the teacher button max scale proportionally to the width but always between 1 & 2.5
            double scale = Math.max(1, Math.min(width / 600, 2.5));
            scaledTeacherButton.setMaxScale(scale);
            scaledTopicButton.setMaxScale(scale);
            scaledSwitchBox.setMaxScale(scale);
            // Also the space above and below
            VBox.setMargin(lineSeparator, new Insets(10, 0, 40 * scale, 0));
            VBox.setMargin(podcastsContainer, new Insets(40 * scale, 0, 10, 0));
        }, pageContainer.widthProperty());

        pageContainer.setOnSwipeLeft(e -> videosSwitch.setSelected(true));  // finger right to left = videos request (as videos are on the right)
        pageContainer.setOnSwipeRight(e -> videosSwitch.setSelected(false)); // finger left to right = podcasts request (as podcasts are on the left)

        // Embedding the page in a ScrollPane. The page itself is embedded in a BorderPane in order to keep the page
        // centered when it reaches its max width (without the BorderPane, the ScrollPane would position it on left).
        BorderPane borderPane = new BorderPane(pageContainer);
        // Also a background is necessary for devices not supporting inverse clipping used in circle animation
        borderPane.setBackground(Background.fill(Color.WHITE));
        ScrollPane scrollPane = ControlUtil.createVerticalScrollPane(borderPane);

        podcastsFeed.addListener((InvalidationListener) observable -> {
            lastLoadedPodcast = Collections.last(podcastsFeed);
            if (lastLoadedPodcast != null) {
                if (lastLoadedPodcastProperty.get() == null)
                    podcastsContainer.getChildren().clear();
                podcastsContainer.getChildren().addAll(Collections.map(podcastsFeed, news -> {
                    PodcastView newsView = new PodcastView();
                    newsView.setMediaInfo(news);
                    return newsView.getView();
                }));
                podcastsFeed.clear();
            }
        });

        videosFeed.addListener((InvalidationListener) observable -> {
            lastLoadedVideo = Collections.last(videosFeed);
            if (lastLoadedVideo != null) {
                if (lastLoadedVideoProperty.get() == null)
                    videosContainer.getChildren().clear();
                videosContainer.getChildren().addAll(Collections.map(videosFeed, video -> {
                    VideoView videoView = new VideoView();
                    videoView.setMediaInfo(video);
                    return videoView.getView();
                }));
                videosFeed.clear();
            }
        });

        ObjectProperty<Color> backgroundColorProperty = new SimpleObjectProperty<>() {
            @Override
            protected void invalidated() {
                borderPane.setBackground(Background.fill(get()));
            }
        };

        // Lazy loading when the user scrolls down
        double lazyLoadingBottomSpace = Screen.getPrimary().getVisualBounds().getHeight();
        pageContainer.setPadding(new Insets(0, 0, lazyLoadingBottomSpace, 0));
        FXProperties.runOnPropertiesChange(() -> {
            double topOffset = ControlUtil.computeScrollPaneVTopOffset(scrollPane);
            boolean scrollDown = topOffset > lastTopOffset; lastTopOffset = topOffset;
            boolean newBlackOpaque = videosSwitch.isSelected() && topOffset > filterBar.getLayoutY() + (scrollDown ? filterBar.getHeight() : 0);
            if (newBlackOpaque != opaqueBlack) {
                opaqueBlack = newBlackOpaque;
                if (backgroundTimeline != null)
                    backgroundTimeline.stop();
                backgroundTimeline = Animations.animateProperty(backgroundColorProperty, newBlackOpaque ? Color.BLACK : new Color(0, 0, 0,0));
                FXCollapseFooter.setCollapseFooter(opaqueBlack);
            }
            double bottomOffset = topOffset + scrollPane.getViewportBounds().getHeight();
            if (bottomOffset > pageContainer.getHeight() - lazyLoadingBottomSpace) {
                if (videosSwitch.isSelected()) {
                    if (lastLoadedVideo != null && videosFeed.isEmpty())
                        FXProperties.setIfNotEquals(lastLoadedVideoProperty, lastLoadedVideo);
                } else {
                    if (lastLoadedPodcast != null && podcastsFeed.isEmpty())
                        FXProperties.setIfNotEquals(lastLoadedPodcastProperty, lastLoadedPodcast);
                }
            }
        }, scrollPane.vvalueProperty()/*, pageContainer.heightProperty()*/);

        scrollPane.getStyleClass().add("podcasts-activity"); // for CSS styling
        // Ensuring to not keep this activity in the scene graph after transition in order to stop the video players
        // in the browser (in case TransitionPane keepsLeavingNode is enabled)
        TransitionPane.setKeepsLeavingNode(scrollPane, false);
        return scrollPane;
    }

    private Label createPodcastsChannelButton(String i18nKey, String url) {
        Label button = GeneralUtility.createLabel(i18nKey, Color.BLACK);
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
        // Resetting podcasts limit to initial value whenever the user plays with filters
        FXProperties.runOnPropertiesChange(() -> {
            lastLoadedVideo = null;
            lastLoadedPodcastProperty.set(null);
            lastLoadedVideo = null;
            lastLoadedVideoProperty.set(null);
            carousel.displaySlide(videosSwitch.isSelected() ? videosContainer : podcastsContainer);
        }, teacherProperty, topicProperty, videosSwitch.selectedProperty());

        // Podcasts loader
        ReactiveEntitiesMapper.<Podcast>createReactiveChain(this)
            .always("{class: 'Podcast', fields: 'channel, channelPodcastId, date, title, excerpt, imageUrl, audioUrl, wistiaVideoId, durationMillis', orderBy: 'date desc, id desc'}")
            .bindActivePropertyTo(videosSwitch.selectedProperty().not().and(activeProperty()))
            //.always(I18n.languageProperty(), lang -> DqlStatement.where("lang = ?", lang))
            .always(DqlStatement.limit("?", INITIAL_LIMIT))
            .ifNotNull(teacherProperty, teacher -> teacher == FAVORITE_TAB_VIRTUAL_TEACHER ? DqlStatement.whereFieldIn("id", FXFavoritePodcasts.getFavoritePodcastIds().toArray()) : DqlStatement.where("teacher = ?", teacher))
            .ifNotNull(topicProperty, topic -> {
                String searchLike = "%" + topic.getName().toLowerCase() + "%";
                return DqlStatement.where("lower(title) like ? or lower(excerpt) like ?", searchLike, searchLike);
            })
            .always(DqlStatement.where("audioUrl != null"))
            .ifNotNull(lastLoadedPodcastProperty, podcast -> DqlStatement.where("date < ?", podcast.getDate()))
            .storeEntitiesInto(podcastsFeed)
            //.setResultCacheEntry(LocalStorageCache.get().getCacheEntry("cache-podcasts"))
            .start();

        // Videos loader
        ReactiveEntitiesMapper.<Video>createReactiveChain(this)
            // Note: note loading the excerpt causes the video to be displayed in wide width mode
            .always("{class: 'Video', fields: 'date, title, imageUrl, wistiaVideoId, youtubeVideoId, durationMillis, width, height, ord', orderBy: 'ord nulls last, date desc'}")
            .bindActivePropertyTo(videosSwitch.selectedProperty().and(activeProperty()))
            .always(I18n.languageProperty(), lang -> DqlStatement.where("lang = ?", lang))
            .always(DqlStatement.limit("?", INITIAL_LIMIT))
            .always(DqlStatement.where("playlist=1 or teacher != null"))
            .ifNotNull(teacherProperty, teacher -> teacher == FAVORITE_TAB_VIRTUAL_TEACHER ? DqlStatement.whereFieldIn("id", FXFavoritePodcasts.getFavoritePodcastIds().toArray()) : DqlStatement.where("teacher = ?", teacher))
            .ifNotNull(topicProperty, topic -> {
                String searchLike = "%" + topic.getName().toLowerCase() + "%";
                return DqlStatement.where("lower(title) like ? or lower(excerpt) like ?", searchLike, searchLike);
            })
            .ifNotNull(lastLoadedVideoProperty, video -> DqlStatement.where("ord!=null and ord > ? or ord=null and date < ?", video.getOrd(), video.getOrd() == null ? video.getDate() : LocalDate.now()))
            .storeEntitiesInto(videosFeed)
            //.setResultCacheEntry(LocalStorageCache.get().getCacheEntry("cache-podcasts-videos"))
            .start();
    }

}
