package io.wispforest.testccessories.fabric.client;

import io.wispforest.testccessories.fabric.Testccessories;
import io.wispforest.testccessories.fabric.accessories.AppleAccessory;
import io.wispforest.testccessories.fabric.accessories.PointedDripstoneAccessory;
import io.wispforest.testccessories.fabric.accessories.PotatoAccessory;
import io.wispforest.testccessories.fabric.accessories.TntAccessory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Testccessories.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TestccessoriesClient {

    @SubscribeEvent
    public static void onInitializeClient(FMLClientSetupEvent event) {
        AppleAccessory.clientInit();
        PotatoAccessory.clientInit();
        PointedDripstoneAccessory.clientInit();
        TntAccessory.clientInit();

        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
    }
}