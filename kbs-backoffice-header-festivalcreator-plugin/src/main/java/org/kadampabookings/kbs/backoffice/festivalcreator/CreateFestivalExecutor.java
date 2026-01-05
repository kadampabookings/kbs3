package org.kadampabookings.kbs.backoffice.festivalcreator;

import dev.webfx.extras.aria.AriaToggleGroup;
import dev.webfx.extras.async.AsyncSpinner;
import dev.webfx.extras.exceptions.UserCancellationException;
import dev.webfx.extras.i18n.I18n;
import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.panes.ColumnsPane;
import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.extras.styles.materialdesign.textfield.MaterialTextField;
import dev.webfx.extras.styles.materialdesign.util.MaterialUtil;
import dev.webfx.extras.time.pickers.DateField;
import dev.webfx.extras.util.dialog.DialogCallback;
import dev.webfx.extras.util.dialog.builder.DialogBuilder;
import dev.webfx.extras.util.dialog.builder.DialogBuilderUtil;
import dev.webfx.extras.util.dialog.builder.SimpleDialogBuilder;
import dev.webfx.extras.util.layout.Layouts;
import dev.webfx.extras.validation.ValidationSupport;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.stack.orm.entity.UpdateStore;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import one.modality.base.client.i18n.BaseI18nKeys;
import one.modality.base.client.mainframe.fx.FXMainFrameDialogArea;
import one.modality.base.shared.entities.Event;
import one.modality.base.shared.entities.Site;
import one.modality.event.backoffice.eventcreator.EventCreator;
import org.kadampabookings.kbs.client.festivaltypes.FXFestivals;
import org.kadampabookings.kbs.client.festivaltypes.FestivalType;

/**
 * @author Bruno Salmon
 */
final class CreateFestivalExecutor {

    private final AriaToggleGroup<FestivalType> toggleGroup = new AriaToggleGroup<>();

