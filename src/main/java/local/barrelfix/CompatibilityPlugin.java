package local.barrelfix;

import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Runs before Mixin resolves the patch class, including its Core references. */
public final class CompatibilityPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sophisticated Storage Fixes");
    private boolean enabled;
    private final java.util.function.Function<String, java.util.Optional<ModContainer>> lookup;
    public CompatibilityPlugin() { this(id -> FabricLoader.getInstance().getModContainer(id)); }
    CompatibilityPlugin(java.util.function.Function<String, java.util.Optional<ModContainer>> lookup) { this.lookup = lookup; }

    @Override public void onLoad(String mixinPackage) {
        enabled = false;
        String reason;
        try {

            var storage = lookup.apply("sophisticatedstorage");
            var core = lookup.apply("sophisticatedcore");
            if (storage.isEmpty() || core.isEmpty()) {
                reason = "Sophisticated Storage or Sophisticated Core is not installed";
            } else {
                String storageVersion = storage.get().getMetadata().getVersion().getFriendlyString();
                String coreVersion = core.get().getMetadata().getVersion().getFriendlyString();
                reason = Compatibility.check(storageVersion, coreVersion,
                    readClass(storage.get(), Compatibility.STORAGE_CLASS),
                    readClass(core.get(), Compatibility.CORE_CLASS));
                if (reason == null) {
                    enabled = true;
                    LOGGER.info("Barrel fix compatibility check passed (Storage {}, Core {}).", storageVersion, coreVersion);
                    return;
                }
            }
        } catch (Exception | LinkageError e) {
            reason = "compatibility check failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        LOGGER.warn("Barrel fix DISABLED: {}. Continuing without the fix; barrel displays may flicker. "
            + "Use checked Fabric 1.20.1 releases: Storage 1.20.1-1.0.10.1.100 through 1.20.1-1.3.5.11.142, "
            + "with a compatible Core release from 1.20.1-1.0.8.1.119 through 1.20.1-1.2.7.15.166.", reason);
    }
    private static byte[] readClass(ModContainer mod, String name) throws java.io.IOException {
        var path = mod.findPath(name);
        return path.isPresent() ? Files.readAllBytes(path.get()) : null;
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return enabled; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
