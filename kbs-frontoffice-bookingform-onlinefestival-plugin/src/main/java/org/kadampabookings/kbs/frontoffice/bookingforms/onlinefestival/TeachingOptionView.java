package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.platform.util.time.Times;
import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.i18n.spi.impl.I18nSubKey;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import one.modality.base.shared.entities.BookablePeriod;
import one.modality.base.shared.entities.ScheduledItem;
import one.modality.base.shared.entities.formatters.EventPriceFormatter;
import one.modality.ecommerce.client.workingbooking.PriceCalculator;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.document.service.PolicyAggregate;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Bruno Salmon
 */
final class TeachingOptionView {

    private final RadioButton radioButton = new RadioButton();
    private final Label periodLabel = new Label();
    private final Label priceLabel = new Label();

    TeachingOptionView(BookablePeriod bookablePeriod, WorkingBooking workingBooking) {
        PolicyAggregate policyAggregate = workingBooking.getPolicyAggregate();
        LocalDate startDate = bookablePeriod.getStartScheduledItem().getDate();
        LocalDate endDate = bookablePeriod.getEndScheduledItem().getDate();
        List<ScheduledItem> bookableScheduledItem = Collections.filter(policyAggregate.getTeachingScheduledItems(),
            si -> Times.isBetween(si.getDate(), startDate, endDate));
        FXProperties.runOnPropertyChange(selected -> {
            if (selected)
                workingBooking.bookScheduledItems(bookableScheduledItem, false);
        }, radioButton.selectedProperty());
        I18nControls.bindI18nProperties(radioButton, new I18nSubKey("expression: i18n(this)", bookablePeriod));
        I18nControls.bindI18nProperties(periodLabel, new I18nSubKey("expression: dateIntervalFormat(startScheduledItem.date, endScheduledItem.date) + ' (' + (endScheduledItem.date - startScheduledItem.date + 1) + ' [days])'", bookablePeriod));
        WorkingBooking periodWorkingBooking = new WorkingBooking(policyAggregate, workingBooking.getInitialDocumentAggregate());
        periodWorkingBooking.bookScheduledItems(bookableScheduledItem, false);
        int price = new PriceCalculator(periodWorkingBooking.getLastestDocumentAggregate()).calculateTotalPrice();
        priceLabel.setText(EventPriceFormatter.formatWithCurrency(price, policyAggregate.getEvent()));
        radioButton.getStyleClass().add("teaching-option-radio-button");
        periodLabel.getStyleClass().add("teaching-option-period-label");
        priceLabel.getStyleClass().add("teaching-option-price-label");
    }

    RadioButton getRadioButton() {
        return radioButton;
    }

    Label getPeriodLabel() {
        return periodLabel;
    }

    Label getPriceLabel() {
        return priceLabel;
    }

}
