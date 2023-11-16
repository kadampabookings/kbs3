package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.extras.imagestore.ImageStore;
import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.util.control.ControlUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.async.Handler;
import dev.webfx.platform.conf.SourcesConfig;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.orm.dql.DqlStatement;
import dev.webfx.stack.orm.reactive.entities.entities_to_objects.IndividualEntityToObjectMapper;
import dev.webfx.stack.orm.reactive.entities.entities_to_objects.ReactiveObjectsMapper;
import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import one.modality.base.frontoffice.utility.GeneralUtility;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.shared.entities.News;
import org.kadampabookings.kbs.frontoffice.activities.news.views.NewsView;

public final class NewsActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin {

    private final BorderPane homeContainer = new BorderPane();
    private final VBox pageContainer = new VBox(); // The main container inside the vertical scrollbar
    private final VBox newsContainer = new VBox(40);
    public final IntegerProperty newsLimitProperty = new SimpleIntegerProperty(5);

    @Override
    public Node buildUi() {
        Text headerText = I18n.bindI18nProperties(new Text(), "frontOfficeHomeHeaderText");
        headerText.setFill(Color.web(StyleUtility.MAIN_BLUE));
        headerText.setFont(Font.font(StyleUtility.TEXT_FAMILY, FontWeight.BOLD, 32));
        headerText.setStyle("-fx-font-family: " + StyleUtility.TEXT_FAMILY + "; -fx-font-weight: bold; -fx-font-size: 32");
        headerText.setWrappingWidth(210);
        String headerImageUrl = SourcesConfig.getSourcesRootConfig().childConfigAt("kbs.frontoffice.activity.news").getString("headerImageUrl");
        ImageView headerImageView = ImageStore.createImageView(headerImageUrl);
        headerImageView.setPreserveRatio(true);
        headerImageView.setFitHeight(600);
        StackPane headerPane = new StackPane(headerImageView, headerText);
        StackPane.setAlignment(headerText, Pos.TOP_LEFT);
        StackPane.setAlignment(headerImageView, Pos.CENTER_RIGHT);
        StackPane.setMargin(headerText, new Insets(150, 0, 0, 0));
        headerPane.setMaxHeight(600);

        ScalePane scalePane = new ScalePane(headerPane);
        scalePane.setBackground(Background.fill(Color.WHITE));

        pageContainer.setAlignment(Pos.CENTER);
        Insets containerMargins = new Insets(30, 20, 10, 20);
        VBox.setMargin(newsContainer, containerMargins);
        pageContainer.getChildren().setAll(
                scalePane,
                newsContainer
        );

        FXProperties.runOnPropertiesChange(() -> {
            double width = pageContainer.getWidth();
            double maxHeight = width < 600 ? width : 600;
            scalePane.setMaxHeight(maxHeight);
            headerText.setTranslateX(Math.max(0, (width - 600) * 0.5));
            GeneralUtility.screenChangeListened(width);
        }, pageContainer.widthProperty());

        // Setting a max width for big desktop screens
        pageContainer.setMaxWidth(1200); // Similar value as our website

        // Embedding the page in a ScrollPane. The page itself is embedded in a BorderPane in order to keep the page
        // centered when it reaches its max width (without the BorderPane, the ScrollPane would position it on left).
        ScrollPane scrollPane = ControlUtil.createVerticalScrollPane(new BorderPane(pageContainer));

        scrollPane.vvalueProperty().addListener((observable, oldValue, vValue) -> {
            if (vValue.doubleValue() >= scrollPane.getVmax() * 0.999)
                newsLimitProperty.set(newsLimitProperty.get() + 5);
        });

        homeContainer.setCenter(scrollPane);

        getUiRouter().getRouter().route().handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext ctx) {
                homeContainer.setCenter(scrollPane);
                ctx.next();
            }
        });

        return WEB_VIEW_CONTAINER = homeContainer;
    }

    @Override
    protected void startLogic() {

        ReactiveObjectsMapper.<News, Node>createPushReactiveChain(this)
                .always("{class: 'News', fields: 'channel, channelNewsId, date, title, excerpt, imageUrl, linkUrl', orderBy: 'date desc, id desc'}")
                .always(newsLimitProperty, limit -> DqlStatement.limit("?", limit))
                .setIndividualEntityToObjectMapperFactory(IndividualEntityToObjectMapper.createFactory(org.kadampabookings.kbs.frontoffice.activities.news.views.NewsView::new, org.kadampabookings.kbs.frontoffice.activities.news.views.NewsView::setNews, NewsView::getView))
                .storeMappedObjectsInto(newsContainer.getChildren())
                .start();
    }

    private static BorderPane WEB_VIEW_CONTAINER;
    private static WebView WEB_VIEW;
    private static String URL;

    public static void browse(String url) {
        if (WEB_VIEW == null)
            WEB_VIEW = new WebView();
        if (!url.equals(URL)) {
            WEB_VIEW.getEngine().load(URL = url);
        }
        WEB_VIEW_CONTAINER.setCenter(WEB_VIEW);
    }
}
