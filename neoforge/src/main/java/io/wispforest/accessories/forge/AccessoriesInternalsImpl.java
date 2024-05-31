package io.wispforest.accessories.forge;

import com.google.gson.JsonObject;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class AccessoriesInternalsImpl {

    public static AccessoriesHolder getHolder(LivingEntity livingEntity){
        return ((AttachmentTarget) livingEntity).getAttachedOrCreate(AccessoriesForge.HOLDER_ATTACHMENT_TYPE);
    }

    public static void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolderImpl> modifier){
        var holder = (AccessoriesHolderImpl) getHolder(livingEntity);

        holder = modifier.apply(holder);

        ((AttachmentTarget) livingEntity).setAttached(AccessoriesForge.HOLDER_ATTACHMENT_TYPE, holder);
    }

    public static AccessoriesNetworkHandler getNetworkHandler(){
        return AccessoriesForgeNetworkHandler.INSTANCE;
    }

    public static <T> Optional<Collection<Holder<T>>> getHolder(TagKey<T> tagKey){
        return currentContext.map(iContext -> iContext.getTag(tagKey));
    }

    private static Optional<ICondition.IContext> currentContext = Optional.empty();

    public static void setContext(@Nullable ICondition.IContext context){
        currentContext = Optional.ofNullable(context);
    }

    //--

    public static void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }

    public static boolean isValidOnConditions(JsonObject object) {
        //TODO [PORT]: UNKNOWN IF WORKS
        return ICondition.shouldRegisterEntry(object);
    }

    public static <T extends AbstractContainerMenu> MenuType<T> registerMenuType(ResourceLocation location, TriFunction<Integer, Inventory, FriendlyByteBuf, T> func) {
        var type = IForgeMenuType.create(func::apply);

        ForgeRegistries.MENU_TYPES.register(location, type);

        return type;
    }

    public static void openAccessoriesMenu(Player player, @Nullable LivingEntity targetEntity, @Nullable ItemStack carriedStack) {
        NetworkHooks.openScreen((ServerPlayer) player, new SimpleMenuProvider((i, arg, arg2) -> {
            var menu = new AccessoriesMenu(i, arg, true, targetEntity);

            if(carriedStack != null) menu.setCarried(carriedStack);

            return menu;
        }, Component.empty()), buf -> {
            AccessoriesMenu.writeBufData(buf, targetEntity);
        });
    }


}
