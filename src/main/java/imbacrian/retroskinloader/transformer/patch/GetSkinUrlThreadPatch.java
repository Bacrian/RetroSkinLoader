package imbacrian.retroskinloader.transformer.patch;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import static org.objectweb.asm.Opcodes.*;

import imbacrian.retroskinloader.BootstrapLogger;
import imbacrian.retroskinloader.transformer.ClassTransformationContext;
import imbacrian.retroskinloader.transformer.TargetedClassTransformer;


public final class GetSkinUrlThreadPatch extends TargetedClassTransformer {

	private static final String GET_SKIN_URL_THREAD = "net/minecraft/core/util/helper/GetSkinUrlThread";
	private static final String PROBE = "imbacrian/retroskinloader/transformer/patch/GetSkinUrlThreadPatch";

	public GetSkinUrlThreadPatch() {
		super("retroskinloader:get-skin-url-thread-patch", 1000, GET_SKIN_URL_THREAD);
	}

	@Override
	public boolean transform(ClassTransformationContext context) throws Exception {
		if (!context.isTarget(GET_SKIN_URL_THREAD)) {
			return false;
		}

		MethodNode runMethod = context.findMethod(GET_SKIN_URL_THREAD, "run", "()V");
		if (runMethod == null) {
			BootstrapLogger.LOGGER.warn("RetroSkinLoader: couldn't find GetSkinUrlThread.run()V — ¿has signature changed?");
			return false;
		}

		// Visible entry mark: if this doesn't appear on logs, the patch isn't getting applied.
		InsnList probe = new InsnList();
		probe.add(new VarInsnNode(ALOAD, 0));
		probe.add(new MethodInsnNode(INVOKESTATIC, PROBE, "onRunIntercepted",
			"(Ljava/lang/Object;)V", false));
		runMethod.instructions.insertBefore(runMethod.instructions.getFirst(), probe);

		return true;
	}

	// Call via injected bytecode - keeping it public and static.
	public static void onRunIntercepted(Object thread) {
		BootstrapLogger.LOGGER.info("[RetroSkinLoader] Intercepting GetSkinUrlThread.run() -> " + thread);
	}
}
