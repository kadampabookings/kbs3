package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.personal;

import dev.webfx.extras.i18n.I18n;
import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.operation.OperationUtil;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.extras.util.control.Controls;
import dev.webfx.extras.util.layout.Layouts;
import dev.webfx.extras.validation.ValidationSupport;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.password.PasswordI18nKeys;
import dev.webfx.stack.authn.logout.client.operation.LogoutRequest;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.orm.entity.UpdateStore;
import dev.webfx.stack.orm.entity.binding.EntityBindings;
import dev.webfx.stack.orm.entity.controls.entity.selector.EntityButtonSelector;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.modality.base.shared.entities.Event;
import one.modality.base.shared.entities.FrontendAccount;
import one.modality.base.shared.entities.Organization;
import one.modality.base.shared.entities.Person;
import one.modality.crm.client.i18n.CrmI18nKeys;
import one.modality.crm.frontoffice.activities.userprofile.UserProfileI18nKeys;
import one.modality.crm.frontoffice.activities.userprofile.UserProfileView;
import one.modality.crm.shared.services.authn.fx.FXUserPerson;
import one.modality.ecommerce.client.workingbooking.FXPersonToBook;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.ecommerce.document.service.DocumentService;
import one.modality.ecommerce.frontoffice.bookingelements.BookingElements;
import one.modality.ecommerce.frontoffice.bookingform.BookingForm;
import one.modality.ecommerce.frontoffice.bookingform.multipages.BookingFormPage;
import one.modality.ecommerce.frontoffice.order.OrderActions;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.OnlineFestivalI18nKeys;

/**
 * @author Bruno Salmon
 * @author David Hello
 */
public final class PersonalDetailsPage implements BookingFormPage {

    private final Event event;
    private final MonoPane embeddedLoginContainer = new MonoPane();
    private final EntityButtonSelector<Person> personToBookSelector = BookingElements.createPersonToBookSelector(false);
    private final Button personToBookButton = personToBookSelector.getButton();
    private final Label alreadyBookedLabel = Bootstrap.textDanger(new Label());
    private final Hyperlink modifyBookingLink = Bootstrap.textPrimary(new Hyperlink());
    private final MonoPane modifyBookingPane = centerInVBoxWithMargin(modifyBookingLink, new Insets(30, 0, 50, 0));
    private final VBox personalDetailsVBox = new VBox(
        Bootstrap.strong(I18n.newText(CrmI18nKeys.PersonToBook)),
        personToBookButton,
        alreadyBookedLabel,
        modifyBookingPane
    );
    private final VBox container = BookingElements.createFormPageVBox(false,
        embeddedLoginContainer,
        personalDetailsVBox
    );
    private final UserProfileView userProfileView;
    private UpdateStore updateStore;
    private Person personToBook;
    private boolean isNewPerson;
    private boolean syncing;
    private final ObjectProperty<Future<?>> busyFutureProperty = new SimpleObjectProperty<>();
    private final BooleanProperty alreadyBookedProperty = new SimpleBooleanProperty();

    private final ValidationSupport validationSupport = new ValidationSupport();

