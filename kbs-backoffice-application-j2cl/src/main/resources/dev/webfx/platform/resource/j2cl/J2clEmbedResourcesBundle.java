// File managed by WebFX (DO NOT EDIT MANUALLY)
package dev.webfx.platform.resource.j2cl;

import org.treblereel.j2cl.processors.annotations.GWT3Resource;
import org.treblereel.j2cl.processors.common.resources.ClientBundle;
import org.treblereel.j2cl.processors.common.resources.TextResource;
import dev.webfx.platform.resource.spi.impl.j2cl.J2clResourceBundleBase;

@GWT3Resource
public interface J2clEmbedResourcesBundle extends ClientBundle {

    J2clEmbedResourcesBundle R = J2clEmbedResourcesBundleImpl.INSTANCE;

    @Source("/dev/webfx/platform/conf/src-root.properties")
    TextResource r1();

    @Source("/dev/webfx/platform/meta/exe/exe.properties")
    TextResource r2();

    @Source("/dev/webfx/stack/i18n/de.properties")
    TextResource r3();

    @Source("/dev/webfx/stack/i18n/en.json")
    TextResource r4();

    @Source("/dev/webfx/stack/i18n/es.properties")
    TextResource r5();

    @Source("/dev/webfx/stack/i18n/fr.properties")
    TextResource r6();

    @Source("/dev/webfx/stack/i18n/pt.properties")
    TextResource r7();

    @Source("/dev/webfx/stack/i18n/vi.properties")
    TextResource r8();

    @Source("/dev/webfx/stack/i18n/zh.properties")
    TextResource r9();

    @Source("/one/modality/base/shared/domainmodel/DomainModelSnapshot.json")
    TextResource r10();

    final class ProvidedJ2clResourceBundle extends J2clResourceBundleBase {
        public ProvidedJ2clResourceBundle() {
            registerResource("dev/webfx/platform/conf/src-root.properties", () -> R.r1().getText());
            registerResource("dev/webfx/platform/meta/exe/exe.properties", () -> R.r2().getText());
            registerResource("dev/webfx/stack/i18n/de.properties", () -> R.r3().getText());
            registerResource("dev/webfx/stack/i18n/en.json", () -> R.r4().getText());
            registerResource("dev/webfx/stack/i18n/es.properties", () -> R.r5().getText());
            registerResource("dev/webfx/stack/i18n/fr.properties", () -> R.r6().getText());
            registerResource("dev/webfx/stack/i18n/pt.properties", () -> R.r7().getText());
            registerResource("dev/webfx/stack/i18n/vi.properties", () -> R.r8().getText());
            registerResource("dev/webfx/stack/i18n/zh.properties", () -> R.r9().getText());
            registerResource("one/modality/base/shared/domainmodel/DomainModelSnapshot.json", () -> R.r10().getText());
        }
    }
}