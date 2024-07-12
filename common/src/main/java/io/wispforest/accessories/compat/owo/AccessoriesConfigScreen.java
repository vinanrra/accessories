package io.wispforest.accessories.compat.owo;

import io.wispforest.accessories.Accessories;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.config.ui.component.OptionValueProvider;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.util.NumberReflection;
import io.wispforest.owo.util.ReflectionUtils;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;

public class AccessoriesConfigScreen {

    private static boolean isValidList(Field field) {
        if (field.getType() != List.class) return false;

        var listType = ReflectionUtils.getTypeArgument(field.getGenericType(), 0);
        if (listType == null) return false;

        return String.class != listType && !NumberReflection.isNumberType(listType);
    }

    public static final Function<Screen, Screen> builder = ExtendedConfigScreen.buildFunc(Accessories.getConfig(), (config, factoryRegister) -> {
        factoryRegister.registerFactory(
                option -> isValidList(option.backingField().field()),
                (uiModel, option) -> {
                    var layout = new StructListOptionContainer<>(uiModel, option);
                    return new OptionComponentFactory.Result<Component, OptionValueProvider>(layout, layout);
                });
    });
}
