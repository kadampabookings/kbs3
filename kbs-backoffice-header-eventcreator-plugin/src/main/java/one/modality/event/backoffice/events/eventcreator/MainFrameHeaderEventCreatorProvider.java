package one.modality.event.backoffice.events.eventcreator;

import dev.webfx.extras.panes.ColumnsPane;
import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.extras.styles.materialdesign.textfield.MaterialTextField;
import dev.webfx.extras.styles.materialdesign.util.MaterialUtil;
import dev.webfx.extras.time.pickers.DateField;
import dev.webfx.extras.util.layout.LayoutUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Numbers;
import dev.webfx.platform.windowhistory.WindowHistory;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.UpdateStore;
import dev.webfx.stack.ui.controls.button.ButtonFactoryMixin;
import dev.webfx.stack.ui.controls.dialog.DialogBuilder;
import dev.webfx.stack.ui.controls.dialog.DialogBuilderUtil;
import dev.webfx.stack.ui.controls.dialog.SimpleDialogBuilder;
import dev.webfx.stack.ui.dialog.DialogCallback;
import dev.webfx.stack.ui.operation.OperationUtil;
import dev.webfx.stack.ui.validation.ValidationSupport;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import one.modality.base.backoffice.mainframe.fx.FXEventSelector;
import one.modality.base.backoffice.mainframe.headernode.MainFrameHeaderNodeProvider;
import one.modality.base.client.i18n.ModalityI18nKeys;
import one.modality.base.client.mainframe.fx.FXMainFrameDialogArea;
import one.modality.base.shared.entities.*;
import one.modality.crm.backoffice.organization.fx.FXOrganizationId;
import one.modality.event.backoffice.activities.pricing.EventPricingRouting;
import one.modality.event.client.event.fx.FXEventId;
import org.kadampabookings.kbs.client.festivaltypes.FXFestivals;
import org.kadampabookings.kbs.client.festivaltypes.FestivalType;

import java.time.LocalDate;

/**
 * @author Bruno Salmon
 */
public class MainFrameHeaderEventCreatorProvider implements MainFrameHeaderNodeProvider {

    private final Button createHeaderButton = Bootstrap.successButton(I18nControls.newButton(EventCreatorI18nKeys.CreateEvent));
    private ToggleGroup toggleGroup; // Not final because renewing it on each dialog, so old listeners are not triggered anymore

    @Override
    public String getName() {
        return "eventCreator";
    }

    @Override
    public Node getHeaderNode(ButtonFactoryMixin buttonFactory, Pane frameContainer, DataSourceModel dataSourceModel) {
        createHeaderButton.visibleProperty().bind(FXProperties.combine(FXEventSelector.eventSelectorVisibleProperty(), FXOrganizationId.organizationIdProperty(),
            (eventSelectorVisible, organizationId) ->
                !eventSelectorVisible // TODO: replace this with a property that is true when home page is displayed
                && Numbers.identicalObjectsOrNumberValues(Entities.getPrimaryKey(organizationId), 1)) // restricted to NKT only for now (organizationId = 1) as we don't manage events other than NKT Festivals for now
        );
        createHeaderButton.managedProperty().bind(createHeaderButton.visibleProperty());
        // TODO: Use an operation request instead of a direct call to allow authorization checks
        createHeaderButton.setOnAction(e -> openNKTFestivalCreatorDialog());
        return createHeaderButton;
    }

