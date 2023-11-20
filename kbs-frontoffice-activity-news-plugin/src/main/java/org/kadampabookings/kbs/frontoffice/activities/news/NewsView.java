package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.extras.imagestore.ImageStore;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.platform.util.Objects;
import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import one.modality.base.frontoffice.utility.GeneralUtility;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.frontoffice.utility.TextUtility;
import one.modality.base.shared.entities.News;
import org.kadampabookings.kbs.frontoffice.operations.routes.news.RouteToArticleRequest;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public final class NewsView {

    private final static String FAVORITE_PATH = "M 24.066331,0 C 21.212473,0 18.540974,1.2921301 16.762259,3.4570778 14.983544,1.2920563 12.312119,0 9.4581876,0 4.2429514,0 0,4.2428782 0,9.4581873 0,13.54199 2.4351327,18.265558 7.237612,23.497667 c 3.695875,4.026405 7.716386,7.143963 8.860567,8.003592 L 16.762038,32 17.425897,31.501333 c 1.144181,-0.859629 5.164839,-3.977113 8.860788,-8.003518 4.802627,-5.23211 7.237834,-9.955751 7.237834,-14.0396277 C 33.524519,4.2428782 29.281567,0 24.066331,0 Z";

    private final BrowsingHistory history;
    private News news;
    private final Label titleLabel = GeneralUtility.getMainLabel(null, StyleUtility.MAIN_BLUE);
    private final Text dateText = TextUtility.getSubText(null);
    private final Label excerptLabel = GeneralUtility.getMainLabel(null, StyleUtility.VICTOR_BATTLE_BLACK);
    private final ImageView imageView = new ImageView();
    private final SVGPath favoriteSvgPath = new SVGPath();
    private final Pane favoritePane = new MonoPane(favoriteSvgPath);
    private final Pane newsContainer = new Pane(imageView, titleLabel, dateText, excerptLabel, favoritePane) {
        private double imageY, imageWidth, rightX, rightWidth, dateY, dateHeight, titleY, titleHeight, excerptY, excerptHeight, favoriteY, favoriteHeight;
        @Override
        protected void layoutChildren() {
            computeLayout(getWidth());
            imageView.setFitWidth(imageWidth);
            layoutInArea(imageView, 0, imageY, imageWidth, 0, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(titleLabel, rightX, titleY, rightWidth, titleHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(dateText, rightX, dateY, rightWidth, dateHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(excerptLabel, rightX, excerptY, rightWidth, excerptHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(favoritePane, rightX, favoriteY, rightWidth, favoriteHeight, 0, HPos.LEFT, VPos.TOP);
        }

        @Override
        protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }

        @Override
        protected double computeMaxHeight(double width) {
            return computePrefHeight(width);
        }

        @Override
        protected double computePrefHeight(double width) {
            computeLayout(width);
            return Math.max(imageY + imageView.prefHeight(imageWidth), favoriteY + favoriteHeight);
        }

        private void computeLayout(double width) {
            if (width == -1)
                width = getWidth();
            /* Image: */       imageY = 0;                                 imageWidth = width / 3;
            /* Right side: */  rightX = imageWidth + 20;                   rightWidth = width - rightX;
            /* Title: */       titleY = 0;                                titleHeight = titleLabel.prefHeight(rightWidth);
            /* Date: */         dateY = titleY + titleHeight + 5;          dateHeight = dateText.prefHeight(rightWidth);
            /* Excerpt: */   excerptY = dateY + dateHeight + 5;         excerptHeight = excerptLabel.prefHeight(rightWidth);
            /* Favorite: */ favoriteY = excerptY + excerptHeight + 20; favoriteHeight = 32;
        }
    };

    private double screenPressedY;

    public NewsView(BrowsingHistory history) {
        this.history = history;
        imageView.setPreserveRatio(true);
        favoriteSvgPath.setContent(FAVORITE_PATH);
        favoriteSvgPath.setStrokeWidth(2);
        favoritePane.setOnMousePressed(e -> {
            FXFavoriteNews.toggleNewsAsFavorite(news);
            updateFavorite();
            e.consume();
        });
        newsContainer.setCursor(Cursor.HAND);
        newsContainer.setOnMousePressed(e -> screenPressedY = e.getScreenY());
        newsContainer.setOnMouseReleased(e -> {
            if (Math.abs(e.getScreenY() - screenPressedY) < 10) // This is to avoid browsing the article when the user just wants to scroll
                browseArticle();
        });
    }

    public void setNews(News news) {
        this.news = news;
        updateLabel(titleLabel, news.getTitle().toUpperCase());
        updateText(dateText, DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(news.getDate()));
        updateLabel(excerptLabel, news.getExcerpt());
        imageView.setImage(ImageStore.getOrCreateImage(news.getImageUrl()));
        updateFavorite();
    }

    public Node getView() {
        return newsContainer;
    }

    private void browseArticle() {
        FXDisplayedArticle.setDisplayedArticle(news);
        new RouteToArticleRequest(news, history).execute();
    }

    private void updateFavorite() {
        boolean isFavorite = FXFavoriteNews.isNewsMarkedAsFavorite(news);
        favoriteSvgPath.setStroke(isFavorite ? Color.RED : Color.GRAY);
        favoriteSvgPath.setFill(isFavorite ? Color.RED : null);
    }

    private static void updateText(Text text, String newContent) {
        if (!Objects.areEquals(newContent, text.getText()))
            text.setText(newContent);
    }

    private static void updateLabel(Label label, String newContent) {
        if (!Objects.areEquals(newContent, label.getText()))
            label.setText(newContent);
    }
}
