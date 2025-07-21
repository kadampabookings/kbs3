package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.payment;

import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.util.layout.Layouts;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import one.modality.ecommerce.client.i18n.EcommerceI18nKeys;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.ecommerce.frontoffice.bookingelements.BookingElements;
import one.modality.ecommerce.frontoffice.bookingform.BookingForm;
import one.modality.ecommerce.frontoffice.bookingform.BookingFormActivityCallback;
import one.modality.ecommerce.frontoffice.bookingform.BookingFormI18nKeys;
import one.modality.ecommerce.frontoffice.bookingform.multipages.BookingFormPage;

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
    private final Label paymentBottomLabel = BookingElements.createSecondaryWordingLabel(BookingFormI18nKeys.PaymentBottomMessage);
    private final VBox container = BookingElements.createFormPageVBox(true,
        BookingElements.createWordingLabel(BookingFormI18nKeys.PaymentTopMessage),
        gridPane,
        BookingElements.createWordingLabel(BookingFormI18nKeys.SelectPaymentAmount),
        embeddedLoginContainer,
        BookingElements.buttonBar(saveButton, payButton),
        paymentBottomLabel
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
        gridPane.add(BookingElements.createPriceLabel(workingBookingProperties.formattedTotalProperty()), 1, 0);
        gridPane.add(BookingElements.createPriceLabel(workingBookingProperties.formattedBalanceProperty()), 1, 1);
        BookingFormActivityCallback activityCallback = bookingForm.getActivityCallback();
        // We hide the save button if there are no changes
        Layouts.setManagedAndVisibleProperties(saveButton, workingBookingProperties.hasChanges());
        // Same with paymentBottomLabel because it says: If you save the booking for later, it will be saved in your orders until you complete the minimum payment.
        Layouts.bindManagedAndVisiblePropertiesTo(saveButton.visibleProperty(), paymentBottomLabel);
        // And when it's visible, we disable if the booking is not ready to submit
        BooleanBinding disableSubmit = activityCallback.readyToSubmitBookingProperty().not();
        saveButton.disableProperty().bind(disableSubmit);
        // When it's visible and enabled, the user can submit the changes but with no deposit to pay
        saveButton.setOnAction(e -> activityCallback.submitBooking(0, saveButton, payButton));
        // For now because the context is only online Festivals so far, the amount to pay is necessarily the whole balance
        selectedAmountProperty.bind(workingBookingProperties.balanceProperty());
        // We show the amount to pay in the button itself
        I18nControls.bindI18nProperties(payButton, BookingFormI18nKeys.PayNow1, workingBookingProperties.getFormattedBalance());
        // But it is disabled if the booking is not ready to submit or if there is nothing to pay
        payButton.disableProperty().bind(disableSubmit.or(selectedAmountProperty.lessThanOrEqualTo(0)));
        // When it's enabled, the user can submit the changes and pay the selected amount
        payButton.setOnAction(e -> activityCallback.submitBooking(selectedAmountProperty.get(), payButton, saveButton));
    }
}