    public PersonalDetailsPage(BookingForm bookingForm) {
        event = ((EventBookingFormSettings) bookingForm.getSettings()).event();
        personalDetailsVBox.setMaxWidth(450);
        personToBookButton.setMaxWidth(Double.MAX_VALUE);
        // personalDetailsVBox is not visible when login is showing, and vice versa
        Layouts.bindManagedAndVisiblePropertiesTo(embeddedLoginContainer.visibleProperty().not(), personalDetailsVBox);
        // We want to show only the email, address and kadampa center info
        userProfileView = new UserProfileView(null, false, false, true, true, false, false, true, true, true);
        Node viewNode = userProfileView.buildView();
        userProfileView.setChangeEmailLinkVisible(false);
        userProfileView.setEmailFieldDisabled(false);
        userProfileView.infoMessage.setVisible(false);
        Button cancelButton = Bootstrap.largeSecondaryButton(I18nControls.newButton(UserProfileI18nKeys.Cancel));
        cancelButton.disableProperty().bind(userProfileView.saveButton.disableProperty());
        cancelButton.visibleProperty().bind(userProfileView.saveButton.visibleProperty());
        personalDetailsVBox.setAlignment(Pos.TOP_LEFT);
        personalDetailsVBox.getChildren().addAll(viewNode, centerInVBoxWithMargin(cancelButton, new Insets(10, 0, 0, 0)));
        Controls.setupTextWrapping(alreadyBookedLabel, true, false);
        Layouts.bindAllManagedAndVisiblePropertiesTo(alreadyBookedProperty, alreadyBookedLabel, modifyBookingPane);

        FXProperties.runNowAndOnPropertyChange(person -> {
            boolean isAccountOwner = Entities.samePrimaryKey(person, FXUserPerson.getUserPerson());
            boolean isLinkedAccount = false;
            if (person != null) {
                // Forcing logout for security staff if they try to book with that account
                FrontendAccount userAccount = person.getFrontendAccount(); // Note: null (not loaded) for members
                if (userAccount != null && userAccount.isSecurity())
                    OperationUtil.executeOperation(new LogoutRequest());
                else {
                    isLinkedAccount = Entities.getPrimaryKey(person.getAccountPersonId()) != null;
                    setPersonToBook(person);
                }
            } else if (personToBook != null) {
                setPersonToBook(null);
            }
            userProfileView.setLoginDetailsVisible(!isLinkedAccount);
            userProfileView.setEmailFieldDisabled(isAccountOwner);
            userProfileView.setAddressInfoVisible(!isLinkedAccount);
            userProfileView.setKadampaCenterVisible(!isLinkedAccount);
            userProfileView.saveButton.setVisible(!isLinkedAccount);
        }, FXPersonToBook.personToBookProperty());

        FXProperties.runOnPropertiesChange(this::syncModelFromUI,
            userProfileView.firstNameTextField.textProperty(),
            userProfileView.lastNameTextField.textProperty(),
            userProfileView.emailTextField.textProperty(),
            userProfileView.layNameTextField.textProperty(),
            userProfileView.phoneTextField.textProperty(),
            userProfileView.streetTextField.textProperty(),
            userProfileView.postCodeTextField.textProperty(),
            userProfileView.cityNameTextField.textProperty(),
            userProfileView.countrySelector.selectedItemProperty(),
            userProfileView.organizationSelector.selectedItemProperty(),
            userProfileView.noOrganizationRadioButton.selectedProperty()
        );

        // If there are some changes, we forbid to switch to another user
        personToBookButton.disableProperty().bind(userProfileView.saveButton.disableProperty().not());

        cancelButton.setOnAction(e -> {
            if (isNewPerson)
                FXPersonToBook.setPersonToBook(FXUserPerson.getUserPerson());
            setPersonToBook(personToBook);
        });
        userProfileView.saveButton.setOnAction(e -> {
            if (validateForm()) {
                OperationUtil.turnOnButtonsWaitModeDuringExecution(
                    updateStore.submitChanges()
                        .inUiThread()
                        .onFailure(failure -> {
                            Console.log("Error while updating account:" + failure);
                            userProfileView.infoMessage.setVisible(true);
                            Bootstrap.textDanger(I18nControls.bindI18nProperties(userProfileView.infoMessage, UserProfileI18nKeys.ErrorWhileUpdatingPersonalInformation));
                        })
                        .onSuccess(success -> {
                            Console.log("Account updated with success");
                            userProfileView.infoMessage.setVisible(true);
                            Bootstrap.textSuccess(I18nControls.bindI18nProperties(userProfileView.infoMessage, UserProfileI18nKeys.PersonalInformationUpdated));
                            if (isNewPerson) {
                                personToBookSelector.refreshWhenActive();
                                FXPersonToBook.setPersonToBook(personToBook);
                            }
                            UiScheduler.scheduleDelay(5000, () -> userProfileView.infoMessage.setVisible(false));
                        })
                    , userProfileView.saveButton);
            }
        });
    }

    private void setPersonToBook(Person person) {
        if (updateStore != null)
            updateStore.cancelChanges();
        if (person != null) {
            if (updateStore == null) {
                updateStore = UpdateStore.createAbove(person.getStore());
                userProfileView.saveButton.disableProperty().bind(EntityBindings.hasChangesProperty(updateStore).not());
            }
            personToBook = updateStore.updateEntity(person);
            isNewPerson = false;
            busyFutureProperty.set(DocumentService.loadDocument(event, person)
                .inUiThread()
                .onSuccess(documentAggregate -> {
                    alreadyBookedProperty.set(documentAggregate != null);
                    if (documentAggregate != null) {
                        I18nControls.bindI18nProperties(alreadyBookedLabel, OnlineFestivalI18nKeys.PersonAlreadyBooked1, person.getFullName());
                        I18nControls.bindI18nProperties(modifyBookingLink, OnlineFestivalI18nKeys.ModifyBooking1, documentAggregate.getDocumentRef());
                        OrderActions.setupModifyOrderButton(modifyBookingLink, documentAggregate.getDocumentPrimaryKey());
                    }
                }));
        } else if (updateStore != null) { // Should be always true because the account owner was always selected first
            // Here the update store should have already been initialized
            personToBook = updateStore.insertEntity(Person.class);
            isNewPerson = true;
            alreadyBookedProperty.set(false);
            FXProperties.onPropertySet(FXUserPerson.userPersonProperty(), p -> personToBook.setFrontendAccount(p.getFrontendAccount()));
        }
        syncUIFromModel();
    }

