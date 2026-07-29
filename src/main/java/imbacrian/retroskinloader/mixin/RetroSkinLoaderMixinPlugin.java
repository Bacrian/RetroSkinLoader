package imbacrian.retroskinloader.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import imbacrian.retroskinloader.BootstrapLogger;
import imbacrian.retroskinloader.transformer.ClassTransformationReport;
import imbacrian.retroskinloader.transformer.TransformerBootstrapSupport;

public final class RetroSkinLoaderMixinPlugin implements IMixinConfigPlugin {

    private static final TransformerBootstrapSupport BOOTSTRAP =
        new TransformerBootstrapSupport(RetroSkinLoaderMixinPlugin.class);

    @Override
    public void onLoad(String mixinPackage) {
        BootstrapLogger.LOGGER.info("RetroSkinLoader mixin plugin loaded for " + mixinPackage);
    }

    @Override
    public String getRefMapperConfig() {
        return null; // no refmap: working with actual names from runtime
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null; // the targets come out from the mixins.json
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        try {
            ClassTransformationReport report =
                BOOTSTRAP.transformClassNode(targetClassName.replace('.', '/'), targetClass);
            if (report.isModified()) {
                BootstrapLogger.LOGGER.info(
                    "RetroSkinLoader applied " + report.getAppliedTransformerNames() + " to " + targetClassName);
            }
        } catch (Exception exception) {
            BootstrapLogger.LOGGER.error("RetroSkinLoader failed to transform " + targetClassName, exception);
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
