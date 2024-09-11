package dev.webfx.platform.boot.j2cl;

import dev.webfx.platform.reflect.RArray;
import dev.webfx.platform.boot.ApplicationBooter;
import dev.webfx.platform.boot.spi.ApplicationBooterProvider;
import org.treblereel.j2cl.processors.annotations.GWT3EntryPoint;

import static dev.webfx.platform.service.gwtj2cl.ServiceRegistry.*;

public final class J2clEntryPoint implements ApplicationBooterProvider {

    @GWT3EntryPoint
    public void entryPoint() {
        registerArrayConstructors();
        registerServiceProviders();
        ApplicationBooter.start(this, null);
    }

    public static void registerArrayConstructors() {
        RArray.register(dev.webfx.stack.db.query.QueryResult.class, dev.webfx.stack.db.query.QueryResult[]::new);
        RArray.register(dev.webfx.stack.db.submit.SubmitResult.class, dev.webfx.stack.db.submit.SubmitResult[]::new);
    }

    public static void registerServiceProviders() {
        register(dev.webfx.extras.time.format.spi.TimeFormatProvider.class, dev.webfx.stack.i18n.time.I18nTimeFormatProvider::new);
        register(dev.webfx.kit.launcher.spi.WebFxKitLauncherProvider.class, dev.webfx.kit.launcher.spi.impl.gwtj2cl.GwtJ2clWebFxKitLauncherProvider::new);
        register(dev.webfx.kit.mapper.peers.javafxmedia.spi.WebFxKitMediaMapperProvider.class, dev.webfx.kit.mapper.peers.javafxmedia.spi.gwtj2cl.GwtJ2clWebFxKitMediaMapperProvider::new);
        register(dev.webfx.kit.mapper.spi.WebFxKitMapperProvider.class, dev.webfx.kit.mapper.spi.impl.gwtj2cl.GwtJ2clWebFxKitHtmlMapperProvider::new);
        register(dev.webfx.platform.ast.spi.factory.AstFactoryProvider.class, dev.webfx.platform.ast.spi.factory.impl.generic.GenericAstFactoryProvider::new);
        register(dev.webfx.platform.ast.spi.formatter.AstFormatterProvider.class, dev.webfx.platform.ast.json.formatter.JsonFormatterProvider::new);
        register(dev.webfx.platform.ast.spi.parser.AstParserProvider.class, dev.webfx.platform.ast.json.parser.JsonParserProvider::new);
        register(dev.webfx.platform.blob.spi.BlobProvider.class, dev.webfx.platform.blob.spi.impl.gwtj2cl.GwtJ2clBlobProvider::new);
        register(dev.webfx.platform.boot.spi.ApplicationJob.class, dev.webfx.stack.orm.dql.query.interceptor.DqlQueryInterceptorInitializer::new, dev.webfx.stack.orm.dql.querypush.interceptor.DqlQueryPushInterceptorInitializer::new, dev.webfx.stack.orm.dql.submit.interceptor.DqlSubmitInterceptorInitializer::new, one.modality.base.client.entities.util.functions.ClientFunctionsRegisteringApplicationJob::new, one.modality.crm.client.profile.ModalityClientProfileInitJob::new);
        register(dev.webfx.platform.boot.spi.ApplicationModuleBooter.class, dev.webfx.kit.launcher.WebFxKitLauncherModuleBooter::new, dev.webfx.platform.boot.spi.impl.ApplicationJobsInitializer::new, dev.webfx.platform.boot.spi.impl.ApplicationJobsStarter::new, dev.webfx.platform.resource.spi.impl.j2cl.J2clResourceModuleBooter::new, dev.webfx.stack.com.bus.call.BusCallModuleBooter::new, dev.webfx.stack.com.bus.spi.impl.json.client.JsonClientBusModuleBooter::new, dev.webfx.stack.com.serial.SerialCodecModuleBooter::new, dev.webfx.stack.db.querypush.client.simple.SimpleQueryPushClientJob::new, dev.webfx.stack.ui.fxraiser.json.JsonFXRaiserModuleBooter::new, one.modality.base.client.operationactionsloading.ModalityClientOperationActionsLoader::new, one.modality.crm.client.services.authz.ModalityAuthorizationClientModuleBooter::new);
        register(dev.webfx.platform.browser.spi.BrowserProvider.class, dev.webfx.platform.browser.spi.impl.gwtj2cl.GwtJ2clBrowserProvider::new);
        register(dev.webfx.platform.console.spi.ConsoleProvider.class, dev.webfx.platform.console.spi.impl.gwtj2cl.GwtJ2clConsoleProvider::new);
        register(dev.webfx.platform.fetch.spi.FetchProvider.class, dev.webfx.platform.fetch.spi.impl.gwtj2cl.GwtJ2clFetchProvider::new);
        register(dev.webfx.platform.file.spi.FileProvider.class, dev.webfx.platform.file.spi.impl.gwtj2cl.GwtJ2clFileProvider::new);
        register(dev.webfx.platform.os.spi.OperatingSystemProvider.class, dev.webfx.platform.os.spi.impl.gwtj2cl.GwtJ2clOperatingSystemProvider::new);
        register(dev.webfx.platform.resource.spi.ResourceProvider.class, dev.webfx.platform.resource.spi.impl.j2cl.J2clResourceProvider::new);
        register(dev.webfx.platform.resource.spi.impl.j2cl.J2clResourceBundle.class, dev.webfx.platform.resource.j2cl.J2clEmbedResourcesBundle.ProvidedJ2clResourceBundle::new);
        register(dev.webfx.platform.scheduler.spi.SchedulerProvider.class, dev.webfx.platform.uischeduler.spi.impl.gwtj2cl.GwtJ2clUiSchedulerProvider::new);
        register(dev.webfx.platform.shutdown.spi.ShutdownProvider.class, dev.webfx.platform.shutdown.spi.impl.gwtj2cl.GwtJ2clShutdownProvider::new);
        register(dev.webfx.platform.storage.spi.LocalStorageProvider.class, dev.webfx.platform.storage.spi.impl.gwtj2cl.GwtJ2clLocalStorageProvider::new);
        register(dev.webfx.platform.storage.spi.SessionStorageProvider.class, dev.webfx.platform.storage.spi.impl.gwtj2cl.GwtJ2clSessionStorageProvider::new);
        register(dev.webfx.platform.substitution.spi.SubstitutorProvider.class, dev.webfx.platform.substitution.spi.impl.var.VariablesSubstitutorProvider::new);
        register(dev.webfx.platform.substitution.var.spi.VariablesResolver.class, dev.webfx.platform.substitution.var.spi.impl.conf.ConfigVariablesResolver::new, dev.webfx.platform.substitution.var.spi.impl.localstorage.LocalStorageVariablesResolver::new, dev.webfx.platform.substitution.var.spi.impl.windowlocation.WindowLocationVariablesResolver::new);
        register(dev.webfx.platform.uischeduler.spi.UiSchedulerProvider.class, dev.webfx.platform.uischeduler.spi.impl.gwtj2cl.GwtJ2clUiSchedulerProvider::new);
        register(dev.webfx.platform.useragent.spi.UserAgentProvider.class, dev.webfx.platform.useragent.spi.impl.gwtj2cl.GwtJ2clUserAgentProvider::new);
        register(dev.webfx.platform.windowhistory.spi.WindowHistoryProvider.class, dev.webfx.platform.windowhistory.spi.impl.web.WebWindowHistoryProvider::new);
        register(dev.webfx.platform.windowhistory.spi.impl.web.JsWindowHistory.class, dev.webfx.platform.windowhistory.spi.impl.gwtj2cl.GwtJ2clJsWindowHistory::new);
        register(dev.webfx.platform.windowlocation.spi.WindowLocationProvider.class, dev.webfx.platform.windowlocation.spi.impl.gwtj2cl.GwtJ2clWindowLocationProvider::new);
        register(dev.webfx.stack.authn.login.ui.spi.UiLoginServiceProvider.class, dev.webfx.stack.authn.login.ui.spi.impl.portal.UiLoginPortalProvider::new);
        register(dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProvider.class, dev.webfx.stack.authn.login.ui.spi.impl.gateway.password.PasswordUiLoginGatewayProvider::new);
        register(dev.webfx.stack.authn.spi.AuthenticationServiceProvider.class, dev.webfx.stack.authn.spi.impl.remote.RemoteAuthenticationServiceProvider::new);
        register(dev.webfx.stack.authz.client.spi.AuthorizationClientServiceProvider.class, one.modality.crm.client.services.authz.ModalityAuthorizationClientServiceProvider::new);
        register(dev.webfx.stack.com.bus.call.spi.BusCallEndpoint.class, dev.webfx.stack.authn.buscall.AuthenticateMethodEndpoint::new, dev.webfx.stack.authn.buscall.GetUserClaimsMethodEndpoint::new, dev.webfx.stack.authn.buscall.LogoutMethodEndpoint::new, dev.webfx.stack.authn.buscall.UpdateCredentialsMethodEndpoint::new, dev.webfx.stack.db.query.buscall.ExecuteQueryBatchMethodEndpoint::new, dev.webfx.stack.db.query.buscall.ExecuteQueryMethodEndpoint::new, dev.webfx.stack.db.querypush.buscall.ExecuteQueryPushMethodEndpoint::new, dev.webfx.stack.db.submit.buscall.ExecuteSubmitBatchMethodEndpoint::new, dev.webfx.stack.db.submit.buscall.ExecuteSubmitMethodEndpoint::new, one.modality.ecommerce.document.service.buscall.LoadDocumentMethodEndpoint::new, one.modality.ecommerce.document.service.buscall.LoadPolicyMethodEndpoint::new, one.modality.ecommerce.document.service.buscall.SubmitDocumentMethodEndpoint::new, one.modality.ecommerce.payment.buscall.CancelPaymentMethodEndpoint::new, one.modality.ecommerce.payment.buscall.CompletePaymentMethodEndpoint::new, one.modality.ecommerce.payment.buscall.InitiatePaymentMethodEndpoint::new, one.modality.ecommerce.payment.buscall.MakeApiPaymentMethodEndpoint::new);
        register(dev.webfx.stack.com.bus.spi.BusServiceProvider.class, dev.webfx.stack.com.bus.spi.impl.json.client.websocket.web.WebWebsocketBusServiceProvider::new);
        register(dev.webfx.stack.com.serial.spi.SerialCodec.class, dev.webfx.stack.authn.serial.MagicLinkCredentialsSerialCodec::new, dev.webfx.stack.authn.serial.MagicLinkPasswordUpdateSerialCodec::new, dev.webfx.stack.authn.serial.MagicLinkRequestSerialCodec::new, dev.webfx.stack.authn.serial.PasswordUpdateSerialCodec::new, dev.webfx.stack.authn.serial.UserClaimsSerialCodec::new, dev.webfx.stack.authn.serial.UsernamePasswordCredentialsSerialCodec::new, dev.webfx.stack.com.bus.call.BusCallArgument.ProvidedSerialCodec::new, dev.webfx.stack.com.bus.call.BusCallResult.ProvidedSerialCodec::new, dev.webfx.stack.com.bus.call.SerializableAsyncResult.ProvidedSerialCodec::new, dev.webfx.stack.com.serial.spi.impl.ProvidedBatchSerialCodec::new, dev.webfx.stack.db.datascope.aggregate.AggregateScope.ProvidedSerialCodec::new, dev.webfx.stack.db.query.buscall.serial.PairSerialCodec::new, dev.webfx.stack.db.query.buscall.serial.QueryArgumentSerialCodec::new, dev.webfx.stack.db.query.buscall.serial.QueryResultSerialCodec::new, dev.webfx.stack.db.querypush.buscall.serial.QueryPushArgumentSerialCodec::new, dev.webfx.stack.db.querypush.buscall.serial.QueryPushResultSerialCodec::new, dev.webfx.stack.db.querypush.buscall.serial.QueryResultTranslationSerialCodec::new, dev.webfx.stack.db.submit.buscall.serial.GeneratedKeyReferenceSerialCodec::new, dev.webfx.stack.db.submit.buscall.serial.SubmitArgumentSerialCodec::new, dev.webfx.stack.db.submit.buscall.serial.SubmitResultSerialCodec::new, one.modality.base.shared.context.serial.ModalityContextSerialCodec::new, one.modality.crm.shared.services.authn.serial.ModalityGuestPrincipalSerialCodec::new, one.modality.crm.shared.services.authn.serial.ModalityUserPrincipalSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.DocumentAggregateSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.LoadDocumentArgumentSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.LoadPolicyArgumentSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.PolicyAggregateSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.SubmitDocumentChangesArgumentSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.SubmitDocumentChangesResultSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.book.AddAttendancesEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.book.AddDocumentEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.book.AddDocumentLineEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.book.AddMoneyTransferEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.book.ApplyFacilityFeeDocumentEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.book.CancelDocumentEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.book.RemoveAttendancesEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.gateway.UpdateMoneyTransferEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.multiplebookings.CancelOtherMultipleBookingsEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.multiplebookings.GetBackCancelledMultipleBookingsDepositEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.multiplebookings.MarkNotMultipleBookingEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.multiplebookings.MergeMultipleBookingsOptionsEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.registration.ConfirmDocumentEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.registration.FlagDocumentEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.registration.MarkDocumentAsArrivedEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.registration.MarkDocumentAsReadEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.registration.MarkDocumentAsWillPayEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.registration.MarkDocumentPassAsReadyEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.registration.MarkDocumentPassAsUpdatedEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.registration.documentline.CancelDocumentLineEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.registration.documentline.RemoveDocumentLineEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.registration.moneytransfer.RemoveMoneyTransferEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.security.MarkDocumentAsKnownEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.security.MarkDocumentAsUncheckedEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.security.MarkDocumentAsUnknownEventSerialCodec::new, one.modality.ecommerce.document.service.buscall.serial.security.MarkDocumentAsVerifiedEventSerialCodec::new, one.modality.ecommerce.payment.buscall.serial.CancelPaymentArgumentSerialCodec::new, one.modality.ecommerce.payment.buscall.serial.CancelPaymentResultSerialCodec::new, one.modality.ecommerce.payment.buscall.serial.CompletePaymentArgumentSerialCodec::new, one.modality.ecommerce.payment.buscall.serial.CompletePaymentResultSerialCodec::new, one.modality.ecommerce.payment.buscall.serial.InitiatePaymentArgumentSerialCodec::new, one.modality.ecommerce.payment.buscall.serial.InitiatePaymentResultSerialCodec::new, one.modality.ecommerce.payment.buscall.serial.MakeApiPaymentArgumentSerialCodec::new, one.modality.ecommerce.payment.buscall.serial.MakeApiPaymentResultSerialCodec::new, one.modality.ecommerce.payment.buscall.serial.SandboxCardSerialCodec::new);
        register(dev.webfx.stack.com.websocket.spi.WebSocketServiceProvider.class, dev.webfx.stack.com.websocket.spi.impl.gwtj2cl.GwtJ2clWebSocketServiceProvider::new);
        register(dev.webfx.stack.db.query.spi.QueryServiceProvider.class, dev.webfx.stack.db.query.spi.impl.remote.RemoteQueryServiceProvider::new);
        register(dev.webfx.stack.db.querypush.spi.QueryPushServiceProvider.class, dev.webfx.stack.db.querypush.client.simple.SimpleQueryPushClientServiceProvider::new);
        register(dev.webfx.stack.db.submit.spi.SubmitServiceProvider.class, dev.webfx.stack.db.submit.spi.impl.remote.RemoteSubmitServiceProvider::new);
        register(dev.webfx.stack.i18n.operations.ChangeLanguageRequestEmitter.class, one.modality.base.client.operations.i18n.ChangeLanguageToEnglishRequest.ProvidedEmitter::new, one.modality.base.client.operations.i18n.ChangeLanguageToFrenchRequest.ProvidedEmitter::new);
        register(dev.webfx.stack.i18n.spi.I18nProvider.class, one.modality.base.client.services.i18n.ModalityI18nProvider::new);
        register(dev.webfx.stack.orm.datasourcemodel.service.spi.DataSourceModelProvider.class, one.modality.base.shared.services.datasourcemodel.ModalityDataSourceModelProvider::new);
        register(dev.webfx.stack.orm.domainmodel.service.spi.DomainModelProvider.class, one.modality.base.shared.services.domainmodel.ModalityDomainModelProvider::new);
        register(dev.webfx.stack.orm.entity.EntityFactoryProvider.class, one.modality.base.shared.entities.impl.AttendanceImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.BookImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.CartImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.ChannelImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.CountryImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.CurrencyImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.DayTemplateImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.DocumentImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.DocumentLineImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.EventImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.EventTypeImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.FilterImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.FrontendAccountImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.GatewayCompanyImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.GatewayParameterImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.HistoryImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.ImageImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.ItemFamilyImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.ItemImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.KdmCenterImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.LabelImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.MagicLinkImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.MailImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.MediaImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.MethodImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.MoneyAccountImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.MoneyAccountTypeImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.MoneyFlowImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.MoneyTransferImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.NewsImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.OrganizationImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.OrganizationTypeImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.PersonImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.PodcastImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.RateImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.ResourceConfigurationImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.ResourceImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.ScheduledItemImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.ScheduledResourceImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.SiteImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.SiteItemFamilyImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.SnapshotImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.TeacherImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.TimeLineImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.TopicImpl.ProvidedFactory::new, one.modality.base.shared.entities.impl.VideoImpl.ProvidedFactory::new);
        register(dev.webfx.stack.orm.push.client.spi.PushClientServiceProvider.class, dev.webfx.stack.orm.push.client.spi.impl.simple.SimplePushClientServiceProvider::new);
        register(dev.webfx.stack.routing.router.spi.RouterFactoryProvider.class, dev.webfx.stack.routing.router.spi.impl.client.ClientRouterFactoryProvider::new);
        register(dev.webfx.stack.routing.uirouter.UiRoute.class, one.modality.base.frontoffice.activities.account.AccountUiRoute::new, one.modality.base.frontoffice.activities.account.friendsfamily.AccountFriendsAndFamilyUiRoute::new, one.modality.base.frontoffice.activities.account.friendsfamily.edit.AccountFriendsAndFamilyEditUiRoute::new, one.modality.base.frontoffice.activities.account.personalinfo.AccountPersonalInformationUiRoute::new, one.modality.base.frontoffice.activities.account.settings.AccountSettingsUiRoute::new, one.modality.base.frontoffice.activities.alerts.AlertsUiRoute::new, one.modality.crm.client.activities.login.LoginUiRoute::new, one.modality.crm.client.activities.unauthorized.UnauthorizedUiRoute::new, one.modality.event.frontoffice.activities.booking.BookingUiRoute::new, one.modality.event.frontoffice.activities.booking.process.event.BookEventUiRoute::new, org.kadampabookings.kbs.frontoffice.activities.news.ArticleUiRoute::new, org.kadampabookings.kbs.frontoffice.activities.news.NewsUiRoute::new, org.kadampabookings.kbs.frontoffice.activities.podcasts.PodcastsUiRoute::new);
        register(dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter.class, one.modality.base.frontoffice.activities.account.RouteToAccountRequestEmitter::new, one.modality.base.frontoffice.activities.account.friendsfamily.RouteToAccountFriendsAndFamilyEmitter::new, one.modality.base.frontoffice.activities.account.friendsfamily.edit.RouteToAccountFriendsAndFamilyEditEmitter::new, one.modality.base.frontoffice.activities.account.personalinfo.RouteToAccountPersonalInformationEmitter::new, one.modality.base.frontoffice.activities.account.settings.RouteToAccountSettingsRequestEmitter::new, one.modality.base.frontoffice.activities.alerts.RouteToAlertsRequestEmitter::new, one.modality.event.frontoffice.activities.booking.RouteToBookingRequestEmitter::new, org.kadampabookings.kbs.frontoffice.activities.news.RouteToNewsRequestEmitter::new, org.kadampabookings.kbs.frontoffice.activities.podcasts.RouteToPodcastsRequestEmitter::new);
        register(dev.webfx.stack.session.spi.SessionServiceProvider.class, dev.webfx.stack.session.spi.impl.client.ClientSessionServiceProvider::new);
        register(javafx.application.Application.class, one.modality.base.frontoffice.application.ModalityFrontOfficeApplication::new);
        register(one.modality.ecommerce.document.service.spi.DocumentServiceProvider.class, one.modality.ecommerce.document.service.spi.impl.remote.RemoteDocumentServiceProvider::new);
        register(one.modality.ecommerce.payment.spi.PaymentServiceProvider.class, one.modality.ecommerce.payment.spi.impl.remote.RemotePaymentServiceProvider::new);
    }
}