    private void openNKTFestivalCreatorDialog() {
        toggleGroup = new ToggleGroup();
        ToggleButton springButton = createEventTypeButton(EventCreatorI18nKeys.SpringCard, FestivalType.SPRING_FESTIVAL);
        ToggleButton summerButton = createEventTypeButton(EventCreatorI18nKeys.SummerCard, FestivalType.SUMMER_FESTIVAL);
        ToggleButton fallButton = createEventTypeButton(EventCreatorI18nKeys.FallCard, FestivalType.FALL_FESTIVAL);
        HBox eventTypeBar = new HBox(10, springButton, summerButton, fallButton);
        Button cancelButton = Bootstrap.secondaryButton(I18nControls.newButton(ModalityI18nKeys.Cancel));
        LayoutUtil.setMinWidthToPref(cancelButton); // to avoid the button to stretch (will be scaled down instead)
        Button createButton = Bootstrap.largeSuccessButton(I18nControls.newButton(ModalityI18nKeys.Create), false);
        HBox buttonBar = Bootstrap.large(new HBox(10,
            cancelButton,
            createButton
        ));
        buttonBar.setAlignment(Pos.CENTER);
        //TextField eventNameTextField = new TextField();
        DateField startDateField = createDateField(EventCreatorI18nKeys.StartDate);
        DateField endDateField = createDateField(EventCreatorI18nKeys.EndDate);
        VBox mainContent = new VBox(80,
            I18nControls.newLabel(EventCreatorI18nKeys.SelectEventType),
            eventTypeBar,
            /*I18nControls.newLabel(EventCreatorI18nKeys.NameEvent),
            MaterialUtil.makeMaterial(eventNameTextField),*/
            new ColumnsPane(80, startDateField.getView(), endDateField.getView())
        );
        mainContent.setAlignment(Pos.CENTER);
        // Note: we use a different scale pane for the main content and the button bar, so that the button bar is not scaled down immediately
        VBox container = new VBox(
            new ScalePane(mainContent),
            new ScalePane(buttonBar)
        );
        //container.spacingProperty().bind(container.heightProperty().multiply(0.1)); // multiply not yet supported by webfx
        container.spacingProperty().bind(FXProperties.compute(container.heightProperty(), height -> height.doubleValue() * 0.1));
        /*MaterialTextField materialTextField = MaterialUtil.getMaterialTextField(eventNameTextField);
        I18n.bindI18nTextProperty(materialTextField.labelTextProperty(), EventCreatorI18nKeys.NameEventTextField);
        materialTextField.setAnimateLabel(false);*/
        container.getStyleClass().add("event-creator-dialog");
        container.setMaxWidth(700);

        // Pre-computing the most probable Festival start date from the festival type selected by the user
        FXProperties.runOnPropertyChange(() -> {
            FestivalType festivalType = getSelectedFestivalType();
            if (festivalType != null) {
                int year = nextFestivalYear(festivalType);
                startDateField.setDate(festivalType.evaluateStartDate(year));
            }
        }, toggleGroup.selectedToggleProperty());

        // Pre-computing the most probable Festival end date from the Festival start date
        FXProperties.runOnPropertyChange(startDate -> {
            FestivalType festivalType = getSelectedFestivalType();
            if (festivalType != null)
                endDateField.setDate(festivalType.evaluateEndDate(startDate));
        }, startDateField.dateProperty());

        DialogBuilder dialogBuilder = new SimpleDialogBuilder(container);
        DialogCallback dialogCallback = DialogBuilderUtil.showModalNodeInGoldLayout(dialogBuilder, FXMainFrameDialogArea.getDialogArea());
        createHeaderButton.setDisable(true);
        dialogCallback.addCloseHook(() -> createHeaderButton.setDisable(false));
        ValidationSupport validationSupport = new ValidationSupport();
        validationSupport.addRequiredInput(toggleGroup.selectedToggleProperty(), eventTypeBar);
        //validationSupport.addRequiredInput(eventNameTextField);
        cancelButton.setOnAction(e -> dialogCallback.closeDialog());
        createButton.setOnAction(e -> {
            if (validationSupport.isValid()) {
                // Getting the festival type selected by the user (was stored in the toggle button user data)
                FestivalType festivalType = getSelectedFestivalType();
                int year = nextFestivalYear(festivalType);
                // Creating the festival event
                UpdateStore updateStore = UpdateStore.create(DataSourceModelService.getDefaultDataSourceModel());
                Event event = updateStore.insertEntity(Event.class);
                // For now (2025) we do only Online Festivals with KBS3 (in 2026 the same event will be for both in-person & online)
                event.setName(I18n.getI18nText("[{0}] Festival {1} Online", festivalType.getShortI18nKey(), year));
                event.setOrganization(1);
                event.setCorporation(1); // TODO: remove this from database
                event.setType(festivalType.getTypeId());
                event.setStartDate(startDateField.getDate());
                event.setEndDate(endDateField.getDate());
                event.setKbs3(true);
                event.setTeachingsDayTicket(true);
                event.setAudioRecordingsDayTicket(true);
                // Main site
                Site site = updateStore.insertEntity(Site.class);
                site.setName("Online");
                site.setEvent(event);
                site.setOrganization(1);
                site.setItemFamily(KnownItemFamily.TEACHING.getPrimaryKey());
                site.setMain(true);
                site.setOrd(10);
                //event.setVenue(site); // cyclic reference issue => postponed below
                // Bookable scheduled items
                int festivalItemPrimaryKey = FestivalType.getFestivalItemPrimaryKey();
                for (LocalDate date = event.getStartDate(); !date.isAfter(event.getEndDate()) ; date = date.plusDays(1)) {
                    ScheduledItem si = updateStore.insertEntity(ScheduledItem.class);
                    si.setEvent(event);
                    si.setSite(site);
                    si.setItem(festivalItemPrimaryKey);
                    si.setDate(date);
                }
                Rate rate = updateStore.insertEntity(Rate.class);
                rate.setSite(site);
                rate.setItem(festivalItemPrimaryKey);
                rate.setPerDay(true);
                rate.setPrice(0);
                OperationUtil.turnOnButtonsWaitModeDuringExecution(
                    updateStore.submitChanges()
                        // Setting venue afterwards TODO: Improve EntityChangesToSubmitBatchGenerator to solve cyclic references
                        .compose(ignored -> {
                            event.setVenue(site);
                            return updateStore.submitChanges();
                        })
                        .onComplete(Console::log)
                        .onSuccess(ignored -> {
                            // Automatically selecting this new event
                            EntityId eventId = event.getId();
                            FXEventId.setEventId(eventId);
                            // Automatically routing to the pricing activity
                            new EventPricingRouting.RouteToEventPricingRequest(eventId, WindowHistory.getProvider()).execute();
                            // Closing the dialog
                            dialogCallback.closeDialog();
                        })
                    ,
                    createButton, cancelButton
                );
            }
        });
    }

