package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.prerequisite;

import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.platform.useragent.UserAgent;
import dev.webfx.stack.orm.entity.Entities;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import one.modality.booking.client.workingbooking.WorkingBooking;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingelements.BookingElements;
import one.modality.booking.frontoffice.bookingform.multipages.BookingFormPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.OnlineFestivalI18nKeys;

/**
 * @author Bruno Salmon
 */
public final class PrerequisitePage implements BookingFormPage {

    private final Label onlineEmpowermentUKMessageLabel = I18nControls.bindI18nProperties(BookingElements.createWordingLabel(), OnlineFestivalI18nKeys.OnlineEmpowermentUKMessage);
    private final CheckBox onlineEmpowermentUKConfirmCheckBox = new CheckBox();
    private final Label onlineEmpowermentUKConfirmLabel = BookingElements.wordingLabel(new Label(), false, OnlineFestivalI18nKeys.OnlineEmpowermentUKConfirm);
    // The reason for using an HBox + checkbox + label instead of a simple checkbox is to keep the checkbox on top when
    // the label is multiline. (the standard checkbox is centering it vertically, but we prefer it to be on top)
    private final HBox onlineEmpowermentUKConfirmHBox = new HBox(5, onlineEmpowermentUKConfirmCheckBox, onlineEmpowermentUKConfirmLabel);
    private final VBox container = BookingElements.createFormPageVBox(true,
        onlineEmpowermentUKMessageLabel,
        onlineEmpowermentUKConfirmHBox
    );

    public PrerequisitePage() { // A few tweaks:
        // Removing the text center alignement set by wordingLabel()
        onlineEmpowermentUKConfirmLabel.setTextAlignment(TextAlignment.LEFT);
        // Reproducing the standard checkbox behavior which also reacts on label click
        onlineEmpowermentUKConfirmLabel.setOnMouseClicked(e -> onlineEmpowermentUKConfirmCheckBox.setSelected(!onlineEmpowermentUKConfirmCheckBox.isSelected()));
        // Only on the web version:
        if (UserAgent.isBrowser()) {
            // Removing the default text cursor on the label
            onlineEmpowermentUKConfirmLabel.setCursor(Cursor.DEFAULT);
            // Preventing the checkbox to shrink horizontally
            onlineEmpowermentUKConfirmCheckBox.setMinWidth(Region.USE_PREF_SIZE);
            // Removing the HBox spacing (otherwise the space is too large for some reason)
            onlineEmpowermentUKConfirmHBox.setSpacing(0);
            // Correcting the vertical position of the checkbox so it aligns vertically with the first line of the label
            onlineEmpowermentUKConfirmCheckBox.setTranslateY(2);
            // Forcing a transparent border to remove the dashed blue border around the focused checkbox
            onlineEmpowermentUKConfirmCheckBox.setBorder(Border.stroke(Color.TRANSPARENT));
        } else {
            // Correcting the vertical position of the checkbox so it aligns vertically with the first line of the label
            onlineEmpowermentUKConfirmCheckBox.setTranslateY(-2); // The correction is different for OpenJFX and WebFX
        }
    }

    @Override
    public Object getTitleI18nKey() {
        return OnlineFestivalI18nKeys.Prerequisite;
    }

    @Override
    public Node getView() {
        return container;
    }

    @Override
    public boolean isApplicableToBooking(WorkingBooking workingBooking) {
        // We use this booking form also for MKMC online empowerment weekends (hardcoded for now)
        return workingBooking.isNewBooking() && Entities.samePrimaryKey(workingBooking.getEvent().getType(), 24);
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {

    }

    @Override
    public ObservableBooleanValue canGoForwardProperty() {
        return onlineEmpowermentUKConfirmCheckBox.selectedProperty();
    }
}
