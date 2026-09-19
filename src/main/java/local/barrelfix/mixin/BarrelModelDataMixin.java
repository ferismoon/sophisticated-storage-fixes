package local.barrelfix.mixin;

import net.p3pp3rf1y.sophisticatedcore.util.model.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Isolate the model-data handoff between concurrently rebuilt chunks. */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelBakedModelBase", remap = false)
public class BarrelModelDataMixin {
    @Unique
    private final ThreadLocal<ModelData> barrelfix$modelData = new ThreadLocal<>();

    @Redirect(method = {"setModelData", "emitBlockQuads", "emitItemQuads"},
        at = @At(value = "FIELD", opcode = 181,
            target = "Lnet/p3pp3rf1y/sophisticatedstorage/client/render/BarrelBakedModelBase;modelData:Lnet/p3pp3rf1y/sophisticatedcore/util/model/ModelData;"),
        require = 3, expect = 3, allow = 3)
    private void barrelfix$write(@Coerce Object model, ModelData data) {
        if (data == null) {
            barrelfix$modelData.remove();
        } else {
            barrelfix$modelData.set(data);
        }
    }

    // Runtime intermediary name for BakedModel.getQuads(BlockState, Direction, Random).
    @Redirect(method = "method_4707", at = @At(value = "FIELD", opcode = 180,
        target = "Lnet/p3pp3rf1y/sophisticatedstorage/client/render/BarrelBakedModelBase;modelData:Lnet/p3pp3rf1y/sophisticatedcore/util/model/ModelData;"),
        require = 1, expect = 1, allow = 1)
    private ModelData barrelfix$read(@Coerce Object model) {
        return barrelfix$modelData.get();
    }
}
