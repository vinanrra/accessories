package top.theillusivec4.curios.common.capability;

import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.compat.WrappedCurioItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CurioInventoryCapability {

    public static ICapabilityProvider createProvider(final LivingEntity livingEntity) {
        return new Provider(livingEntity);
    }

    public static class Provider implements ICapabilitySerializable<Tag> {

        final LazyOptional<ICuriosItemHandler> optional;
        final ICuriosItemHandler handler;
        final LivingEntity wearer;

        Provider(final LivingEntity livingEntity) {
            this.wearer = livingEntity;
            this.handler = new WrappedCurioItemHandler(() -> (AccessoriesCapabilityImpl) this.wearer.accessoriesCapability());
            this.optional = LazyOptional.of(() -> this.handler);
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nullable Capability<T> capability, Direction facing) {
            if (CuriosApi.getEntitySlots(this.wearer.getType()).isEmpty()) {
                return LazyOptional.empty();
            }

            return CuriosCapability.INVENTORY.orEmpty(capability, this.optional);
        }

        @Override
        public Tag serializeNBT() {
            if (CuriosApi.getEntitySlots(this.wearer.getType()).isEmpty()) {
                return new CompoundTag();
            }

            return this.handler.writeTag();
        }

        @Override
        public void deserializeNBT(Tag nbt) {
            if (!(nbt instanceof CompoundTag compoundTag)) return;
            //if (CuriosApi.getEntitySlots(this.wearer.getType()).isEmpty()) return;

            var inv = new CurioInventory(new AccessoriesCapabilityImpl(this.wearer));

            inv.deserializeNBT(compoundTag);

            inv.init(this.handler);
        }
    }
}
