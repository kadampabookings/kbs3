package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.extras.imagestore.ImageStore;
import dev.webfx.extras.panes.ColumnsPane;
import dev.webfx.extras.panes.ScaleMode;
import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.util.control.ControlUtil;
import dev.webfx.extras.visual.VisualColumn;
import dev.webfx.extras.visual.VisualResultBuilder;
import dev.webfx.extras.visual.controls.grid.SkinnedVisualGrid;
import dev.webfx.extras.visual.controls.grid.VisualGrid;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.conf.SourcesConfig;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.orm.dql.DqlStatement;
import dev.webfx.stack.orm.entity.controls.entity.selector.ButtonSelector;
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
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import one.modality.base.client.activity.ModalityButtonFactoryMixin;
import one.modality.base.client.mainframe.dialogarea.fx.FXMainFrameDialogArea;
import one.modality.base.frontoffice.utility.GeneralUtility;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.shared.entities.News;

import java.util.stream.IntStream;

public final class NewsActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin, ModalityButtonFactoryMixin {

    private enum NewsTab { ALL, VIDEOS, FAVORITES }
    private final BorderPane homeContainer = new BorderPane();
    private final VBox pageContainer = new VBox(); // The main container inside the vertical scrollbar
    private final VBox newsContainer = new VBox(40);
    private final ObjectProperty<NewsTab> selectedTab = new SimpleObjectProperty<>();
    public final IntegerProperty newsLimitProperty = new SimpleIntegerProperty(5);
    private final TextField searchTextField = new TextField();
    private final SVGPath searchIconSvgPath = new SVGPath();
    private final HBox searchBar = new HBox(searchTextField, searchIconSvgPath);

