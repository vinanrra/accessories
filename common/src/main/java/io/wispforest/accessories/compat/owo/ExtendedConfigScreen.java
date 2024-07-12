package io.wispforest.accessories.compat.owo;

import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.config.ui.OptionComponents;
import io.wispforest.owo.config.ui.component.ConfigSlider;
import io.wispforest.owo.config.ui.component.ConfigTextBox;
import io.wispforest.owo.config.ui.component.ListOptionContainer;
import io.wispforest.owo.config.ui.component.SearchAnchorComponent;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.ui.util.UISounds;
import io.wispforest.owo.util.NumberReflection;
import io.wispforest.owo.util.ReflectionUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.intellij.lang.annotations.Identifier;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.*;

public final class ExtendedConfigScreen extends ConfigScreen {
    protected ExtendedConfigScreen(ConfigWrapper<?> config, @Nullable Screen parent, BiConsumer<ConfigWrapper<?>, FactoryRegister> consumer) {
        super(ConfigScreen.DEFAULT_MODEL_ID, config, parent);

        consumer.accept(config, this.extraFactories::put);
    }

    public static Function<Screen, Screen> buildFunc(ConfigWrapper<?> config, BiConsumer<ConfigWrapper<?>, FactoryRegister> consumer) {
        return screen -> new ExtendedConfigScreen(config, screen, consumer);
    }

    public interface FactoryRegister {
        void registerFactory(Predicate<Option<?>> predicate, OptionComponentFactory<?> factory);
    }
}