    Future<Void> openNKTFestivalCreatorDialog() {
        Promise<Void> promise = Promise.promise();
        toggleGroup.clear();
        ToggleButton springButton = createEventTypeButton(FestivalCreatorI18nKeys.SpringCard, FestivalType.SPRING_FESTIVAL);
        ToggleButton summerButton = createEventTypeButton(FestivalCreatorI18nKeys.SummerCard, FestivalType.SUMMER_FESTIVAL);
        ToggleButton fallButton = createEventTypeButton(FestivalCreatorI18nKeys.FallCard, FestivalType.FALL_FESTIVAL);
        HBox eventTypeBar = new HBox(10, springButton, summerButton, fallButton);
        Button cancelButton = Bootstrap.secondaryButton(I18nControls.newButton(BaseI18nKeys.Cancel));
        Layouts.setMinWidthToPref(cancelButton); // to avoid the button to stretch (will be scaled down instead)
        Button createButton = Bootstrap.largeSuccessButton(I18nControls.newButton(BaseI18nKeys.Create), false);
        HBox buttonBar = Bootstrap.large(new HBox(10,
            cancelButton,
            createButton
        ));
        buttonBar.setAlignment(Pos.CENTER);
        DateField startDateField = createDateField(BaseI18nKeys.StartDate);
        DateField endDateField = createDateField(BaseI18nKeys.EndDate);
        VBox mainContent = new VBox(80,
            I18nControls.newLabel(FestivalCreatorI18nKeys.SelectFestivalType),
            eventTypeBar,
            new ColumnsPane(80, startDateField.getView(), endDateField.getView())
        );
        mainContent.setAlignment(Pos.CENTER);
        // Note: we use a different scale pane for the main content and the button bar, so that the button bar is not scaled down immediately
        VBox container = new VBox(
            new ScalePane(mainContent),
            new ScalePane(buttonBar)
        );
        container.spacingProperty().bind(container.heightProperty().multiply(0.1)); // multiply not yet supported by webfx
        container.getStyleClass().add("event-creator-dialog");
        container.setMaxWidth(700);

        // Pre-computing the most probable Festival start date from the festival type selected by the user
        FXProperties.runOnPropertyChange(() -> {
            FestivalType festivalType = getSelectedFestivalType();
            if (festivalType != null) {
                int year = nextFestivalYear(festivalType);
                startDateField.setDate(festivalType.evaluateStartDate(year));
            }
        }, toggleGroup.firedItemProperty());

        // Pre-computing the most probable Festival end date from the Festival start date
        FXProperties.runOnPropertyChange(startDate -> {
            FestivalType festivalType = getSelectedFestivalType();
            if (festivalType != null)
                endDateField.setDate(festivalType.evaluateEndDate(startDate));
        }, startDateField.dateProperty());

        DialogBuilder dialogBuilder = new SimpleDialogBuilder(container);
        DialogCallback dialogCallback = DialogBuilderUtil.showModalNodeInGoldLayout(dialogBuilder, FXMainFrameDialogArea.getDialogArea());
        // Adding a close hook to the dialog callback to fail the promise when the user cancels the dialog
        dialogCallback.addCloseHook(() -> promise.tryFail(new UserCancellationException())); // do nothing if the promise is already completed
        ValidationSupport validationSupport = new ValidationSupport();
        validationSupport.addRequiredInput(toggleGroup.firedToggleButtonProperty(), eventTypeBar);
        //validationSupport.addRequiredInput(eventNameTextField);
        cancelButton.setOnAction(e -> dialogCallback.closeDialog());
        createButton.setOnAction(e -> {
            if (validationSupport.isValid()) {
                FestivalType festivalType = getSelectedFestivalType();
                int year = startDateField.getDate().getYear();
                // For now (2025) we do only Online Festivals with KBS3 (in 2026 the same event will be for both in-person & online)
                String eventName = I18n.getI18nText("[{0}] Festival {1} Online", festivalType.getShortI18nKey(), year);
                UpdateStore updateStore = UpdateStore.create();
                Site venue = EventCreator.insertNewVenue("Online", true, updateStore);
                AsyncSpinner.displayButtonSpinnerDuringAsyncExecution(
                    EventCreator.createEvent(eventName, festivalType.getTypeId(), venue, startDateField.getDate(), endDateField.getDate(), updateStore),
                    createButton, cancelButton
                );
            }
        });

        return promise.future();
    }

    private FestivalType getSelectedFestivalType() {
        return toggleGroup.getFiredItem();
    }

    private ToggleButton createEventTypeButton(Object cardI18nKey, FestivalType festivalType) {
        // card18nKey has a graphic and a text such as "[Spring] {0}" where {0} is supposed to be the year of the next festival to create
        ObservableValue<Integer> nextYearProperty = FXFestivals.lastFestivalProperty(festivalType, false).map(CreateFestivalExecutor::nextFestivalYear);
        ToggleButton button = I18nControls.bindI18nProperties(toggleGroup.createItemButton(festivalType), cardI18nKey, nextYearProperty);
        button.setContentDisplay(ContentDisplay.TOP);
        button.setGraphicTextGap(20);
        button.setMinSize(277, 173);
        button.getStyleClass().add(festivalType.getStyleClass());
        return button;
    }

    // Static methods

    private static Integer nextFestivalYear(FestivalType festivalType) {
        return FXFestivals.lastFestivalProperty(festivalType, false).getValue().getStartDate().getYear() + 1;
    }

    private static Integer nextFestivalYear(Event lastFestival) {
        return lastFestival == null ? null : lastFestival.getStartDate().getYear() + 1;
    }

    private static DateField createDateField(Object i18nKey) {
        DateField dateField = new DateField(FXMainFrameDialogArea.getDialogArea());
        MaterialUtil.makeMaterial(dateField.getTextField());
        MaterialTextField startDateMaterialTextField = MaterialUtil.getMaterialTextField(dateField.getTextField());
        I18n.bindI18nTextProperty(startDateMaterialTextField.labelTextProperty(), i18nKey);
        startDateMaterialTextField.setAnimateLabel(false);
        return dateField;
    }

}
