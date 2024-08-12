package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.extras.carousel.Carousel;
import dev.webfx.extras.imagestore.ImageStore;
import dev.webfx.extras.panes.FlexPane;
import dev.webfx.extras.panes.ScaleMode;
import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.switches.Switch;
import dev.webfx.extras.util.control.ControlUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.conf.SourcesConfig;
import dev.webfx.stack.cache.client.LocalStorageCache;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.orm.dql.DqlStatement;
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
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import one.modality.base.client.activity.ModalityButtonFactoryMixin;
import one.modality.base.client.mainframe.dialogarea.fx.FXMainFrameDialogArea;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.frontoffice.utility.TextUtility;
import one.modality.base.shared.entities.News;
import one.modality.base.shared.entities.Topic;
import one.modality.base.shared.entities.Video;
import org.kadampabookings.kbs.frontoffice.mediaview.VideoView;

public final class NewsActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin, ModalityButtonFactoryMixin {

    private static final double MAX_PAGE_WIDTH = 1200; // Similar value to website
    private static final int INITIAL_LIMIT = 5;
    private static final String SEARCH_ICON_SVG_PATH = "m 15.559797,15.559797 c -0.586939,0.586937 -1.538283,0.586937 -2.125253,0 l -2.65194,-2.651972 C 9.692322,13.607263 8.4035396,14.023982 7.0120069,14.023982 3.1396594,14.023982 0,10.884761 0,7.0119756 0,3.1391906 3.1396594,0 7.0120069,0 c 3.8728471,0 7.0120071,3.139128 7.0120071,7.0119756 0,1.391064 -0.417251,2.6803464 -1.116189,3.7711284 l 2.651972,2.651972 c 0.586937,0.586938 0.586937,1.537782 0,2.124721 z M 7.0120069,2.0034082 c -2.7659715,0 -5.0085674,2.242096 -5.0085674,5.0085362 0,2.7664401 2.2426272,5.0085676 5.0085674,5.0085676 2.766409,0 5.0085361,-2.2421275 5.0085361,-5.0085676 0,-2.7664402 -2.2421271,-5.0085362 -5.0085361,-5.0085362 z";

    private final VBox pageContainer = new VBox(); // The main container inside the vertical scrollbar
    private final VBox newsContainer = new VBox(40);
    private final VBox videosContainer = new VBox(20);
    private final Carousel carousel = new Carousel(newsContainer, videosContainer);
    public final IntegerProperty newsLimitProperty = new SimpleIntegerProperty(INITIAL_LIMIT);
    private final ObjectProperty<Topic> topicProperty = new SimpleObjectProperty<>();
    private final Label videosLabel = I18nControls.bindI18nProperties(new Label(), "Videos");
    private final Switch videosSwitch = new Switch();
    private final TextField searchTextField = new TextField();
    private final SVGPath searchIconSvgPath = new SVGPath();
    private final HBox searchBar = new HBox(searchTextField, searchIconSvgPath);

    @Override
    public Node buildUi() {
        Text headerText = TextUtility.createText("newsHeaderText", StyleUtility.MAIN_ORANGE_COLOR);
        TextUtility.setTextFont(headerText, StyleUtility.TEXT_FAMILY, FontWeight.BOLD, 32);
        headerText.setWrappingWidth(250);

        String headerImageUrl = SourcesConfig.getSourcesRootConfig().childConfigAt("kbs.frontoffice.activity.news").getString("headerImageUrl");
        ImageView headerImageView = ImageStore.createImageView(headerImageUrl);
        headerImageView.setPreserveRatio(true);
        headerImageView.setFitHeight(600);

        StackPane headerPane = new StackPane(headerImageView, headerText);
        StackPane.setAlignment(headerText, Pos.TOP_LEFT);
        StackPane.setAlignment(headerImageView, Pos.CENTER_RIGHT);
        StackPane.setMargin(headerText, new Insets(150, 0, 0, 0));
        headerPane.setMaxHeight(600);

        ScalePane headerScalePane = new ScalePane(headerPane);
        headerScalePane.setBackground(Background.fill(Color.WHITE));

        Text topicPrefixText = TextUtility.createText("topic", Color.GRAY);
        EntityButtonSelector<Topic> topicButtonSelector = new EntityButtonSelector<Topic>(
                "{class: 'Topic', alias: 't', columns: 'name', where: 'exists(select News where topic = t)', orderBy: 'id'}",
                this, FXMainFrameDialogArea::getDialogArea, getDataSourceModel()
        ) { // Overriding the button content to add the "Teacher" prefix text
            @Override
            protected Node getOrCreateButtonContentFromSelectedItem() {
                return new HBox(10, topicPrefixText, super.getOrCreateButtonContentFromSelectedItem());
            }
        }.appendNullEntity(true); // Also adding null entity that will represent all teachers
        // Creating a virtual teacher named "All" that will be used to select all teachers
        EntityStore store = topicButtonSelector.getStore();
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

        topicProperty.bind(topicButtonSelector.selectedItemProperty());

        searchBar.getStyleClass().setAll("searchbar");
        searchBar.setAlignment(Pos.CENTER);
        searchBar.setMinWidth(200);
        searchBar.setMaxWidth(500);
        searchBar.setMaxHeight(40);
        searchTextField.setPromptText("Search");
        searchIconSvgPath.setContent(SEARCH_ICON_SVG_PATH);
        searchIconSvgPath.setFill(Color.GRAY);
        HBox.setHgrow(searchTextField, Priority.ALWAYS);
        HBox.setMargin(searchTextField, new Insets(0, 0, 0, 10));
        HBox.setMargin(searchIconSvgPath, new Insets(0,12,0,0));
        VBox.setMargin(searchBar, new Insets(10, 20, 10, 20));

        HBox switchBox = new HBox(5, videosLabel, videosSwitch);
        switchBox.setMinWidth(Region.USE_PREF_SIZE);
        switchBox.setMaxWidth(Region.USE_PREF_SIZE);
        switchBox.setAlignment(Pos.CENTER);
        ScalePane scaledSwitchBox = new ScalePane(ScaleMode.FIT_HEIGHT, switchBox);
        scaledSwitchBox.setCanShrink(false);
        FlexPane filterBar = new FlexPane(searchBar, scaledTopicButton, scaledSwitchBox);
        filterBar.setHorizontalSpace(10);
        filterBar.setVerticalSpace(10);
        filterBar.setAlignment(Pos.CENTER);
        filterBar.setDistributeRemainingRowSpace(true);
        VBox.setMargin(filterBar, new Insets(20));

        pageContainer.setAlignment(Pos.CENTER);
        VBox.setMargin(carousel.getContainer(), new Insets(30, 20, 10, 20));
        carousel.setShowingDots(false);

        pageContainer.getChildren().setAll(
                headerScalePane,
                filterBar,
                carousel.getContainer()
        );

        FXProperties.runOnPropertiesChange(() -> {
            double width = pageContainer.getWidth();
            double maxHeight = width < 600 ? width : 600;
            headerScalePane.setMaxHeight(maxHeight);
            headerText.setTranslateX(Math.max(20, (width - 600) * 0.5));
            // Setting the teacher button max scale proportionally to the width but always between 1 & 2.5
            double scale = Math.max(1, Math.min(width / 600, 1.75));
            scaledTopicButton.setMaxScale(scale);
            scaledSwitchBox.setMaxScale(scale);
        }, pageContainer.widthProperty());

        // Setting a max width for big desktop screens
        pageContainer.setMaxWidth(MAX_PAGE_WIDTH);

        // Embedding the page in a ScrollPane. The page itself is embedded in a BorderPane in order to keep the page
        // centered when it reaches its max width (without the BorderPane, the ScrollPane would position it on left).
        ScrollPane scrollPane = ControlUtil.createVerticalScrollPane(new BorderPane(pageContainer));

        scrollPane.vvalueProperty().addListener((observable, oldValue, vValue) -> {
            int currentLimit = newsLimitProperty.get();
            if (vValue.doubleValue() >= scrollPane.getVmax() * 0.999 && newsContainer.getChildren().size() == currentLimit)
                newsLimitProperty.set(currentLimit + 5);
        });

        scrollPane.getStyleClass().add("news-activity");
        return scrollPane;
    }

