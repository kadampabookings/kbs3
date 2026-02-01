package org.kadampabookings.kbs.frontoffice.activities.home;

import dev.webfx.extras.i18n.I18nKeys;
import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.panes.GoldenRatioPane;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.extras.util.animation.Animations;
import dev.webfx.extras.util.dialog.DialogCallback;
import dev.webfx.extras.util.dialog.DialogUtil;
import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.orm.entity.Entities;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import one.modality.base.client.i18n.I18nEntities;
import one.modality.base.client.mainframe.fx.FXMainFrameDialogArea;
import one.modality.base.shared.entities.Event;
import one.modality.booking.frontoffice.bookingform.BookingFormI18nKeys;
import one.modality.event.client.lifecycle.EventLifeCycle;
import one.modality.event.frontoffice.activities.book.BookStarter;
import org.kadampabookings.kbs.client.festivaltypes.FXFestivals;
import org.kadampabookings.kbs.client.festivaltypes.FestivalType;

import static org.kadampabookings.kbs.frontoffice.activities.home.FrontOfficeHomeCssSelectors.*;

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
        /* Finally commented. We displayed "Closed" instead.
        boolean inferNextFestival = EventLifeCycle.isPastEvent(festival);
        if (inferNextFestival) {
            year++;
            // Note: a bit dirty, but we changed the festival dates with the estimated one for next year
            festival.setStartDate(festivalType.evaluateStartDate(year));
            festival.setEndDate(festivalType.evaluateEndDate(festival.getStartDate()));
        }*/
        Label festivalName = I18nControls.newLabel(I18nKeys.embedInString("[0] {0}", festivalType.getLongI18nKey()), year);
        festivalName.setWrapText(true);
        festivalName.setTextAlignment(TextAlignment.CENTER);
        festivalName.getStyleClass().setAll(festival_name);

        boolean canBookNow = EventLifeCycle.canBookNow(festival);
        Button button = Bootstrap.button(I18nControls.newButton(
            canBookNow ? BookingFormI18nKeys.bookNow
                : EventLifeCycle.isPastEvent(festival) || EventLifeCycle.isClosed(festival, false) ? BookingFormI18nKeys.closed
                : BookingFormI18nKeys.comingSoon));
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
            I18nEntities.newExpressionText(festival, "dateIntervalFormat(startDate, endDate)"),
            button
        );
        container.setPrefSize(360, 400);
        container.setMinSize(360, 400);
        container.setMaxSize(2 * 360, 2 * 400);
        container.setPadding(new Insets(48, 8, 48, 8));
        container.setAlignment(Pos.TOP_CENTER);
        container.getStyleClass().setAll(festival_thumbnail, festivalType.getStyleClass());
        return container;
    }

    private void openBookNowDialog() {
        Hyperlink bookInPersonLink = I18nControls.newHyperlink(FrontOfficeHomeI18nKeys.AttendInPerson);

        Event onlineFestival = FXFestivals.lastFestivalProperty(festivalType, true).get();
        Labeled bookOnline;
        if (!EventLifeCycle.canBookNow(onlineFestival)) {
            bookOnline = I18nControls.newLabel(FrontOfficeHomeI18nKeys.OnlineBookingsOpenLater);
            bookOnline.setDisable(true);
        } else {
            bookOnline = I18nControls.newHyperlink(FrontOfficeHomeI18nKeys.AttendOnline);
        }
        bookOnline.setWrapText(true);
        bookOnline.setTextAlignment(TextAlignment.CENTER);

        VBox bookNowOptionsBox = new VBox(20, bookInPersonLink, bookOnline);
        bookNowOptionsBox.setAlignment(Pos.CENTER);

        Pane bookNowDialog = new GoldenRatioPane(bookNowOptionsBox);
        bookNowDialog.getStyleClass().setAll(book_now_dialog);
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        bookNowDialog.setPrefSize(Math.min(700, screenBounds.getWidth() * 0.8), Math.min(500, screenBounds.getHeight() * 0.3));
        Pane dialogArea = FXMainFrameDialogArea.getDialogArea();
        DialogCallback callback = DialogUtil.showModalNodeInGoldLayout(bookNowDialog, dialogArea);
        dialogArea.setOnMouseClicked(e2 -> callback.closeDialog());

        bookInPersonLink.setOnAction(e2 -> {
            BookStarter.startBookEvent(festival);
            callback.closeDialog();
        });

        if (bookOnline instanceof Hyperlink) {
            ((Hyperlink) bookOnline).setOnAction(e2 -> {
                BookStarter.startBookEvent(onlineFestival);
                callback.closeDialog();
            });
        }

        Animations.fadeIn(bookNowDialog);
    }

}
