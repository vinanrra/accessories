package io.wispforest.cclayer.client;

import io.wispforest.cclayer.CCLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@Mod.EventBusSubscriber(modid = CCLayer.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCLayerClient {

    @SubscribeEvent
    public static void onClientInit(FMLClientSetupEvent event){
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        eventBus.addListener(CCLayerClient::loaderRenders);
    }

    @SubscribeEvent
    public static void loaderRenders(EntityRenderersEvent.AddLayers event) {
        CuriosRendererRegistry.load();
    }
}