    @Override
    protected void startLogic() {
        // Resetting news limit to initial value whenever the user plays with filters
        FXProperties.runOnPropertiesChange(() -> {
            newsLimitProperty.set(INITIAL_LIMIT);
            carousel.displaySlide(videosSwitch.isSelected() ? videosContainer : newsContainer);
        }, searchTextField.textProperty(), topicProperty, videosSwitch.selectedProperty());

        ReactiveObjectsMapper.<News, Node>createPushReactiveChain(this)
                .always("{class: 'News', fields: 'channel, channelNewsId, date, title, excerpt, imageUrl, linkUrl', orderBy: 'date desc, id desc'}")
                .always(I18n.languageProperty(), lang -> DqlStatement.where("lang = ?", lang))
                .always(newsLimitProperty, limit -> DqlStatement.limit("?", limit))
                .ifTrimNotEmpty(searchTextField.textProperty(), searchText -> { String searchLike = "%" + searchText.toLowerCase() + "%"; return DqlStatement.where("lower(title) like ? or lower(excerpt) like ?", searchLike, searchLike); })
                .ifNotNull(topicProperty, topic -> DqlStatement.where("topic=?", topic))
                //.ifTrue(videosSwitch.selectedProperty(), DqlStatement.where("withVideos"))
                .setIndividualEntityToObjectMapperFactory(IndividualEntityToObjectMapper.createFactory(() -> new NewsView(getHistory()), NewsView::setNews, NewsView::getView))
                .storeMappedObjectsInto(newsContainer.getChildren())
                .setResultCacheEntry(LocalStorageCache.get().getCacheEntry("cache-news"))
                .start();

        ReactiveObjectsMapper.<Video, Node>createPushReactiveChain(this)
                .always("{class: 'Video', fields: 'date, title, excerpt, imageUrl, wistiaVideoId, durationMillis', orderBy: 'date desc, id desc'}")
                .always(I18n.languageProperty(), lang -> DqlStatement.where("lang = ?", lang))
                .always(newsLimitProperty, limit -> DqlStatement.limit("?", limit))
                .ifTrimNotEmpty(searchTextField.textProperty(), searchText -> { String searchLike = "%" + searchText.toLowerCase() + "%"; return DqlStatement.where("lower(title) like ? or lower(excerpt) like ?", searchLike, searchLike); })
                .always(DqlStatement.where("teacher==null"))
                .ifNotNull(topicProperty, topic -> { String searchLike = "%" + topic.getName().toLowerCase() + "%"; return DqlStatement.where("lower(title) like ? or lower(excerpt) like ?", searchLike, searchLike); })
                .setIndividualEntityToObjectMapperFactory(IndividualEntityToObjectMapper.createFactory(VideoView::new, VideoView::setMediaInfo, VideoView::getView))
                .storeMappedObjectsInto(videosContainer.getChildren())
                .setResultCacheEntry(LocalStorageCache.get().getCacheEntry("cache-news-videos"))
                .start();
    }
}
