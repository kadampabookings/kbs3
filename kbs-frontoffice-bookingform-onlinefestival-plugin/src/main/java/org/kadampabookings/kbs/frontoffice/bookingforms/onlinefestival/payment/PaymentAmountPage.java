package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.payment;

import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.kit.util.properties.FXProperties;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import one.modality.ecommerce.client.i18n.EcommerceI18nKeys;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.event.frontoffice.activities.booking.BookingI18nKeys;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingForm;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingFormActivityCallback;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.multipages.BookingFormPage;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.util.BookingFormUtil;

/**
 * @author Bruno Salmon
 */
public final class PaymentAmountPage implements BookingFormPage {

    private final BookingForm bookingForm;
    private final GridPane gridPane = BookingFormUtil.createOptionsGridPane(false);
    private final MonoPane embeddedLoginContainer = new MonoPane();
    private final Button saveButton = BookingFormUtil.createPrimaryButton(BookingI18nKeys.SaveBooking);
    private final Button payButton = BookingFormUtil.createBlackButton(BookingI18nKeys.PayNow1);
    private final VBox container = BookingFormUtil.createPageVBox("payment", true,
        BookingFormUtil.createStrongLabel(BookingI18nKeys.PaymentTopMessage),
        gridPane,
        BookingFormUtil.createStrongLabel(BookingI18nKeys.SelectPaymentAmount),
        embeddedLoginContainer,
        BookingFormUtil.buttonBar(saveButton, payButton),
        BookingFormUtil.createSecondaryLabel(BookingI18nKeys.PaymentBottomMessage)
    );

    public PaymentAmountPage(BookingForm bookingForm) {
        this.bookingForm = bookingForm;
        gridPane.setHgap(350);
        gridPane.setVgap(30);
        gridPane.add(BookingFormUtil.createPricePromptLabel(EcommerceI18nKeys.Total, false), 0, 0);
        gridPane.add(BookingFormUtil.createPricePromptLabel(EcommerceI18nKeys.MinDeposit, false), 0, 1);
    }

    @Override
    public Object getTitleI18nKey() {
        return BookingI18nKeys.Payment;
    }

    @Override
    public Node getView() {
        return container;
    }

    @Override
    public MonoPane getEmbeddedLoginContainer() {
        return embeddedLoginContainer;
    }

    @Override
    public boolean isShowingOwnSubmitButton() {
        return true;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
        gridPane.add(BookingFormUtil.createPriceAmountLabel(workingBookingProperties.formattedTotalProperty()), 1, 0);
        gridPane.add(BookingFormUtil.createPriceAmountLabel(workingBookingProperties.formattedBalanceProperty()), 1, 1);
        BookingFormActivityCallback activityCallback = bookingForm.getActivityCallback();
        BooleanBinding disableSubmit = FXProperties.not(activityCallback.readyToSubmitBookingProperty());
        saveButton.disableProperty().bind(disableSubmit);
        saveButton.setOnAction(e -> activityCallback.submitBooking(0));
        I18nControls.bindI18nProperties(payButton, BookingI18nKeys.PayNow1, workingBookingProperties.getFormattedBalance());
        payButton.disableProperty().bind(disableSubmit);
        payButton.setOnAction(e -> activityCallback.submitBooking(workingBookingProperties.getBalance()));
    }
}
