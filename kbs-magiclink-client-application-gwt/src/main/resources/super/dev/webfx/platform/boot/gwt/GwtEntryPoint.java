package dev.webfx.platform.boot.gwt;

import com.google.gwt.core.client.EntryPoint;
import dev.webfx.platform.boot.ApplicationBooter;
import dev.webfx.platform.boot.spi.ApplicationBooterProvider;
import dev.webfx.platform.reflect.RArray;

import static dev.webfx.platform.service.gwtj2cl.ServiceRegistry.*;

public final class GwtEntryPoint implements ApplicationBooterProvider, EntryPoint {

    @Override
    public void onModuleLoad() {
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
        register(dev.webfx.kit.mapper.spi.WebFxKitMapperProvider.class, dev.webfx.kit.mapper.spi.impl.gwtj2cl.GwtJ2clWebFxKitHtmlMapperProvider::new);
        register(dev.webfx.platform.ast.spi.factory.AstFactoryProvider.class, dev.webfx.platform.ast.spi.factory.impl.gwt.GwtAstFactoryProvider::new);
        register(dev.webfx.platform.ast.spi.formatter.AstFormatterProvider.class, dev.webfx.platform.ast.json.formatter.JsonFormatterProvider::new);
        register(dev.webfx.platform.ast.spi.parser.AstParserProvider.class, dev.webfx.platform.ast.json.parser.JsonParserProvider::new);
        register(dev.webfx.platform.boot.spi.ApplicationModuleBooter.class, dev.webfx.kit.launcher.WebFxKitLauncherModuleBooter::new, dev.webfx.platform.boot.spi.impl.ApplicationJobsInitializer::new, dev.webfx.platform.boot.spi.impl.ApplicationJobsStarter::new, dev.webfx.platform.resource.spi.impl.gwt.GwtResourceModuleBooter::new, dev.webfx.stack.com.bus.call.BusCallModuleBooter::new, dev.webfx.stack.com.bus.spi.impl.json.client.JsonClientBusModuleBooter::new, dev.webfx.stack.com.serial.SerialCodecModuleBooter::new, dev.webfx.stack.ui.fxraiser.json.JsonFXRaiserModuleBooter::new);
        register(dev.webfx.platform.console.spi.ConsoleProvider.class, dev.webfx.platform.console.spi.impl.gwtj2cl.GwtJ2clConsoleProvider::new);
        register(dev.webfx.platform.os.spi.OperatingSystemProvider.class, dev.webfx.platform.os.spi.impl.gwtj2cl.GwtJ2clOperatingSystemProvider::new);
        register(dev.webfx.platform.resource.spi.ResourceProvider.class, dev.webfx.platform.resource.spi.impl.gwt.GwtResourceProvider::new);
        register(dev.webfx.platform.resource.spi.impl.gwt.GwtResourceBundle.class, dev.webfx.platform.resource.gwt.GwtEmbedResourcesBundle.ProvidedGwtResourceBundle::new);
        register(dev.webfx.platform.scheduler.spi.SchedulerProvider.class, dev.webfx.platform.uischeduler.spi.impl.gwtj2cl.GwtJ2clUiSchedulerProvider::new);
        register(dev.webfx.platform.shutdown.spi.ShutdownProvider.class, dev.webfx.platform.shutdown.spi.impl.gwtj2cl.GwtJ2clShutdownProvider::new);
        register(dev.webfx.platform.storage.spi.LocalStorageProvider.class, dev.webfx.platform.storage.spi.impl.gwtj2cl.GwtJ2clLocalStorageProvider::new);
        register(dev.webfx.platform.storage.spi.SessionStorageProvider.class, dev.webfx.platform.storage.spi.impl.gwtj2cl.GwtJ2clSessionStorageProvider::new);
        register(dev.webfx.platform.substitution.spi.SubstitutorProvider.class, dev.webfx.platform.substitution.spi.impl.var.VariablesSubstitutorProvider::new);
        register(dev.webfx.platform.substitution.var.spi.VariablesResolver.class, dev.webfx.platform.substitution.var.spi.impl.localstorage.LocalStorageVariablesResolver::new, dev.webfx.platform.substitution.var.spi.impl.windowlocation.WindowLocationVariablesResolver::new);
        register(dev.webfx.platform.uischeduler.spi.UiSchedulerProvider.class, dev.webfx.platform.uischeduler.spi.impl.gwtj2cl.GwtJ2clUiSchedulerProvider::new);
        register(dev.webfx.platform.useragent.spi.UserAgentProvider.class, dev.webfx.platform.useragent.spi.impl.gwtj2cl.GwtJ2clUserAgentProvider::new);
        register(dev.webfx.platform.windowlocation.spi.WindowLocationProvider.class, dev.webfx.platform.windowlocation.spi.impl.gwtj2cl.GwtJ2clWindowLocationProvider::new);
        register(dev.webfx.stack.authn.spi.AuthenticationServiceProvider.class, dev.webfx.stack.authn.spi.impl.remote.RemoteAuthenticationServiceProvider::new);
        register(dev.webfx.stack.com.bus.call.spi.BusCallEndpoint.class, dev.webfx.stack.authn.buscall.AuthenticateMethodEndpoint::new, dev.webfx.stack.authn.buscall.GetUserClaimsMethodEndpoint::new, dev.webfx.stack.authn.buscall.LogoutMethodEndpoint::new, dev.webfx.stack.db.query.buscall.ExecuteQueryBatchMethodEndpoint::new, dev.webfx.stack.db.query.buscall.ExecuteQueryMethodEndpoint::new, dev.webfx.stack.db.submit.buscall.ExecuteSubmitBatchMethodEndpoint::new, dev.webfx.stack.db.submit.buscall.ExecuteSubmitMethodEndpoint::new);
        register(dev.webfx.stack.com.bus.spi.BusServiceProvider.class, dev.webfx.stack.com.bus.spi.impl.json.client.websocket.web.WebWebsocketBusServiceProvider::new);
        register(dev.webfx.stack.com.serial.spi.SerialCodec.class, dev.webfx.stack.authn.serial.MagicLinkCredentialsSerialCodec::new, dev.webfx.stack.authn.serial.MagicLinkRequestSerialCodec::new, dev.webfx.stack.authn.serial.UserClaimsSerialCodec::new, dev.webfx.stack.authn.serial.UsernamePasswordCredentialsSerialCodec::new, dev.webfx.stack.com.bus.call.BusCallArgument.ProvidedSerialCodec::new, dev.webfx.stack.com.bus.call.BusCallResult.ProvidedSerialCodec::new, dev.webfx.stack.com.bus.call.SerializableAsyncResult.ProvidedSerialCodec::new, dev.webfx.stack.com.serial.spi.impl.ProvidedBatchSerialCodec::new, dev.webfx.stack.db.datascope.aggregate.AggregateScope.ProvidedSerialCodec::new, dev.webfx.stack.db.query.buscall.serial.PairSerialCodec::new, dev.webfx.stack.db.query.buscall.serial.QueryArgumentSerialCodec::new, dev.webfx.stack.db.query.buscall.serial.QueryResultSerialCodec::new, dev.webfx.stack.db.submit.buscall.serial.GeneratedKeyReferenceSerialCodec::new, dev.webfx.stack.db.submit.buscall.serial.SubmitArgumentSerialCodec::new, dev.webfx.stack.db.submit.buscall.serial.SubmitResultSerialCodec::new);
        register(dev.webfx.stack.com.websocket.spi.WebSocketServiceProvider.class, dev.webfx.stack.com.websocket.spi.impl.gwtj2cl.GwtJ2clWebSocketServiceProvider::new);
        register(dev.webfx.stack.db.query.spi.QueryServiceProvider.class, dev.webfx.stack.db.query.spi.impl.remote.RemoteQueryServiceProvider::new);
        register(dev.webfx.stack.db.submit.spi.SubmitServiceProvider.class, dev.webfx.stack.db.submit.spi.impl.remote.RemoteSubmitServiceProvider::new);
        register(dev.webfx.stack.i18n.operations.ChangeLanguageRequestEmitter.class, one.modality.base.client.operations.i18n.ChangeLanguageToEnglishRequest.ProvidedEmitter::new, one.modality.base.client.operations.i18n.ChangeLanguageToFrenchRequest.ProvidedEmitter::new);
        register(dev.webfx.stack.i18n.spi.I18nProvider.class, one.modality.base.client.services.i18n.ModalityI18nProvider::new);
        register(dev.webfx.stack.orm.datasourcemodel.service.spi.DataSourceModelProvider.class, one.modality.base.shared.services.datasourcemodel.ModalityDataSourceModelProvider::new);
        register(dev.webfx.stack.orm.domainmodel.service.spi.DomainModelProvider.class, one.modality.base.shared.services.domainmodel.ModalityDomainModelProvider::new);
        register(dev.webfx.stack.session.spi.SessionServiceProvider.class, dev.webfx.stack.session.spi.impl.client.ClientSessionServiceProvider::new);
        register(javafx.application.Application.class, one.modality.crm.magiclink.application.MagicLinkApplication::new);
    }
}