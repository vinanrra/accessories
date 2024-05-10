package top.theillusivec4.curios.common.network.client;

import com.aetherteam.aether.network.packet.serverbound.OpenAccessoriesPacket;
import io.wispforest.accessories.networking.server.NukeAccessories;
import io.wispforest.accessories.networking.server.ScreenOpen;
import io.wispforest.cclayer.WrappedAccessoriesPacket;
import net.minecraft.world.item.ItemStack;

public class CPacketOpenCurios extends WrappedAccessoriesPacket {
    public CPacketOpenCurios(ItemStack stack) {
        super(new ScreenOpen());
    }
}
