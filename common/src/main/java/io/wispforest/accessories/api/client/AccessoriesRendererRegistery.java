package io.wispforest.accessories.api.client;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Main class used to register and hold {@link AccessoryRenderer}'s for the given items
 */
public class AccessoriesRendererRegistery {

    private static final Map<Item, Supplier<AccessoryRenderer>> RENDERERS = new HashMap<>();

    private static final Map<Item, AccessoryRenderer> CACHED_RENDERERS = new HashMap<>();

    /**
     * Main method used to register an {@link Item} with a given {@link AccessoryRenderer}
     */
    public static void registerRenderer(Item item, Supplier<AccessoryRenderer> renderer){
        RENDERERS.put(item, renderer);
    }

    public static void registerDefaultedRenderer(Item item){
        RENDERERS.put(item, DefaultAccessoryRenderer::new);
    }

    public static final String defaultRenderOverrideKey = "AccessoriesDefaultRenderOverride";

    @Nullable
    public static AccessoryRenderer getRender(ItemStack stack){
        if(stack.hasTag()) {
            var tag = stack.getTag();

            if(tag.contains(defaultRenderOverrideKey)){
                if(tag.getBoolean(defaultRenderOverrideKey)) {
                    return DefaultAccessoryRenderer.INSTANCE;
                } else if(AccessoriesAPI.getOrDefaultAccessory(stack.getItem()) == AccessoriesAPI.defaultAccessory()) {
                    return null;
                }
            }
        }

        return getRender(stack.getItem());
    }

    /**
     * @return Either the {@link AccessoryRenderer} bound to the item or the instance of the {@link DefaultAccessoryRenderer}
     */
    @Nullable
    public static AccessoryRenderer getRender(Item item){
        var accessory = AccessoriesAPI.getOrDefaultAccessory(item);

        if(accessory == AccessoriesAPI.defaultAccessory() || (!CACHED_RENDERERS.containsKey(item)) && Accessories.getConfig().clientData.useDefaultRender) {
            return DefaultAccessoryRenderer.INSTANCE;
        }

        return CACHED_RENDERERS.get(item);
    }

    public static void onReload() {
        CACHED_RENDERERS.clear();

        RENDERERS.forEach((item, supplier) -> CACHED_RENDERERS.put(item, supplier.get()));
    }
}
