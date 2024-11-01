package org.kadampabookings.kbs.frontoffice.activities.books;

import dev.webfx.extras.carousel.Carousel;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import one.modality.base.client.activity.ModalityButtonFactoryMixin;
import one.modality.base.frontoffice.mainframe.fx.FXCollapseFooter;
import one.modality.base.frontoffice.utility.activity.FrontOfficeActivityUtil;
import one.modality.base.frontoffice.utility.browser.BrowserUtil;
import one.modality.base.shared.entities.Book;
import one.modality.base.shared.entities.Video;
import one.modality.base.shared.entities.impl.VideoImpl;
import one.modality.event.client.mediaview.Players;
import one.modality.event.client.mediaview.VideoView;

final class BooksActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin, ModalityButtonFactoryMixin {

    private final VBox pageContainer = new VBox(); // The main container inside the vertical scrollbar
    private final VBox booksContainer = new VBox(20);
    private final VBox videosContainer = new VBox(20);
    private final Carousel carousel = new Carousel(pageContainer, videosContainer);
    private final BooleanProperty showVideosProperty = FXProperties.newBooleanProperty(showVideos -> {
        carousel.displaySlide(showVideos ? videosContainer : pageContainer);
        FXCollapseFooter.setCollapseFooter(showVideos);
    });

    @Override
    public Node buildUi() {

        videosContainer.setBackground(Background.fill(Color.BLACK));

        Video collectedWorksVideo = new VideoImpl(EntityId.create(Video.class), null);
        collectedWorksVideo.setWistiaVideoId("bpoth1bo20");
        collectedWorksVideo.setImageUrl(Resource.toUrl("CollectedWorks.png", getClass()));
        VideoView collectedWorksVideoView = new VideoView();
        collectedWorksVideoView.setDecorated(false);
        collectedWorksVideoView.setWideVideoMaxWidth(FrontOfficeActivityUtil.MAX_PAGE_WIDTH);
        collectedWorksVideoView.setMediaInfo(collectedWorksVideo);

        // Setting a max width for big desktop screens
        pageContainer.setAlignment(Pos.CENTER);
        pageContainer.setPadding(new Insets(20, 0, 50, 0)); // in addition to page left & right margins
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

        ScrollPane scrollPane = FrontOfficeActivityUtil.createActivityPageScrollPane(carousel.getContainer(), true);
        scrollPane.getStyleClass().add("podcasts-activity"); // for CSS styling
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
