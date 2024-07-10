package io.wispforest.accessories.compat.owo;

import com.mojang.datafixers.types.Func;
import io.wispforest.accessories.Accessories;
import io.wispforest.owo.config.annotation.Expanded;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.config.ui.component.OptionValueProvider;
import io.wispforest.owo.config.ui.component.SearchAnchorComponent;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.util.NumberReflection;
import io.wispforest.owo.util.ReflectionUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

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
                    var parentKey = option.key().parent();

                    var expanded = !parentKey.isRoot() && config.fieldForKey(parentKey).isAnnotationPresent(Expanded.class);

                    var layout = new ExtendedConfigScreen.CustomListOptionContainer<>(uiModel, option);

                    var finalLayout = Containers.collapsible(
                            Sizing.fill(100), Sizing.content(),
                            net.minecraft.network.chat.Component.translatable("text.config." + config.name() + ".category." + parentKey.asString()),
                            expanded
                    ).<CollapsibleContainer>configure(nestedContainer -> {
                        final var categoryKey = "text.config." + config.name() + ".category." + parentKey.asString();
                        if (I18n.exists(categoryKey + ".tooltip")) {
                            nestedContainer.titleLayout().tooltip(net.minecraft.network.chat.Component.translatable(categoryKey + ".tooltip"));
                        }

                        nestedContainer.titleLayout().child(new SearchAnchorComponent(
                                nestedContainer.titleLayout(),
                                option.key(),
                                () -> I18n.get(categoryKey)
                        ).highlightConfigurator(highlight ->
                                highlight.positioning(Positioning.absolute(-5, -5))
                                        .verticalSizing(Sizing.fixed(19))
                        ));
                    }).child(layout);

                    return new OptionComponentFactory.Result<Component, OptionValueProvider>(layout, layout);
                });
    });
}
