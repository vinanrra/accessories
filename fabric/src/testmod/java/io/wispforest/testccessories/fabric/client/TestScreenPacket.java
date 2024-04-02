package io.wispforest.testccessories.fabric.client;

import io.wispforest.accessories.networking.AccessoriesPacket;
import io.wispforest.testccessories.fabric.TestMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public class TestScreenPacket extends AccessoriesPacket {

    private static final MenuProvider INSTANCE = new SimpleMenuProvider(TestMenu::new, Component.literal("TEST"));

    @Override public void write(FriendlyByteBuf buf) {}
    @Override protected void read(FriendlyByteBuf buf) {}

    @Override
    public void handle(Player player) {
        if(player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(INSTANCE);
        }
    }
}
