package io.wispforest.accessories.fabric.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.compat.owo.AccessoriesConfigScreen;
import me.shedaniel.autoconfig.AutoConfig;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;

public class ModMenuEntry implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AccessoriesConfigScreen.builder::apply;
    }
}
