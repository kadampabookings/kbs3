package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import dev.webfx.extras.panes.TransitionPane;
import dev.webfx.extras.util.layout.Layouts;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.event.frontoffice.activities.booking.process.event.AbstractBookingForm;
import one.modality.event.frontoffice.activities.booking.process.event.BookEventActivity;
import one.modality.event.frontoffice.activities.booking.process.event.BookingFormSettings;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.audiorecording.AudioRecordingOptionsView;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.teaching.TeachingOptionsView;

/**
 * @author Bruno Salmon
 */
final class OnlineFestivalBookingForm extends AbstractBookingForm {

    private static final double MAX_WIDTH = 800;

    private final NavigationBar navigationBar = new NavigationBar();
    private final TransitionPane transitionPane = new TransitionPane();
    private final FamilyOptionsView[] familyOptionsViews = {
        new TeachingOptionsView(),
        new AudioRecordingOptionsView(),
    };
    private int currentFamilyOptionsViewIndex;

    public OnlineFestivalBookingForm(BookEventActivity activity, BookingFormSettings settings) {
        super(activity, settings);
        Layouts.setFixedWidth(transitionPane, MAX_WIDTH);
    }

    @Override
    public Node buildUi() {
        BorderPane container = new BorderPane();
        container.setTop(navigationBar.getView());
        container.setCenter(transitionPane);
        container.setBottom(new PriceBar(activity.getWorkingBookingProperties()).getView());
        container.getStyleClass().add("online-festival-booking-form");
        container.setMaxWidth(MAX_WIDTH);
        navigationBar.getBackButton().setOnMouseClicked(e -> {
            navigateToFamilyOptionsView(currentFamilyOptionsViewIndex - 1);
        });
        navigationBar.getNextButton().setOnMouseClicked(e -> {
            FamilyOptionsView familyOptionsView = familyOptionsViews[currentFamilyOptionsViewIndex];
            if (familyOptionsView.isValid()) {
                if (currentFamilyOptionsViewIndex < familyOptionsViews.length - 1)
                    navigateToFamilyOptionsView(currentFamilyOptionsViewIndex + 1);
                else
                    activity.displayCheckoutSlide();
            }
        });
        return container;
    }

    @Override
    public void onWorkingBookingLoaded() {
        //bookWholeEvent();
        navigateToFamilyOptionsView(0);
    }

    private void navigateToFamilyOptionsView(int index) {
        FamilyOptionsView familyOptionsView = familyOptionsViews[index];
        WorkingBookingProperties workingBookingProperties = activity.getWorkingBookingProperties();
        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        familyOptionsView.setWorkingBooking(workingBooking);
        navigationBar.setTitleI18nKey(familyOptionsView.getTitleI18nKey());
        transitionPane.setReverse(index < currentFamilyOptionsViewIndex);
        transitionPane.transitToContent(familyOptionsView.getView());
        currentFamilyOptionsViewIndex = index;
        navigationBar.getBackButton().setDisable(index == 0);
        //navigationBar.getNextButton().setDisable(index == familyOptionsViews.length - 1);
    }

}
