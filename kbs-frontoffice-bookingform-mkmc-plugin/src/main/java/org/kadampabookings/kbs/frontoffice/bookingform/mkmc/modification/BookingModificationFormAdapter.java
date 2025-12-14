package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.modification;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormActivityCallback;
import one.modality.booking.frontoffice.bookingform.BookingFormSettings;

/**
 * Adapter that wraps a BookingModificationForm to implement the BookingForm interface.
 * This allows modification forms to be used through the unified BookingFormProvider interface.
 *
 * @author Bruno Salmon
 */
public class BookingModificationFormAdapter implements BookingForm {

    private final BookingModificationForm form;
    private BookingFormActivityCallback activityCallback;

    public BookingModificationFormAdapter(BookingModificationForm form) {
        this.form = form;
    }

    @Override
    public BookingFormSettings getSettings() {
        // Modification forms don't use standard settings
        return null;
    }

    @Override
    public Node buildUi() {
        return form.getView();
    }

    @Override
    public Node getView() {
        return form.getView();
    }

    @Override
    public void onWorkingBookingLoaded() {
        // Already loaded when form is created
    }

    @Override
    public ObservableBooleanValue transitingProperty() {
        return form.loadingProperty();
    }

    @Override
    public void setActivityCallback(BookingFormActivityCallback activityCallback) {
        this.activityCallback = activityCallback;
        // Wire up form callbacks
        form.setOnComplete(() -> {
            if (activityCallback != null) {
                activityCallback.onEndReached();
            }
        });
    }

    @Override
    public BookingFormActivityCallback getActivityCallback() {
        return activityCallback;
    }

    /**
     * Returns the underlying modification form.
     */
    public BookingModificationForm getModificationForm() {
        return form;
    }
}
