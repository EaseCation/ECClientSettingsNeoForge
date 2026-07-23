package net.easecation.clientsettings.mixin.obsoverlay;

import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Avoids resolving optional compatibility targets when their owning mod is absent. */
public final class ObsOverlayMixinPlugin implements IMixinConfigPlugin {

    private static final String VIA_BEDROCK_MIXIN =
            "net.easecation.clientsettings.mixin.obsoverlay.ViaBedrockDeferredNameTagMixin";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !VIA_BEDROCK_MIXIN.equals(mixinClassName) || classResourceExists(targetClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (VIA_BEDROCK_MIXIN.equals(mixinClassName)) {
            ObsOverlayRuntime.markViaBedrockDeferredNameTagHookApplied();
        }
    }

    private static boolean classResourceExists(String className) {
        String resource = className.replace('.', '/') + ".class";
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null && contextLoader.getResource(resource) != null) {
            return true;
        }
        ClassLoader pluginLoader = ObsOverlayMixinPlugin.class.getClassLoader();
        return pluginLoader != null && pluginLoader.getResource(resource) != null;
    }
}