    @Override
    public Node buildUi() {
        Text headerText = I18n.bindI18nProperties(new Text(), "newsHeaderText");
        headerText.setFill(Color.web(StyleUtility.MAIN_ORANGE));
        headerText.setFont(Font.font(StyleUtility.TEXT_FAMILY, FontWeight.BOLD, 32));
        headerText.setStyle("-fx-font-family: " + StyleUtility.TEXT_FAMILY + "; -fx-font-weight: bold; -fx-font-size: 32");
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

        Text typePrefixText = I18n.bindI18nProperties(new Text(), "Type");
        typePrefixText.setFill(Color.GRAY);
        int length = NewsTab.values().length;
        VisualResultBuilder vrb = VisualResultBuilder.create(length, VisualColumn.create(null, PrimType.STRING));
        IntStream.range(0, length).forEach(i -> vrb.setValue(i, 0, NewsTab.values()[i].name()));
        VisualGrid typeGrid = new SkinnedVisualGrid(vrb.build());
        typeGrid.setHeaderVisible(false);
        ButtonSelector<NewsTab> typeButtonSelector = new ButtonSelector<>(this, FXMainFrameDialogArea::getDialogArea) {

            {
                setSelectedItem(NewsTab.ALL);
                setSearchEnabled(false);
                setShowMode(ShowMode.DROP_DOWN);
                typeGrid.visualSelectionProperty().addListener((observable, oldValue, newValue) -> {
                    setSelectedItem(NewsTab.values()[newValue.getSelectedRow()]);
                    Platform.runLater(this::closeDialog);
                });
            }

            @Override
            protected Node getOrCreateButtonContentFromSelectedItem() {
                return new HBox(10, typePrefixText, new Text(getSelectedItem().name()));
            }

            @Override
            protected Region getOrCreateDialogContent() {
                return typeGrid;
            }

            @Override
            protected void startLoading() {}
        };

        selectedTab.bind(typeButtonSelector.selectedItemProperty());

        Button typeButton = typeButtonSelector.getButton();
        typeButton.setMaxWidth(Region.USE_PREF_SIZE);
        ScalePane scaledTypeButton = new ScalePane(ScaleMode.FIT_WIDTH, typeButton);

        searchBar.getStyleClass().setAll("searchbar");
        searchBar.setAlignment(Pos.CENTER);
        searchBar.setMaxWidth(500);
        searchBar.setMaxHeight(40);
        searchTextField.setPromptText("Search");
        searchIconSvgPath.setContent("m 15.559797,15.559797 c -0.586939,0.586937 -1.538283,0.586937 -2.125253,0 l -2.65194,-2.651972 C 9.692322,13.607263 8.4035396,14.023982 7.0120069,14.023982 3.1396594,14.023982 0,10.884761 0,7.0119756 0,3.1391906 3.1396594,0 7.0120069,0 c 3.8728471,0 7.0120071,3.139128 7.0120071,7.0119756 0,1.391064 -0.417251,2.6803464 -1.116189,3.7711284 l 2.651972,2.651972 c 0.586937,0.586938 0.586937,1.537782 0,2.124721 z M 7.0120069,2.0034082 c -2.7659715,0 -5.0085674,2.242096 -5.0085674,5.0085362 0,2.7664401 2.2426272,5.0085676 5.0085674,5.0085676 2.766409,0 5.0085361,-2.2421275 5.0085361,-5.0085676 0,-2.7664402 -2.2421271,-5.0085362 -5.0085361,-5.0085362 z");
        searchIconSvgPath.setFill(Color.GRAY);
        HBox.setHgrow(searchTextField, Priority.ALWAYS);
        HBox.setMargin(searchTextField, new Insets(0, 0, 0, 10));
        HBox.setMargin(searchIconSvgPath, new Insets(0,14,0,0));
        VBox.setMargin(searchBar, new Insets(10, 20, 10, 20));

        pageContainer.setAlignment(Pos.CENTER);
        VBox.setMargin(newsContainer, new Insets(30, 20, 10, 20));
        ColumnsPane filterBar = new ColumnsPane(scaledTypeButton, searchBar);
        VBox.setMargin(filterBar, new Insets(20));
        pageContainer.getChildren().setAll(
                headerScalePane,
                filterBar,
                newsContainer
        );

        FXProperties.runOnPropertiesChange(() -> {
            double width = pageContainer.getWidth();
            double maxHeight = width < 600 ? width : 600;
            headerScalePane.setMaxHeight(maxHeight);
            headerText.setTranslateX(Math.max(0, (width - 600) * 0.5));
            // Setting the teacher button max scale proportionally to the width but always between 1 & 2.5
            double scale = Math.max(1, Math.min(width / 600, 1.75));
            scaledTypeButton.setMaxScale(scale);
            GeneralUtility.screenChangeListened(width);
        }, pageContainer.widthProperty());

        // Setting a max width for big desktop screens
        pageContainer.setMaxWidth(1200); // Similar value as our website

        // Embedding the page in a ScrollPane. The page itself is embedded in a BorderPane in order to keep the page
        // centered when it reaches its max width (without the BorderPane, the ScrollPane would position it on left).
        ScrollPane scrollPane = ControlUtil.createVerticalScrollPane(new BorderPane(pageContainer));

        scrollPane.vvalueProperty().addListener((observable, oldValue, vValue) -> {
            int currentLimit = newsLimitProperty.get();
            if (vValue.doubleValue() >= scrollPane.getVmax() * 0.999 && newsContainer.getChildren().size() == currentLimit)
                newsLimitProperty.set(currentLimit + 5);
        });

        homeContainer.setCenter(scrollPane);
        return homeContainer;
    }

    @Override
    protected void startLogic() {
        ReactiveObjectsMapper.<News, Node>createPushReactiveChain(this)
                .always("{class: 'News', fields: 'channel, channelNewsId, date, title, excerpt, imageUrl, linkUrl', orderBy: 'date desc, id desc'}")
                .always(I18n.languageProperty(), lang -> DqlStatement.where("lang = ?", lang))
                .always(newsLimitProperty, limit -> DqlStatement.limit("?", limit))
                .ifEquals(selectedTab, NewsTab.VIDEOS, DqlStatement.where("withVideos"))
                .ifEquals(selectedTab, NewsTab.FAVORITES, () -> DqlStatement.whereFieldIn("id", FXFavoriteNews.getFavoriteNewsIds().toArray()))
                .ifTrimNotEmpty(searchTextField.textProperty(), searchText -> { String searchLike = "%" + searchText.toLowerCase() + "%"; return DqlStatement.where("lower(title) like ? or lower(excerpt) like ?", searchLike, searchLike); })
                .setIndividualEntityToObjectMapperFactory(IndividualEntityToObjectMapper.createFactory(() -> new NewsView(getHistory()), NewsView::setNews, NewsView::getView))
                .storeMappedObjectsInto(newsContainer.getChildren())
                .start();
    }
}
