package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.teaching;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.platform.util.time.Times;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import one.modality.base.client.i18n.I18nEntities;
import one.modality.base.shared.entities.BookablePeriod;
import one.modality.base.shared.entities.ScheduledItem;
import one.modality.base.shared.entities.formatters.EventPriceFormatter;
import one.modality.ecommerce.shared.pricecalculator.PriceCalculator;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.document.service.PolicyAggregate;
import one.modality.ecommerce.frontoffice.bookingform.util.BookingFormUtil;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Bruno Salmon
 */
final class TeachingPeriodOption {

    private final RadioButton radioButton = new RadioButton();
    private final Label periodLabel = BookingFormUtil.createPeriodLabel();
    private final Label priceLabel = BookingFormUtil.createPriceAmountLabel();

    TeachingPeriodOption(BookablePeriod bookablePeriod, WorkingBooking workingBooking) {
        PolicyAggregate policyAggregate = workingBooking.getPolicyAggregate();
        LocalDate startDate = bookablePeriod.getStartScheduledItem().getDate();
        LocalDate endDate = bookablePeriod.getEndScheduledItem().getDate();
        List<ScheduledItem> bookableScheduledItem = Collections.filter(policyAggregate.getTeachingScheduledItems(),
            si -> Times.isBetween(si.getDate(), startDate, endDate));
        FXProperties.runOnPropertyChange(selected -> {
            if (selected)
                workingBooking.bookScheduledItems(bookableScheduledItem, false);
        }, radioButton.selectedProperty());
        I18nEntities.bindExpressionProperties(radioButton, bookablePeriod, "i18n(this)");
        I18nEntities.bindExpressionProperties(periodLabel, bookablePeriod, "dateIntervalFormat(startScheduledItem.date, endScheduledItem.date) + ' (' + (endScheduledItem.date - startScheduledItem.date + 1) + ' [days])'");
        WorkingBooking periodWorkingBooking = new WorkingBooking(policyAggregate, workingBooking.getInitialDocumentAggregate());
        periodWorkingBooking.bookScheduledItems(bookableScheduledItem, false);
        int price = new PriceCalculator(periodWorkingBooking.getLastestDocumentAggregate()).calculateTotalPrice();
        priceLabel.setText(EventPriceFormatter.formatWithCurrency(price, policyAggregate.getEvent()));
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
