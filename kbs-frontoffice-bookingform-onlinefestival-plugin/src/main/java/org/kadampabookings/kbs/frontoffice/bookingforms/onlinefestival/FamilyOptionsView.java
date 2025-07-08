package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import javafx.scene.Node;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;

/**
 * @author Bruno Salmon
 */
public interface FamilyOptionsView {

    Object getTitleI18nKey();

    Node getView();

    void setWorkingBooking(WorkingBooking workingBooking);

    boolean isValid();

}
