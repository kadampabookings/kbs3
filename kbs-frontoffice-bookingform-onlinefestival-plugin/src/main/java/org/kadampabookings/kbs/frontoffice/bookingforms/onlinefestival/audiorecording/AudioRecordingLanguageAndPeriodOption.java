package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.audiorecording;

import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import one.modality.base.client.i18n.I18nEntities;
import one.modality.base.client.time.ModalityDates;
import one.modality.base.shared.entities.Item;
import one.modality.base.shared.entities.ScheduledItem;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.frontoffice.bookingform.util.BookingFormUtil;

import java.util.List;

/**
 * @author Bruno Salmon
 */
final class AudioRecordingLanguageAndPeriodOption {

    private final CheckBox bookButton = new CheckBox();
    private final Label periodLabel = BookingFormUtil.createPeriodLabel();
    private final Label priceLabel = BookingFormUtil.createPriceAmountLabel();

    AudioRecordingLanguageAndPeriodOption(Item item, List<ScheduledItem> scheduledItems, WorkingBooking workingBooking) {
        I18nEntities.bindExpressionTextProperty(bookButton, item, "i18n(this)");
        periodLabel.setText(ModalityDates.formatHasDateSeries(scheduledItems));
        bookButton.setSelected(workingBooking.areScheduledItemsBooked(scheduledItems));
        BookingFormUtil.setupPeriodOption(scheduledItems, priceLabel, bookButton.selectedProperty(), workingBooking);
    }

    ButtonBase getBookButton() {
        return bookButton;
    }

    Label getPeriodLabel() {
        return periodLabel;
    }

    Label getPriceLabel() {
        return priceLabel;
    }

}
