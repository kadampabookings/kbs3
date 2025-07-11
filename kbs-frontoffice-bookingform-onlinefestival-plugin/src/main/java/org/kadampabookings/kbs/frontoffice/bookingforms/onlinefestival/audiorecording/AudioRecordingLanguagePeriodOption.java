package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.audiorecording;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.util.collection.Collections;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import one.modality.base.client.i18n.I18nEntities;
import one.modality.base.client.time.ModalityDates;
import one.modality.base.shared.entities.Item;
import one.modality.base.shared.entities.ScheduledItem;
import one.modality.base.shared.entities.formatters.EventPriceFormatter;
import one.modality.ecommerce.client.workingbooking.PriceCalculator;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.document.service.PolicyAggregate;

import java.util.List;

/**
 * @author Bruno Salmon
 */
final class AudioRecordingLanguagePeriodOption {

    private final CheckBox bookButton = new CheckBox();
    private final Label periodLabel = new Label();
    private final Label priceLabel = new Label();

    AudioRecordingLanguagePeriodOption(Item item, List<ScheduledItem> scheduledItems, WorkingBooking workingBooking) {
        PolicyAggregate policyAggregate = workingBooking.getPolicyAggregate();
        FXProperties.runOnPropertyChange(selected -> {
            if (selected)
                workingBooking.bookScheduledItems(scheduledItems, false);
            else
                workingBooking.removeAttendances(Collections.filter(workingBooking.getAttendanceAdded(), a -> scheduledItems.contains(a.getScheduledItem())));
        }, bookButton.selectedProperty());
        I18nEntities.bindExpressionTextProperty(bookButton, item, "i18n(this)");
        periodLabel.setText(ModalityDates.formatHasDateSeries(scheduledItems));
        WorkingBooking periodWorkingBooking = new WorkingBooking(policyAggregate, workingBooking.getInitialDocumentAggregate());
        int totalPriceBefore = new PriceCalculator(periodWorkingBooking.getLastestDocumentAggregate()).calculateTotalPrice();
        periodWorkingBooking.bookScheduledItems(scheduledItems, false);
        int totalPriceAfter = new PriceCalculator(periodWorkingBooking.getLastestDocumentAggregate()).calculateTotalPrice();
        int optionPrice = totalPriceAfter - totalPriceBefore;
        priceLabel.setText(EventPriceFormatter.formatWithCurrency(optionPrice, policyAggregate.getEvent()));
        bookButton.getStyleClass().add("teaching-option-radio-button");
        periodLabel.getStyleClass().add("teaching-option-period-label");
        priceLabel.getStyleClass().add("teaching-option-price-label");
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
