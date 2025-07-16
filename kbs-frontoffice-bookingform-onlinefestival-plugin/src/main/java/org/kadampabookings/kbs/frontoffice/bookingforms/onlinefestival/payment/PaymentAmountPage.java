package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.payment;

import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.kit.util.properties.FXProperties;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import one.modality.ecommerce.client.i18n.EcommerceI18nKeys;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.ecommerce.frontoffice.bookingform.BookingForm;
import one.modality.ecommerce.frontoffice.bookingform.BookingFormActivityCallback;
import one.modality.ecommerce.frontoffice.bookingform.BookingFormI18nKeys;
import one.modality.ecommerce.frontoffice.bookingform.multipages.BookingFormPage;
import one.modality.ecommerce.frontoffice.bookingelements.BookingElements;

/**
 * @author Bruno Salmon
 */
public final class PaymentAmountPage implements BookingFormPage {

    private final BookingForm bookingForm;
    private final GridPane gridPane = BookingElements.createOptionsGridPane(false);
    private final MonoPane embeddedLoginContainer = new MonoPane();
    private final Button saveButton = BookingElements.createPrimaryButton(BookingFormI18nKeys.SaveBooking);
    private final Button payButton = BookingElements.createBlackButton(BookingFormI18nKeys.PayNow1);
    private final IntegerProperty selectedAmountProperty = new SimpleIntegerProperty();
    private final VBox container = BookingElements.createPageVBox("payment", true,
        BookingElements.createStrongLabel(BookingFormI18nKeys.PaymentTopMessage),
        gridPane,
        BookingElements.createStrongLabel(BookingFormI18nKeys.SelectPaymentAmount),
        embeddedLoginContainer,
        BookingElements.buttonBar(saveButton, payButton),
        BookingElements.createSecondaryLabel(BookingFormI18nKeys.PaymentBottomMessage)
    );

    public PaymentAmountPage(BookingForm bookingForm) {
        this.bookingForm = bookingForm;
        gridPane.setHgap(350);
        gridPane.setVgap(30);
        gridPane.add(BookingElements.createPricePromptLabel(EcommerceI18nKeys.Total, false), 0, 0);
        gridPane.add(BookingElements.createPricePromptLabel(EcommerceI18nKeys.MinDeposit, false), 0, 1);
    }

    @Override
    public Object getTitleI18nKey() {
        return BookingFormI18nKeys.Payment;
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
        gridPane.add(BookingElements.createPriceAmountLabel(workingBookingProperties.formattedTotalProperty()), 1, 0);
        gridPane.add(BookingElements.createPriceAmountLabel(workingBookingProperties.formattedBalanceProperty()), 1, 1);
        BookingFormActivityCallback activityCallback = bookingForm.getActivityCallback();
        BooleanBinding disableSubmit = FXProperties.not(activityCallback.readyToSubmitBookingProperty());
        saveButton.disableProperty().bind(disableSubmit);
        saveButton.setOnAction(e -> activityCallback.submitBooking(0, saveButton, payButton));
        selectedAmountProperty.bind(workingBookingProperties.balanceProperty());
        I18nControls.bindI18nProperties(payButton, BookingFormI18nKeys.PayNow1, workingBookingProperties.getFormattedBalance());
        payButton.disableProperty().bind(disableSubmit.or(selectedAmountProperty.lessThanOrEqualTo(0)));
        payButton.setOnAction(e -> activityCallback.submitBooking(workingBookingProperties.getBalance(), payButton, saveButton));
    }
}
