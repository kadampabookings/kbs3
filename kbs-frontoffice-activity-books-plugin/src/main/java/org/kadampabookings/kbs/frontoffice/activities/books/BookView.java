package org.kadampabookings.kbs.frontoffice.activities.books;

import dev.webfx.extras.imagestore.ImageStore;
import dev.webfx.platform.util.Objects;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import one.modality.base.frontoffice.browser.BrowserUtil;
import one.modality.base.frontoffice.utility.GeneralUtility;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.shared.entities.Book;

public final class BookView {

    private Book book;
    private boolean hasFreeUrl;
    private final Hyperlink titleLink = GeneralUtility.createHyperlink(StyleUtility.MAIN_ORANGE_COLOR);
    private final Label excerptLabel = GeneralUtility.createLabel(Color.BLACK);
    private final ImageView imageView = new ImageView();
    private final Hyperlink freeLink = GeneralUtility.createHyperlink("FreeDownload", StyleUtility.MAIN_ORANGE_COLOR);
    private final Hyperlink orderLink = GeneralUtility.createHyperlink("Order", StyleUtility.MAIN_ORANGE_COLOR);
    private final Hyperlink extractsLink = GeneralUtility.createHyperlink("AudioExtracts", StyleUtility.MAIN_ORANGE_COLOR);
    private final Pane bookContainer = new Pane(imageView, titleLink, excerptLabel, freeLink, orderLink, extractsLink) {
        private double fontFactor;
        private double imageY, imageWidth, imageHeight, rightX, rightWidth, titleY, titleHeight, excerptY, excerptHeight,
            linksY, linksHeight, freeX, orderX, extractsX;
        @Override
        protected void layoutChildren() {
            computeLayout(getWidth());
            imageView.setFitWidth(imageWidth);
            layoutInArea(imageView, 0, imageY, imageWidth, imageHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(titleLink, rightX, titleY, rightWidth, titleHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(excerptLabel, rightX, excerptY, rightWidth, excerptHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(freeLink, freeX, linksY, rightWidth, linksHeight, 0, HPos.LEFT, VPos.CENTER);
            layoutInArea(orderLink, orderX, linksY, rightWidth, linksHeight, 0, HPos.LEFT, VPos.CENTER);
            layoutInArea(extractsLink, extractsX, linksY, rightWidth, linksHeight, 0, HPos.LEFT, VPos.CENTER);
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
            return Math.max(imageY + imageHeight, linksY + linksHeight);
        }

        private void computeLayout(double width) {
            if (width == -1)
                width = getWidth();
            /* Updating fonts if necessary (required before layout) */
            double fontFactor = GeneralUtility.computeFontFactor(width);
            if (fontFactor != this.fontFactor) {
                this.fontFactor = fontFactor;
                GeneralUtility.setLabeledFont(titleLink,    StyleUtility.TEXT_FAMILY, FontWeight.SEMI_BOLD, fontFactor * StyleUtility.MAIN_TEXT_SIZE);
                GeneralUtility.setLabeledFont(excerptLabel, StyleUtility.TEXT_FAMILY, FontWeight.NORMAL,    fontFactor * StyleUtility.MEDIUM_TEXT_SIZE);
                GeneralUtility.setLabeledFont(freeLink,     StyleUtility.TEXT_FAMILY, FontWeight.SEMI_BOLD, fontFactor * StyleUtility.MEDIUM_TEXT_SIZE);
                GeneralUtility.setLabeledFont(orderLink,    StyleUtility.TEXT_FAMILY, FontWeight.SEMI_BOLD, fontFactor * StyleUtility.MEDIUM_TEXT_SIZE);
                GeneralUtility.setLabeledFont(extractsLink, StyleUtility.TEXT_FAMILY, FontWeight.SEMI_BOLD, fontFactor * StyleUtility.MEDIUM_TEXT_SIZE);
            }
            if (width <= 500) { // Small screen => vertical alignment: title above image, date, excerpt, buttons & favorite
            /* Title: */       titleY = 0;                                 titleHeight = titleLink.prefHeight(rightWidth);
            /* Image: */       imageY = titleY + titleHeight + 10;         imageWidth = width; imageHeight = imageView.prefHeight(imageWidth);
            /* Right side: */  rightX = 0;                                 rightWidth = width - rightX;
            } else { // Normal or large screen => image on left, title, date, excerpt, buttons & favorite on right
            /* Image: */       imageY = 0;                                 imageWidth = width / 3; imageHeight = imageView.prefHeight(imageWidth);
                /* Title: */   titleY = imageWidth * 0.1;                  titleHeight = titleLink.prefHeight(rightWidth);
            /* Right side: */  rightX = imageWidth + 20;                   rightWidth = width - rightX;
            }
            /* Excerpt: */   excerptY = /*dateY + dateHeight*/titleY + titleHeight  + 20; excerptHeight = excerptLabel.prefHeight(rightWidth);
            /* Links: */       linksY = excerptY + excerptHeight + 20;      linksHeight = 32;
            /* Free */          freeX = rightX;
            /* Order: */       orderX = freeX + (hasFreeUrl ? freeLink.prefWidth(-1) + 50 : 0);
            /* Extracts: */ extractsX = orderX + orderLink.prefWidth(-1) + 50;
        }
    };

    public BookView(Runnable audioExtractsRunnable) {
        imageView.setPreserveRatio(true);
        GeneralUtility.onNodeClickedWithoutScroll(e -> freeDownload(), freeLink);
        GeneralUtility.onNodeClickedWithoutScroll(e -> order(), orderLink);
        GeneralUtility.onNodeClickedWithoutScroll(e -> audioExtractsRunnable.run(), extractsLink);
    }

    public void setBook(Book book) {
        this.book = book;
        updateLabeled(titleLink, book.getTitle());
        updateLabeled(excerptLabel, book.getDescription());
        imageView.setImage(ImageStore.getOrCreateImage(book.getImageUrl()));
        hasFreeUrl = book.getFreeUrl() != null;
        freeLink.setVisible(hasFreeUrl);
        //extractsLink.setVisible("https://howtotyl.com".equals(book.getFreeUrl()));
        extractsLink.setVisible(false); // because of issue with reactive object mapper active binding in BooksActivity
    }

    public Node getView() {
        return bookContainer;
    }

    private void order() {
        BrowserUtil.openExternalBrowser(book.getOrderUrl());
    }

    private void freeDownload() {
        String freeUrl = book.getFreeUrl();
        if (freeUrl.startsWith("https://tharpa.com")) // issue with Stripe which doesn't allow embed in iFrame
            BrowserUtil.openExternalBrowser(freeUrl);
        else
            BrowserUtil.openInternalBrowser(freeUrl);
    }

    private static void updateLabeled(Labeled labeled, String newContent) {
        if (!Objects.areEquals(newContent, labeled.getText()))
            labeled.setText(newContent);
    }

}