    private void syncModelFromUI() {
        if (syncing)
            return;
        syncing = true;
        personToBook.setFirstName(userProfileView.firstNameTextField.getText());
        personToBook.setLastName(userProfileView.lastNameTextField.getText());
        personToBook.setEmail(userProfileView.emailTextField.getText());
        personToBook.setLayName(userProfileView.layNameTextField.getText());
        personToBook.setPhone(userProfileView.phoneTextField.getText());
        personToBook.setStreet(userProfileView.streetTextField.getText());
        personToBook.setPostCode(userProfileView.postCodeTextField.getText());
        personToBook.setCityName(userProfileView.cityNameTextField.getText());
        personToBook.setCountry(userProfileView.countrySelector.getSelectedItem());
        Organization organization = userProfileView.organizationSelector.getSelectedItem();
        boolean noOrganization = userProfileView.noOrganizationRadioButton.isSelected()
                                 && (organization == null || Entities.sameId(organization, personToBook.getOrganization()));
        if (noOrganization) {
            organization = null;
        }
        personToBook.setOrganization(organization);
        userProfileView.organizationSelector.setSelectedItem(organization);
        userProfileView.noOrganizationRadioButton.setSelected(organization == null);
        syncing = false;
    }

    private void syncUIFromModel() {
        if (syncing)
            return;
        syncing = true;
        userProfileView.firstNameTextField.setText(personToBook.getFirstName());
        userProfileView.lastNameTextField.setText(personToBook.getLastName());
        userProfileView.emailTextField.setText(personToBook.getEmail());
        userProfileView.layNameTextField.setText(personToBook.getLayName());
        userProfileView.phoneTextField.setText(personToBook.getPhone());
        userProfileView.streetTextField.setText(personToBook.getStreet());
        userProfileView.postCodeTextField.setText(personToBook.getPostCode());
        userProfileView.cityNameTextField.setText(personToBook.getCityName());
        userProfileView.countrySelector.setSelectedItem(personToBook.getCountry());
        userProfileView.organizationSelector.setSelectedItem(personToBook.getOrganization());
        userProfileView.noOrganizationRadioButton.setSelected(personToBook.getOrganization() == null);

        Layouts.setManagedAndVisibleProperties(userProfileView.firstNameTextField, isNewPerson);
        Layouts.setManagedAndVisibleProperties(userProfileView.lastNameTextField, isNewPerson);
        syncing = false;
    }

    @Override
    public Object getTitleI18nKey() {
        return CrmI18nKeys.PersonalDetails;
    }

    @Override
    public Node getView() {
        return container;
    }

    @Override
    public boolean isApplicableToBooking(WorkingBooking workingBooking) {
        return workingBooking.isNewBooking();
    }

    public boolean validateForm() {
        initFormValidation();
        return validationSupport.isValid();
    }

    private void initFormValidation() {
        if (validationSupport.isEmpty()) {
            validationSupport.addEmailValidation(userProfileView.emailTextField, userProfileView.emailTextField, I18n.i18nTextProperty(PasswordI18nKeys.InvalidEmail));
            validationSupport.addRequiredInput(userProfileView.firstNameTextField);
            validationSupport.addRequiredInput(userProfileView.lastNameTextField);
            validationSupport.addRequiredInput(userProfileView.streetTextField);
            validationSupport.addRequiredInput(userProfileView.postCodeTextField);
            validationSupport.addRequiredInput(userProfileView.cityNameTextField);
            validationSupport.addRequiredInput(userProfileView.countrySelector.selectedItemProperty(), userProfileView.countrySelector.getButton());
        }
    }

    @Override
    public MonoPane getEmbeddedLoginContainer() {
        return embeddedLoginContainer;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {

    }

    @Override
    public ObservableBooleanValue validProperty() {
        return userProfileView.saveButton.disableProperty().and(alreadyBookedProperty.not());
    }

    @Override
    public ObservableObjectValue<Future<?>> busyFutureProperty() {
        return busyFutureProperty;
    }

    private static MonoPane centerInVBoxWithMargin(Node node, Insets margin) {
        MonoPane monoPane = new MonoPane(node);
        monoPane.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(monoPane, margin);
        return monoPane;
    }
}
