package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.payment;

import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.kit.util.properties.FXProperties;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import one.modality.base.client.bootstrap.ModalityStyle;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.event.frontoffice.activities.booking.BookingI18nKeys;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingForm;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingFormActivityCallback;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.multipages.BookingFormPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.OnlineFestivalI18nKeys;

/**
 * @author Bruno Salmon
 */
public final class PaymentAmountPage implements BookingFormPage {

    private final BookingForm bookingForm;
    private final MonoPane embeddedLoginContainer = new MonoPane();
    private final Button saveButton = Bootstrap.largeButton(Bootstrap.primaryButton(I18nControls.newButton(OnlineFestivalI18nKeys.SaveBooking)));
    private final Button payButton = Bootstrap.largeButton(ModalityStyle.blackButton(I18nControls.newButton(OnlineFestivalI18nKeys.PayNow)));
    private final VBox container;

    public PaymentAmountPage(BookingForm bookingForm) {
        this.bookingForm = bookingForm;
        HBox buttonBar = new HBox(20, saveButton, payButton);
        buttonBar.setMaxWidth(Region.USE_PREF_SIZE);
        container = new VBox(50,
            embeddedLoginContainer,
            buttonBar
        );
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(48, 0, 48, 0));
        container.setMaxWidth(Double.MAX_VALUE);
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
        BookingFormActivityCallback activityCallback = bookingForm.getActivityCallback();
        BooleanBinding disableSubmit = FXProperties.not(activityCallback.readyToSubmitBookingProperty());
        saveButton.disableProperty().bind(disableSubmit);
        saveButton.setOnAction(e -> activityCallback.submitBooking(0));
        payButton.disableProperty().bind(disableSubmit);
        payButton.setOnAction(e -> activityCallback.submitBooking(workingBookingProperties.getBalance()));
    }
}
