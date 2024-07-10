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
import org.intellij.lang.annotations.Identifier;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.Style;
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

    //--

    public static class CustomListOptionContainer<T> extends ListOptionContainer<T> {

        private final UIModel uiModel;

        public CustomListOptionContainer(UIModel uiModel, Option option) {
            super(option);

            this.uiModel = uiModel;

            //this.contentLayout.horizontalSizing(Sizing.fill(90));

            this.refreshOptions();
        }

        @Override
        protected void refreshOptions() {
            if(uiModel == null) return;

            this.collapsibleChildren.clear();

            var listType = ReflectionUtils.getTypeArgument(this.backingOption.backingField().field().getGenericType(), 0);
            for (int i = 0; i < this.backingList.size(); i++) {
                var container = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
                container.verticalAlignment(VerticalAlignment.CENTER);

                int optionIndex = i;
                final var label = Components.label(TextOps.withFormatting("- ", ChatFormatting.GRAY));

                label.margins(Insets.left(10));

                if (!this.backingOption.detached()) {
                    label.cursorStyle(CursorStyle.HAND);
                    label.mouseEnter().subscribe(() -> label.text(TextOps.withFormatting("x ", ChatFormatting.GRAY)));
                    label.mouseLeave().subscribe(() -> label.text(TextOps.withFormatting("- ", ChatFormatting.GRAY)));
                    label.mouseDown().subscribe((mouseX, mouseY, button) -> {
                        this.backingList.remove(optionIndex);
                        this.refreshResetButton();
                        this.refreshOptions();
                        UISounds.playInteractionSound();

                        return true;
                    });
                }

                var option = backingList.get(i);

                if(option instanceof String) {
                    backingList.set(i, (T) ReflectOps.defaultConstruct(listType));
                }

                var labelContainer = Containers.verticalFlow(Sizing.fixed(20), Sizing.content());

                labelContainer.child(label);

                container.child(labelContainer);
                container.child(ConfigurableStructLayout.of(listType, backingList.get(i), uiModel, this.backingOption.configName(), this.backingOption.key()));

                this.collapsibleChildren.add(container);
            }

            this.contentLayout.<FlowLayout>configure(layout -> {
                layout.clearChildren();
                if (this.expanded) layout.children(this.collapsibleChildren);
            });

            this.refreshResetButton();
        }
    }

    public static class ConfigurableStructLayout<T> extends FlowLayout {

        public final Map<String, FieldHandler<T, ?, ? extends Component>> handlers = new LinkedHashMap<>();

        public final UIModel model;

        public final String configName;
        public final Option.Key optionKey;

        protected ConfigurableStructLayout(Sizing horizontalSizing, Sizing verticalSizing, UIModel uiModel, String configName, Option.Key optionKey) {
            super(horizontalSizing, verticalSizing, Algorithm.VERTICAL);

            this.model = uiModel;

            this.configName = configName;
            this.optionKey = optionKey;
        }

        public ConfigurableStructLayout<T> build(T value) {
            handlers.forEach((s, handler) -> this.child(handler.componentConstructor.apply(value, () -> {})));

            return this;
        }

        public static <T> ConfigurableStructLayout of(Class<T> clazz, Object value, UIModel uiModel, String configName, Option.Key optionKey) {
            var layout = new ConfigurableStructLayout<>(Sizing.expand(), Sizing.content(), uiModel, configName, optionKey);

            for (var field : clazz.getFields()) {
                var fieldClazz = field.getType();

                if(NumberReflection.isNumberType(fieldClazz)) {
                    layout.numberField(field, ReflectOps.get(field, value));
                } else if(fieldClazz == String.class) {
                    layout.stringField(field, ReflectOps.get(field, value));
                } else if(fieldClazz == Identifier.class) {
                    layout.identifierField(field, ReflectOps.get(field, value));
                }
            }

            layout.surface(Surface.outline(Color.WHITE.argb()));

            return layout.build(value);
        }

        public <F extends Number> ConfigurableStructLayout<T> numberField(Field field, F defaultValue) {
            if (field.isAnnotationPresent(RangeConstraint.class)) {
                return rangeControlsHandle(field, defaultValue,
                        NumberReflection.isFloatingPointType(field.getType())
                                ? field.getAnnotation(RangeConstraint.class).decimalPlaces()
                                : 0
                );
            } else {
                return textBoxHandle(field, defaultValue, configTextBox -> {
                    configTextBox.configureForNumber((Class<F>) field.getType());
                });
            }
        }

        public ConfigurableStructLayout<T> stringField(Field field, String defaultValue) {
            return textBoxHandle(field, defaultValue, configTextBox -> {
            });
        }

        public ConfigurableStructLayout<T> identifierField(Field field, Identifier defaultValue) {
            return textBoxHandle(field, defaultValue, configTextBox -> {
                configTextBox.inputPredicate(s -> s.matches("[a-z0-9_.:\\-]*"));
                configTextBox.applyPredicate(s -> ResourceLocation.tryParse(s) != null);
                configTextBox.valueParser(ResourceLocation::parse);
            });
        }

        public <F> ConfigurableStructLayout<T> textBoxHandle(Field field, F defaultValue, Consumer<ConfigTextBox> processor) {
            return textBoxHandle(field, defaultValue, Object::toString, processor);
        }

        public <F> ConfigurableStructLayout<T> textBoxHandle(Field field, F defaultValue, Function<F, String> toStringFunc, Consumer<ConfigTextBox> processor) {
            var name = field.getName();

            this.handlers.put(name, textBoxHandle(model, this.configName, this.optionKey, field, defaultValue, toStringFunc, processor));

            return this;
        }

        public <F extends Number> ConfigurableStructLayout<T> rangeControlsHandle(Field field, F defaultValue, int decimalPlaces) {
            var name = field.getName();
            var translationKey = "text.config." + this.configName + ".option." + this.optionKey.asString() + "." + name;

            Function<T, F> getter = t -> ReflectOps.get(field, t);
            BiConsumer<T, F> setter = (t, f) -> ReflectOps.set(field, t, f);

            this.handlers.put(name,
                    new FieldHandler<T, F, FlowLayout>(name, getter, setter,
                            (t, onChange) -> {
                                boolean withDecimals = decimalPlaces > 0;

                                // ------------
                                // Slider setup
                                // ------------

                                var clazz = (Class<F>) field.getType();

                                var value = getter.apply(t);
                                var optionComponent = model.expandTemplate(FlowLayout.class,
                                        "range-config-option",
                                        OptionComponents.packParameters(translationKey, value.toString())
                                );

                                optionComponent.horizontalSizing(Sizing.expand(100));

                                var constraint = field.getAnnotation(RangeConstraint.class);
                                double min = constraint.min(), max = constraint.max();

                                var sliderInput = optionComponent.childById(ConfigSlider.class, "value-slider");
                                sliderInput.min(min).max(max).decimalPlaces(decimalPlaces).snap(!withDecimals).setFromDiscreteValue(value.doubleValue());
                                sliderInput.valueType(clazz);

                                var resetButton = optionComponent.childById(ButtonComponent.class, "reset-button");

                                resetButton.active = (withDecimals ? value.doubleValue() : Math.round(value.doubleValue())) != defaultValue.doubleValue();
                                resetButton.onPress(button -> {
                                    sliderInput.setFromDiscreteValue(defaultValue.doubleValue());
                                    button.active = false;
                                });

                                sliderInput.onChanged().subscribe(newValue -> {
                                    resetButton.active = (withDecimals ? newValue : Math.round(newValue)) != defaultValue.doubleValue();
                                });

                                // ------------------------------------
                                // Component handles and text box setup
                                // ------------------------------------

                                var sliderControls = optionComponent.childById(FlowLayout.class, "slider-controls");
                                var textControls = textBoxHandle(model, this.configName, this.optionKey, field, defaultValue, Objects::toString, configTextBox -> {
                                    configTextBox.configureForNumber(clazz);

                                    var predicate = configTextBox.applyPredicate();
                                    configTextBox.applyPredicate(predicate.and(s -> {
                                        final var parsed = Double.parseDouble(s);
                                        return parsed >= min && parsed <= max;
                                    }));
                                }).componentConstructor().apply(t, onChange).childById(FlowLayout.class, "controls-flow").positioning(Positioning.layout());
                                var textInput = textControls.childById(ConfigTextBox.class, "value-box");

                                // ------------
                                // Toggle setup
                                // ------------

                                var controlsLayout = optionComponent.childById(FlowLayout.class, "controls-flow");
                                var toggleButton = optionComponent.childById(ButtonComponent.class, "toggle-button");

                                var textMode = new MutableBoolean(false);
                                toggleButton.onPress(button -> {
                                    textMode.setValue(textMode.isFalse());

                                    if (textMode.isTrue()) {
                                        sliderControls.remove();
                                        textInput.text(sliderInput.decimalPlaces() == 0 ? String.valueOf((int) sliderInput.discreteValue()) : String.valueOf(sliderInput.discreteValue()));

                                        controlsLayout.child(textControls);
                                    } else {
                                        textControls.remove();
                                        sliderInput.setFromDiscreteValue(((Number) textInput.parsedValue()).doubleValue());

                                        controlsLayout.child(sliderControls);
                                    }

                                    button.tooltip(textMode.isTrue()
                                            ? net.minecraft.network.chat.Component.translatable("text.owo.config.button.range.edit_with_slider")
                                            : net.minecraft.network.chat.Component.translatable("text.owo.config.button.range.edit_as_text")
                                    );
                                });

//                                optionComponent.child(new SearchAnchorComponent(
//                                        optionComponent,
//                                        option.key(),
//                                        () -> optionComponent.childById(LabelComponent.class, "option-name").text().getString(),
//                                        () -> textMode.isTrue() ? textInput.getText() : sliderInput.getMessage().getString()
//                                ));

                                return optionComponent;

//                                return new OptionComponentFactory.Result<>(optionComponent, new OptionValueProvider() {
//                                    @Override
//                                    public boolean isValid() {
//                                        return textMode.isTrue()
//                                                ? textInput.isValid()
//                                                : sliderInput.isValid();
//                                    }
//
//                                    @Override
//                                    public Object parsedValue() {
//                                        return textMode.isTrue()
//                                                ? textInput.parsedValue()
//                                                : sliderInput.parsedValue();
//                                    }
//                                });
                            },
                            (component, T) -> {
                                setter.accept(T, (F) component.childById(ConfigTextBox.class, "value-box").parsedValue());
                            }
                    ));

            return this;
        }

        private static <T, F> FieldHandler<T, F, FlowLayout> textBoxHandle(UIModel model, String configName, Option.Key optionKey, Field field, F defaultValue, Function<F, String> toStringFunc, Consumer<ConfigTextBox> processor) {
            var name = field.getName();
            var translationKey = "text.config." + configName + ".option." + optionKey.asString() + "." + name;

            Function<T, F> getter = t -> ReflectOps.get(field, t);
            BiConsumer<T, F> setter = (t, f) -> ReflectOps.set(field, t, f);

            return new FieldHandler<>(
                    name, t -> ReflectOps.get(field, t), (t, f) -> ReflectOps.set(field, t, f),
                    (t, onChange) -> {
                        var optionComponent = model.expandTemplate(FlowLayout.class,
                                "text-box-config-option",
                                OptionComponents.packParameters(translationKey, toStringFunc.apply(getter.apply(t)))
                        );

                        //optionComponent.horizontalSizing(Sizing.fill(98));

                        var valueBox = optionComponent.childById(ConfigTextBox.class, "value-box");
                        var resetButton = optionComponent.childById(ButtonComponent.class, "reset-button");

                        resetButton.active = !valueBox.getValue().equals(toStringFunc.apply(defaultValue));
                        resetButton.onPress(button -> {
                            valueBox.setValue(toStringFunc.apply(defaultValue));
                            button.active = false;
                        });

                        var onChanged = valueBox.onChanged();

                        onChanged.subscribe(s -> resetButton.active = !s.equals(toStringFunc.apply(defaultValue)));
                        onChanged.subscribe(s -> onChange.run());

                        processor.accept(valueBox);

                        optionComponent.child(new SearchAnchorComponent(
                                optionComponent,
                                optionKey.child(name),
                                () -> optionComponent.childById(LabelComponent.class, "option-name").text().getString(),
                                valueBox::getValue
                        ));

                        return optionComponent;
                    },
                    (component, T) -> {
                        setter.accept(T, (F) component.childById(ConfigTextBox.class, "value-box").parsedValue());
                    }
            );
        }
    }

    public record FieldHandler<T, F, C extends Component>(String name, Function<T, F> getter, BiConsumer<T, F> setter, BiFunction<T, Runnable, C> componentConstructor, BiConsumer<C, T> parseFunc){}

    private static class ReflectOps {
        public static <F> void set(Field field, Object t, F f){
            try {
                field.set(t, f);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public static <F> F get(Field field, Object t) {
            try {
                return (F) field.get(t);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public static <T> T defaultConstruct(Class<T> clazz){
            try {
                return (T) clazz.getConstructors()[0].newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
