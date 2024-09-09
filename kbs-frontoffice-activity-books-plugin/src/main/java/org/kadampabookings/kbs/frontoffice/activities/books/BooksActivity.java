package org.kadampabookings.kbs.frontoffice.activities.books;

import dev.webfx.extras.carousel.Carousel;
import dev.webfx.extras.panes.TransitionPane;
import dev.webfx.extras.util.control.ControlUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.resource.Resource;
import dev.webfx.stack.cache.client.LocalStorageCache;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.orm.dql.DqlStatement;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.reactive.entities.entities_to_objects.IndividualEntityToObjectMapper;
import dev.webfx.stack.orm.reactive.entities.entities_to_objects.ReactiveObjectsMapper;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import one.modality.base.client.activity.ModalityButtonFactoryMixin;
import one.modality.base.frontoffice.browser.BrowserUtil;
import one.modality.base.frontoffice.mainframe.fx.FXCollapseFooter;
import one.modality.base.shared.entities.Book;
import one.modality.base.shared.entities.Video;
import one.modality.base.shared.entities.impl.VideoImpl;
import org.kadampabookings.kbs.frontoffice.mediaview.Players;
import org.kadampabookings.kbs.frontoffice.mediaview.VideoView;

public final class BooksActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin, ModalityButtonFactoryMixin {

    private static final double MAX_PAGE_WIDTH = 1200; // Similar value to website

    private final VBox pageContainer = new VBox(); // The main container inside the vertical scrollbar
    private final VBox booksContainer = new VBox(20);
    private final VBox videosContainer = new VBox(20);
    private final Carousel carousel = new Carousel(pageContainer, videosContainer);
    private final BooleanProperty showVideosProperty = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            carousel.displaySlide(get() ? videosContainer : pageContainer);
            FXCollapseFooter.setCollapseFooter(get());
        }
    };

    @Override
    public Node buildUi() {

        videosContainer.setBackground(Background.fill(Color.BLACK));

        Video collectedWorksVideo = new VideoImpl(EntityId.create(Video.class), null);
        collectedWorksVideo.setWistiaVideoId("bpoth1bo20");
        collectedWorksVideo.setImageUrl(Resource.toUrl("CollectedWorks.png", getClass()));
        VideoView collectedWorksVideoView = new VideoView();
        collectedWorksVideoView.setDecorated(false);
        collectedWorksVideoView.setWideVideoMaxWidth(MAX_PAGE_WIDTH);
        collectedWorksVideoView.setMediaInfo(collectedWorksVideo);

        // Setting a max width for big desktop screens
        pageContainer.setMaxWidth(MAX_PAGE_WIDTH); // Similar value as our website
        pageContainer.setAlignment(Pos.CENTER);
        pageContainer.setPadding(new Insets(20, 20, 50, 20)); // Global page padding
        VBox.setMargin(carousel.getContainer(), new Insets(40, 0, 10, 0));

        carousel.setShowingDots(false);

        pageContainer.getChildren().setAll(
            collectedWorksVideoView.getView(),
            booksContainer
        );

        FXProperties.runOnPropertiesChange(() -> {
            double width = pageContainer.getWidth();
            // Setting the teacher button max scale proportionally to the width but always between 1 & 2.5
            double scale = Math.max(1, Math.min(width / 600, 2.5));
            // Also the space above and below
            VBox.setMargin(booksContainer, new Insets(40 * scale, 0, 10, 0));
        }, pageContainer.widthProperty());

        // Embedding the page in a ScrollPane. The page itself is embedded in a BorderPane in order to keep the page
        // centered when it reaches its max width (without the BorderPane, the ScrollPane would position it on left).
        BorderPane borderPane = new BorderPane(carousel.getContainer());
        // Also a background is necessary for devices not supporting inverse clipping used in circle animation
        borderPane.setBackground(Background.fill(Color.WHITE));
        ScrollPane scrollPane = ControlUtil.createVerticalScrollPane(borderPane);

        scrollPane.getStyleClass().add("podcasts-activity"); // for CSS styling
        // Ensuring to not keep this activity in the scene graph after transition in order to stop the video players
        // in the browser (in case TransitionPane keepsLeavingNode is enabled)
        TransitionPane.setKeepsLeavingNode(scrollPane, false);
        return scrollPane;
    }

    @Override
    public void onResume() {
        BrowserUtil.setUiRouter(getUiRouter());
        Players.setFullscreenButtonEnabled(false);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Players.setFullscreenButtonEnabled(true);
    }

    private void showVideos() {
        showVideosProperty.set(true);
    }

    @Override
    protected void startLogic() {

        // Podcasts loader
        ReactiveObjectsMapper.<Book, Node>createReactiveChain(this)
            .always("{class: 'Book', fields: 'title, description, imageUrl, freeUrl, orderUrl', orderBy: 'id'}")
            .bindActivePropertyTo(showVideosProperty.not().and(activeProperty()))
            .always(I18n.languageProperty(), lang -> DqlStatement.where("lang = ?", lang))
            .setIndividualEntityToObjectMapperFactory(IndividualEntityToObjectMapper.createFactory(() -> new BookView(this::showVideos), BookView::setBook, BookView::getView))
            .storeMappedObjectsInto(booksContainer.getChildren())
            .setResultCacheEntry(LocalStorageCache.get().getCacheEntry("cache-books"))
            .start();

        // Videos loader
/* Commented due to issue with the active binding (not working and not sure why)
        ReactiveObjectsMapper.<Video, Node>createReactiveChain(this)
            .always("{class: 'Video', fields: 'date, title, imageUrl, wistiaVideoId, youtubeVideoId, durationMillis, width, height, ord', orderBy: 'ord'}")
            .bindActivePropertyTo(showVideosProperty.and(activeProperty()))
            //.always(I18n.languageProperty(), lang -> DqlStatement.where("lang = ?", lang))
            .always(DqlStatement.where("playlist=2")) // Kadampa books playlist
            .setIndividualEntityToObjectMapperFactory(IndividualEntityToObjectMapper.createFactory(VideoView::new, VideoView::setMediaInfo, VideoView::getView))
            .storeMappedObjectsInto(videosContainer.getChildren())
            .setResultCacheEntry(LocalStorageCache.get().getCacheEntry("cache-books-videos"))
            .start();
*/
    }

}
