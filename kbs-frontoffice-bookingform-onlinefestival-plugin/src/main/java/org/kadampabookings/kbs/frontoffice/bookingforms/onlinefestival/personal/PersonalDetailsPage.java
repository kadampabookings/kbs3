package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.personal;

import dev.webfx.extras.i18n.I18n;
import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.operation.OperationUtil;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.extras.util.layout.Layouts;
import dev.webfx.extras.validation.ValidationSupport;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.password.PasswordI18nKeys;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.orm.entity.UpdateStore;
import dev.webfx.stack.orm.entity.binding.EntityBindings;
import dev.webfx.stack.orm.entity.controls.entity.selector.EntityButtonSelector;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import one.modality.base.shared.entities.Country;
import one.modality.base.shared.entities.Person;
import one.modality.crm.client.i18n.CrmI18nKeys;
import one.modality.crm.frontoffice.activities.userprofile.UserProfileI18nKeys;
import one.modality.crm.frontoffice.activities.userprofile.UserProfileView;
import one.modality.crm.shared.services.authn.fx.FXUserPerson;
import one.modality.ecommerce.client.workingbooking.FXPersonToBook;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.ecommerce.frontoffice.bookingelements.BookingElements;
import one.modality.ecommerce.frontoffice.bookingform.multipages.BookingFormPage;

/**
 * @author Bruno Salmon
 * @author David Hello
 */
public final class PersonalDetailsPage implements BookingFormPage {

    private final MonoPane embeddedLoginContainer = new MonoPane();
    private final EntityButtonSelector<Person> personToBookSelector = BookingElements.createPersonToBookSelector(false);
    private final Button personToBookButton = personToBookSelector.getButton();
    private final VBox personalDetailsVBox = new VBox(10,
        Bootstrap.strong(I18n.newText(CrmI18nKeys.PersonToBook)),
        personToBookButton
    );
    private final VBox container = BookingElements.createPageVBox("personal-details", false,
        embeddedLoginContainer,
        personalDetailsVBox
    );
    private final UserProfileView userProfileView;
    private UpdateStore updateStore;
    private Person currentPerson;
    private Person currentUser;

    private Button cancelButton;
    private final ValidationSupport validationSupport = new ValidationSupport();
    private BooleanProperty isItANewPersonProperty = new SimpleBooleanProperty();


