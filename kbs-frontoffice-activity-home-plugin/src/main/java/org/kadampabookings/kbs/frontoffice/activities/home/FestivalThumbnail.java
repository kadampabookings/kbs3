package org.kadampabookings.kbs.frontoffice.activities.home;

import dev.webfx.extras.panes.GoldenRatioPane;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.util.animation.Animations;
import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.i18n.I18nKeys;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.i18n.spi.impl.I18nSubKey;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.ui.dialog.DialogCallback;
import dev.webfx.stack.ui.dialog.DialogUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import one.modality.base.client.mainframe.fx.FXMainFrameDialogArea;
import one.modality.base.frontoffice.utility.browser.BrowserUtil;
import one.modality.base.shared.entities.Event;
import one.modality.event.client.lifecycle.EventLifeCycle;
import one.modality.event.frontoffice.activities.booking.BookingI18nKeys;
import org.kadampabookings.kbs.client.festivaltypes.FestivalType;

/**
 * @author Bruno Salmon
 */
final class FestivalThumbnail {

    private final Event festival;
    private final FestivalType festivalType;

    public FestivalThumbnail(Event festival) {
        this.festival = festival;
        int typeId = Numbers.toInteger(Entities.getPrimaryKey(festival.getType()));
        festivalType = FestivalType.fromTypeId(typeId);
    }

    Node getView() {
        int year = festival.getStartDate().getYear();
        // If the loaded festival is past, we rather display the next festival with estimated dates and coming soon button
        boolean inferNextFestival = EventLifeCycle.isPastEvent(festival);
        if (inferNextFestival) {
            year++;
            // Note: a bit dirty, but we changed the festival dates with the estimated one for next year
            festival.setStartDate(festivalType.evaluateStartDate(year));
            festival.setEndDate(festivalType.evaluateEndDate(festival.getStartDate()));
        }
        Label festivalName = I18nControls.newLabel(I18nKeys.embedInString("[0] {0}", festivalType.getLongI18nKey()), year);
        festivalName.setWrapText(true);
        festivalName.setTextAlignment(TextAlignment.CENTER);
        festivalName.getStyleClass().setAll("festival-name");

        boolean canBookNow = EventLifeCycle.canBookNow(festival);
        Button button = I18nControls.newButton(canBookNow ? BookingI18nKeys.bookNow : BookingI18nKeys.comingSoon);
        button.setPrefSize(240, 48);
        if (canBookNow) {
            button.setOnAction(e -> openBookNowDialog());
        }

        // Embedding the festival name into a growing pane so that dates and button all aligned the same at the bottom across the 3 thumbnails
        MonoPane festivalNamePane = new MonoPane(festivalName);
        festivalNamePane.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(festivalNamePane, Priority.SOMETIMES);
        festivalNamePane.setAlignment(Pos.TOP_CENTER);

        VBox container = new VBox(48,
            festivalNamePane,
            I18n.newText(new I18nSubKey("expression: dateIntervalFormat(startDate, endDate)", festival)),
            button
        );
        container.setPrefSize(360, 400);
        container.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        container.setPadding(new Insets(48, 8, 48, 8));
        container.setAlignment(Pos.TOP_CENTER);
        container.getStyleClass().setAll("festival-thumbnail", festivalType.getStyleClass());
        return container;
    }

    private void openBookNowDialog() {
        Hyperlink bookInPersonLink = I18nControls.newHyperlink(FrontOfficeHomeI18nKeys.BookInPerson);
        Label bookOnline = I18nControls.newLabel(FrontOfficeHomeI18nKeys.BookOnline);
        bookOnline.setWrapText(true);
        bookOnline.setTextAlignment(TextAlignment.CENTER);
        bookOnline.setDisable(true);
        VBox bookNowOptionsBox = new VBox(20, bookInPersonLink, bookOnline);
        bookNowOptionsBox.setAlignment(Pos.CENTER);
        Pane bookNowDialog = new GoldenRatioPane(bookNowOptionsBox);
        bookNowDialog.getStyleClass().setAll("book-now-dialog");
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        bookNowDialog.setPrefSize(Math.min(700, screenBounds.getWidth() * 0.8), Math.min(500, screenBounds.getHeight() * 0.3));
        Pane dialogArea = FXMainFrameDialogArea.getDialogArea();
        DialogCallback callback = DialogUtil.showModalNodeInGoldLayout(bookNowDialog, dialogArea);
        dialogArea.setOnMouseClicked(e2 -> callback.closeDialog());
        bookInPersonLink.setOnAction(e2 -> {
            BrowserUtil.openExternalBrowser(EventLifeCycle.getKbs2BookingFormUrl(festival));
            callback.closeDialog();
        });
        Animations.fadeIn(bookNowDialog);
    }
}
