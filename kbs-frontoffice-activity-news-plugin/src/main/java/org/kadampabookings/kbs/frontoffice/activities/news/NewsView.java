package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.extras.imagestore.ImageStore;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.platform.util.Objects;
import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import one.modality.base.frontoffice.utility.GeneralUtility;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.frontoffice.utility.TextUtility;
import one.modality.base.shared.entities.News;
import org.kadampabookings.kbs.frontoffice.operations.routes.news.RouteToArticleRequest;

import java.time.format.DateTimeFormatter;

public final class NewsView {

    private final static String FAVORITE_PATH = "M 24.066331,0 C 21.212473,0 18.540974,1.2921301 16.762259,3.4570778 14.983544,1.2920563 12.312119,0 9.4581876,0 4.2429514,0 0,4.2428782 0,9.4581873 0,13.54199 2.4351327,18.265558 7.237612,23.497667 c 3.695875,4.026405 7.716386,7.143963 8.860567,8.003592 L 16.762038,32 17.425897,31.501333 c 1.144181,-0.859629 5.164839,-3.977113 8.860788,-8.003518 4.802627,-5.23211 7.237834,-9.955751 7.237834,-14.0396277 C 33.524519,4.2428782 29.281567,0 24.066331,0 Z";

    private final BrowsingHistory history;
    private News news;
    private final Hyperlink titleLink = GeneralUtility.createHyperlink(StyleUtility.MAIN_ORANGE_COLOR);
    private final Text dateText = TextUtility.createText(StyleUtility.ELEMENT_GRAY_COLOR);
    private final Label excerptLabel = GeneralUtility.createLabel(Color.BLACK);
    private final ImageView imageView = new ImageView();
    private final SVGPath favoriteSvgPath = new SVGPath();
    private final Pane favoritePane = new MonoPane(favoriteSvgPath);
    private final Hyperlink readMoreLink = GeneralUtility.createHyperlink("readMore", StyleUtility.MAIN_ORANGE_COLOR);
    private final Pane newsContainer = new Pane(imageView, titleLink, dateText, excerptLabel, favoritePane, readMoreLink) {
        private double fontFactor;
        private double imageY, imageWidth, imageHeight, rightX, rightWidth, dateY, dateHeight, titleY, titleHeight, excerptY, excerptHeight, favoriteY, favoriteHeight, readMoreX, readMoreY, readMoreHeight;
        @Override
        protected void layoutChildren() {
            computeLayout(getWidth());
            imageView.setFitWidth(imageWidth);
            layoutInArea(imageView, 0, imageY, imageWidth, imageHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(titleLink, rightX, titleY, rightWidth, titleHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(dateText, rightX, dateY, rightWidth, dateHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(excerptLabel, rightX, excerptY, rightWidth, excerptHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(favoritePane, rightX, favoriteY, rightWidth, favoriteHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(readMoreLink, readMoreX, readMoreY, rightWidth, readMoreHeight, 0, HPos.LEFT, VPos.CENTER);
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
            return Math.max(imageY + imageHeight, favoriteY + favoriteHeight);
        }

        private void computeLayout(double width) {
            if (width == -1)
                width = getWidth();
            /* Updating fonts if necessary (required before layout) */
            double fontFactor = GeneralUtility.computeFontFactor(width);
            if (fontFactor != this.fontFactor) {
                this.fontFactor = fontFactor;
                GeneralUtility.setLabeledFont(titleLink,    StyleUtility.TEXT_FAMILY, FontWeight.SEMI_BOLD, fontFactor * StyleUtility.MAIN_TEXT_SIZE);
                TextUtility.setTextFont(      dateText,     StyleUtility.TEXT_FAMILY, FontWeight.NORMAL,    fontFactor * StyleUtility.SUB_TEXT_SIZE);
                GeneralUtility.setLabeledFont(excerptLabel, StyleUtility.TEXT_FAMILY, FontWeight.NORMAL,    fontFactor * StyleUtility.MEDIUM_TEXT_SIZE);
                GeneralUtility.setLabeledFont(readMoreLink, StyleUtility.TEXT_FAMILY, FontWeight.SEMI_BOLD, fontFactor * StyleUtility.MEDIUM_TEXT_SIZE);
            }
            /* Title: */       titleY = 0;                                titleHeight = titleLink.prefHeight(rightWidth);
            if (width <= 500) { // Small screen => vertical alignment: title above image, date, excerpt, buttons & favorite
            /* Image: */       imageY = titleY + titleHeight + 10;         imageWidth = width; imageHeight = imageView.prefHeight(imageWidth);
            /* Right side: */  rightX = 0;                                 rightWidth = width - rightX;
            /* Date: */         dateY = imageY + imageHeight + 10;         dateHeight = dateText.prefHeight(rightWidth);
            } else { // Normal or large screen => image on left, title, date, excerpt, buttons & favorite on right
            /* Image: */       imageY = 0;                                 imageWidth = width / 3; imageHeight = imageView.prefHeight(imageWidth);
            /* Right side: */  rightX = imageWidth + 20;                   rightWidth = width - rightX;
            /* Date: */         dateY = titleY + titleHeight + 10;         dateHeight = dateText.prefHeight(rightWidth);
            }
            /* Excerpt: */   excerptY = dateY + dateHeight + 20;        excerptHeight = excerptLabel.prefHeight(rightWidth);
            /* Favorite: */ favoriteY = excerptY + excerptHeight + 20; favoriteHeight = 32;
            /* ReadMore: */ readMoreY = favoriteY;                     readMoreHeight = favoriteHeight;
                            readMoreX = rightX + 50;
        }
    };

    public NewsView(BrowsingHistory history) {
        this.history = history;
        imageView.setPreserveRatio(true);
        favoriteSvgPath.setContent(FAVORITE_PATH);
        favoriteSvgPath.setStrokeWidth(2);
        GeneralUtility.onNodeClickedWithoutScroll(e -> {
            FXFavoriteNews.toggleNewsAsFavorite(news);
            updateFavorite();
            e.consume();
        }, favoritePane);
        GeneralUtility.onNodeClickedWithoutScroll(e -> browseArticle(), titleLink, imageView, readMoreLink);
    }

    public void setNews(News news) {
        this.news = news;
        updateLabeled(titleLink, news.getTitle());
        updateText(dateText, DateTimeFormatter.ofPattern("d MMMM yyyy").format(news.getDate()));
        updateLabeled(excerptLabel, news.getExcerpt());
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

    private static void updateLabeled(Labeled labeled, String newContent) {
        if (!Objects.areEquals(newContent, labeled.getText()))
            labeled.setText(newContent);
    }

}