    public PersonalDetailsPage() {
        personalDetailsVBox.setMaxWidth(400);
        // personalDetailsVBox is not visible when login is showing, and vice versa
        Layouts.bindManagedAndVisiblePropertiesTo(embeddedLoginContainer.visibleProperty().not(), personalDetailsVBox);
        // We want to show only the email, address and kadampa center info
        userProfileView = new UserProfileView(null, false, false, true,true, false, false, true, true, true);
        Node viewNode = userProfileView.buildView();
        userProfileView.setChangeEmailLinkVisible(false);
        userProfileView.setEmailFieldDisabled(false);
        userProfileView.infoMessage.setVisible(false);
        userProfileView.firstNameTextField.visibleProperty().bind(isItANewPersonProperty);
        userProfileView.lastNameTextField.visibleProperty().bind(isItANewPersonProperty);
        userProfileView.firstNameTextField.managedProperty().bind(isItANewPersonProperty);
        userProfileView.lastNameTextField.managedProperty().bind(isItANewPersonProperty);
        FXProperties.runOnPropertiesChange(()->{Console.log("new Person: " + isItANewPersonProperty.get());},isItANewPersonProperty);
        cancelButton = Bootstrap.largeSecondaryButton(I18nControls.newButton(UserProfileI18nKeys.Cancel));
        cancelButton.setOnAction(e-> {
            updateStore.cancelChanges();
            if(isItANewPersonProperty.get()) {
                FXPersonToBook.personToBookProperty().set(FXUserPerson.getUserPerson());
            }
            syncUIFromModel(currentPerson);
        });
        cancelButton.disableProperty().bind(userProfileView.saveButton.disableProperty());
        personalDetailsVBox.setAlignment(Pos.TOP_CENTER);
        personalDetailsVBox.getChildren().addAll(viewNode,cancelButton);
        FXProperties.runNowAndOnPropertyChange(person -> {
            if (person != null) {
                userProfileView.setLoginDetailsVisible(!Entities.samePrimaryKey(FXPersonToBook.getPersonToBook(), FXUserPerson.getUserPerson()));
                syncUIFromModel(person);
            }
            if(currentPerson!=null && person == null) {
                Console.log("someone else selected");
                syncUIFromModel(null);
            }
        }, FXPersonToBook.personToBookProperty());

        userProfileView.firstNameTextField.textProperty().addListener((observable, oldValue, newValue) -> currentPerson.setFirstName(newValue));
        userProfileView.lastNameTextField.textProperty().addListener((observable, oldValue, newValue) -> currentPerson.setLastName(newValue));
        userProfileView.emailTextField.textProperty().addListener((observable, oldValue, newValue) -> currentPerson.setEmail(newValue));
        userProfileView.layNameTextField.textProperty().addListener((observable, oldValue, newValue) -> currentPerson.setLayName(newValue));
        userProfileView.phoneTextField.textProperty().addListener((observable, oldValue, newValue) -> currentPerson.setPhone(newValue));
        userProfileView.postCodeTextField.textProperty().addListener((observable, oldValue, newValue) -> currentPerson.setPostCode(newValue));
        userProfileView.cityNameTextField.textProperty().addListener((observable, oldValue, newValue) -> currentPerson.setCityName(newValue));
        userProfileView.countrySelector.selectedItemProperty().addListener((observable, oldValue, newValue) -> currentPerson.setCountry(newValue));
        userProfileView.organizationSelector.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            currentPerson.setOrganization(newValue);
            if (newValue != null) userProfileView.noOrganizationRadioButton.setSelected(false);
        });
        userProfileView.noOrganizationRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                userProfileView.organizationSelector.setSelectedItem(null);
            }
        });
        //If there are some changes, we forbid to switch to another user
        personToBookButton.disableProperty().bind(userProfileView.saveButton.disableProperty().not());
    }

    private void syncUIFromModel(Person person) {
        if(person!=null) {
            updateStore = UpdateStore.createAbove(person.getStore());
            currentPerson = updateStore.updateEntity(person);
            isItANewPersonProperty.setValue(false);
        } else {
            //Here the update store should have already been initialised
            currentPerson = updateStore.insertEntity(Person.class);
            currentPerson.setFrontendAccount(FXUserPerson.getUserPerson().getFrontendAccount());
            person = currentPerson;
            isItANewPersonProperty.setValue(true);
        }
        userProfileView.emailTextField.setText(person.getEmail());
        userProfileView.postCodeTextField.setText(person.getPostCode());
        userProfileView.cityNameTextField.setText(person.getCityName());
        EntityButtonSelector<Country> countrySelector = userProfileView.countrySelector;
        countrySelector.setSelectedItem(person.getCountry());
        userProfileView.organizationSelector.setSelectedItem(person.getOrganization());
        userProfileView.noOrganizationRadioButton.setSelected(person.getOrganization() == null);

        userProfileView.saveButton.disableProperty().bind(EntityBindings.hasChangesProperty(updateStore).not());

        userProfileView.saveButton.setOnAction(e -> {
            if (validateForm()) {
                OperationUtil.turnOnButtonsWaitModeDuringExecution(
                    updateStore.submitChanges().
                        onFailure(failure -> {
                            Console.log("Error while updating account:" + failure);
                            Platform.runLater(() -> {
                                userProfileView.infoMessage.setVisible(true);
                                Bootstrap.textDanger(I18nControls.bindI18nProperties(userProfileView.infoMessage, UserProfileI18nKeys.ErrorWhileUpdatingPersonalInformation));
                            });
                        })
                        .onSuccess(success -> {
                            Console.log("Account updated with success");
                            Platform.runLater(() -> {
                                userProfileView.infoMessage.setVisible(true);
                                Bootstrap.textSuccess(I18nControls.bindI18nProperties(userProfileView.infoMessage, UserProfileI18nKeys.PersonalInformationUpdated));
                                if(isItANewPersonProperty.get()) {
                                    personToBookSelector.refreshWhenActive();
                                    FXPersonToBook.setPersonToBook(currentPerson);
                                }
                                UiScheduler.scheduleDelay(5000,()-> {
                                    userProfileView.infoMessage.setVisible(false);
                                });
                            });
                        })
                    ,  userProfileView.saveButton);
            }
        });

        userProfileView.saveButton.disableProperty().bind(EntityBindings.hasChangesProperty(updateStore).not());
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
        }
    }

    @Override
    public MonoPane getEmbeddedLoginContainer() {
        return embeddedLoginContainer;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
        //personalDetailsVBox.setDisable(!workingBookingProperties.getWorkingBooking().isNewBooking());
    }

    @Override
    public ObservableBooleanValue validProperty() {
        return userProfileView.saveButton.disableProperty();
    }
}
