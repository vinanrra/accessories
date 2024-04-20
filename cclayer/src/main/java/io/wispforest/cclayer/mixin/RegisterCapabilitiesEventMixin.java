package io.wispforest.cclayer.mixin;

import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AttachCapabilitiesEvent.class)
public abstract class RegisterCapabilitiesEventMixin {
//    @Inject(method = "addCapability", at = @At("HEAD"))
//    private <T, C> void hookForBypassingCuriosRegisterCall(ResourceLocation key, ICapabilityProvider cap, CallbackInfo ci) {
//        if(capability.equals(CuriosCapability.ITEM) && !provider.equals(CCLayer.BASE_PROVIDER)){
//            ICapabilityProvider<ItemStack, Void, ICurio> icurioProvider = (ICapabilityProvider<ItemStack, Void, ICurio>) (Object) provider;
//
//            var wrappedCurio = new WrappedICurioProvider(icurioProvider);
//
//            for (var itemLike : items) AccessoriesAPI.registerAccessory(Objects.requireNonNull(itemLike.asItem()), wrappedCurio);
//        }
//    }
}