    private FestivalType getSelectedFestivalType() {
        Toggle selectedToggle = toggleGroup.getSelectedToggle();
        return selectedToggle == null ? null : (FestivalType) selectedToggle.getUserData();
    }

    private ToggleButton createEventTypeButton(String cardI18nKey, FestivalType festivalType) {
        // card18nKey has a graphic and a text such as "[Spring] {0}" where {0} is supposed to be the year of the next festival to create
        ObservableValue<Integer> nextYearProperty = FXProperties.compute(FXFestivals.lastFestivalProperty(festivalType),
            MainFrameHeaderEventCreatorProvider::nextFestivalYear);
        ToggleButton button = I18nControls.newToggleButton(cardI18nKey, nextYearProperty);
        button.setContentDisplay(ContentDisplay.TOP);
        button.setGraphicTextGap(20);
        button.setMinSize(277, 173);
        button.setToggleGroup(toggleGroup);
        button.getStyleClass().add(festivalType.getStyleClass());
        button.setUserData(festivalType);
        return button;
    }

    // Static methods

    private static Integer nextFestivalYear(FestivalType festivalType) {
        return FXFestivals.lastFestivalProperty(festivalType).getValue().getStartDate().getYear() + 1;
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
        /*dateField.getView().setBorder(BorderFactory.newBorder(Color.GRAY, 10));
        LayoutUtil.setPadding(dateField.getView(), 10);*/
        return dateField;
    }
}
