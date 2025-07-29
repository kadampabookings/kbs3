package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.payment;

import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.util.border.BorderFactory;
import dev.webfx.extras.util.layout.Layouts;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import one.modality.base.shared.entities.formatters.EventPriceFormatter;
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
    private final Label selectedAmountCurrencyLabel = new Label();
    private final Label selectedAmountValueLabel = BookingElements.createPriceLabel();
    private final HBox selectedAmountHBox = new HBox(10, selectedAmountCurrencyLabel, selectedAmountValueLabel);
    private final HBox selectAmountHBox = new HBox(20,
        BookingElements.createWordingLabel(BookingFormI18nKeys.SelectPaymentAmount),
        selectedAmountHBox);
    private final Label paymentBottomLabel = BookingElements.createSecondaryWordingLabel(BookingFormI18nKeys.PaymentBottomMessage);
    private final VBox container = BookingElements.createFormPageVBox(true,
        BookingElements.createWordingLabel(BookingFormI18nKeys.PaymentTopMessage),
        gridPane,
        selectAmountHBox,
        embeddedLoginContainer,
        BookingElements.twoLabels(20, true, saveButton, payButton),
        paymentBottomLabel
    );

    public PaymentAmountPage(BookingForm bookingForm) {
        this.bookingForm = bookingForm;
        //gridPane.setGridLinesVisible(true);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.NEVER);
        gridPane.getColumnConstraints().setAll(c1, c2, c2);
        gridPane.setHgap(5);
        gridPane.setVgap(20);
        gridPane.setMaxWidth(450);
        gridPane.add(BookingElements.createPricePromptLabel(EcommerceI18nKeys.Total, false), 0, 0);
        gridPane.add(BookingElements.createPricePromptLabel(EcommerceI18nKeys.MinDeposit, false), 0, 1);
        gridPane.add(BookingElements.createPricePromptLabel(EcommerceI18nKeys.Deposit, false), 0, 2);
        gridPane.add(BookingElements.createPricePromptLabel(EcommerceI18nKeys.Balance, false), 0, 3);
        selectAmountHBox.setAlignment(Pos.CENTER);
        selectedAmountHBox.setPadding(new Insets(10, 20, 10, 20));
        selectedAmountHBox.setBorder(BorderFactory.newBorder(Color.BLACK, 10, 2));
        selectedAmountValueLabel.setAlignment(Pos.CENTER_RIGHT);
        selectedAmountValueLabel.setMinWidth(50);
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
        String currencySymbol = EventPriceFormatter.getEventCurrencySymbol(workingBookingProperties.getEvent());
        gridPane.add(createPriceLabel(currencySymbol), 1, 0);
        gridPane.add(createPriceLabel(workingBookingProperties.formattedTotalWithoutCurrencyProperty()), 2, 0);
        gridPane.add(createPriceLabel(currencySymbol), 1, 1);
        gridPane.add(createPriceLabel(workingBookingProperties.formattedMinDepositWithoutCurrencyProperty()), 2, 1);
        gridPane.add(createPriceLabel(currencySymbol), 1, 2);
        gridPane.add(createPriceLabel(workingBookingProperties.formattedDepositWithoutCurrencyProperty()), 2, 2);
        gridPane.add(createPriceLabel(currencySymbol), 1, 3);
        gridPane.add(createPriceLabel(workingBookingProperties.formattedBalanceWithoutCurrencyProperty()), 2, 3);
        selectedAmountCurrencyLabel.setText(currencySymbol);
        selectedAmountValueLabel.textProperty().bind(workingBookingProperties.formattedBalanceWithoutCurrencyProperty());
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

    private static Label createPriceLabel(String currencySymbol) {
        Label priceLabel = BookingElements.createPriceLabel(currencySymbol);
        GridPane.setHalignment(priceLabel, HPos.RIGHT);
        return priceLabel;
    }

    private static Label createPriceLabel(ReadOnlyStringProperty amountProperty) {
        Label priceLabel = BookingElements.createPriceLabel(amountProperty);
        GridPane.setHalignment(priceLabel, HPos.RIGHT);
        return priceLabel;
    }
}
