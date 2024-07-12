package io.wispforest.accessories.compat.owo;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.RangeConstraint;
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
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.intellij.lang.annotations.Identifier;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class StructListOptionContainer<T> extends ListOptionContainer<T> {

    private final UIModel uiModel;

    public StructListOptionContainer(UIModel uiModel, Option option) {
        super(option);

        this.uiModel = uiModel;

        this.refreshOptions();
    }

    @Override
    protected void refreshOptions() {
        if (uiModel == null) return;

        this.collapsibleChildren.clear();

        var listType = (Class<T>) ReflectionUtils.getTypeArgument(this.backingOption.backingField().field().getGenericType(), 0);
        for (int i = 0; i < this.backingList.size(); i++) {
            var container = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            container.verticalAlignment(VerticalAlignment.CENTER);

            int optionIndex = i;
            final var label = Components.label(TextOps.withFormatting("- ", ChatFormatting.GRAY));

            label.margins(Insets.left(6)); //10

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

            if (option instanceof String) {
                backingList.set(i, (T) ReflectOps.defaultConstruct(listType));
            }

            var labelContainer = Containers.verticalFlow(Sizing.fixed(19), Sizing.content());

            labelContainer.child(label);

            container.child(labelContainer);
            container.child(ConfigurableStructLayout.of(listType, backingList.get(i), uiModel, this.backingOption, true));

            this.collapsibleChildren.add(container);
        }

        this.contentLayout.<FlowLayout>configure(layout -> {
            layout.clearChildren();
            if (this.expanded) layout.children(this.collapsibleChildren);
        });

        this.refreshResetButton();
    }

    public static class ConfigurableStructLayout<T> extends FlowLayout {

        public final Map<Field, ComponentFactory<T, ?>> handlers = new LinkedHashMap<>();

        public final boolean isMap;

        public final UIModel model;

        public final String configName;
        public final Option.Key optionKey;

        protected ConfigurableStructLayout(Sizing horizontalSizing, Sizing verticalSizing, UIModel uiModel, Option<?> option, boolean isMap) {
            super(horizontalSizing, verticalSizing, isMap ? Algorithm.HORIZONTAL : Algorithm.VERTICAL);

            this.isMap = isMap;

            this.model = uiModel;

            this.configName = option.configName();
            this.optionKey = option.key();
        }

        public ConfigurableStructLayout<T> build(T value) {
            handlers.forEach((field, handler) -> {
                var component = new MutableObject<Component>();

                var name = field.getName();
                var translationKey = "text.config." + configName + ".option." + optionKey.asString() + "." + name;

                component.setValue(handler.createComponent(value, field, t -> ReflectOps.get(field, t), (t, f) -> ReflectOps.set(field, t, f), translationKey, this));

                this.child(component.getValue());
            });

            return this;
        }

        public static <T> ConfigurableStructLayout of(Class<T> clazz, Object value, UIModel uiModel, Option<?> option, boolean isMap) {
            var layout = new ConfigurableStructLayout<>(Sizing.expand(), Sizing.content(), uiModel, option, isMap);

            for (var field : clazz.getFields()) {
                var fieldClazz = field.getType();

                if (NumberReflection.isNumberType(fieldClazz)) {
                    layout.numberField(field, ReflectOps.get(field, value));
                } else if (fieldClazz == String.class) {
                    layout.stringField(field, ReflectOps.get(field, value));
                } else if (fieldClazz == ResourceLocation.class) {
                    layout.identifierField(field, ReflectOps.get(field, value));
                } else {
                    throw new IllegalArgumentException("Unable to handle the given field type found within the struct class! [ParentClass: " + clazz.getSimpleName() + ", FieldName: " + field.getName() +"]");
                }
            }

            //if(isMap) layout.padding(Insets.right(1));

            //layout.surface(Surface.outline(Color.WHITE.argb()));

            return layout.build((T) value);
        }

        public <F extends Number> ConfigurableStructLayout<T> numberField(Field field, F defaultValue) {
            if (field.isAnnotationPresent(RangeConstraint.class)) {
                return rangeControlsHandle(field, defaultValue,
                        NumberReflection.isFloatingPointType(field.getType())
                                ? field.getAnnotation(RangeConstraint.class).decimalPlaces()
                                : 0
                );
            }

            return textBoxHandle(field, defaultValue, configTextBox -> configTextBox.configureForNumber((Class<F>) field.getType()));
        }

        public ConfigurableStructLayout<T> stringField(Field field, String defaultValue) {
            return textBoxHandle(field, defaultValue, configTextBox -> {});
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
            this.handlers.put(field, textBoxFactory(defaultValue, toStringFunc, processor));

            return this;
        }

        public <F extends Number> ConfigurableStructLayout<T> rangeControlsHandle(Field field, F defaultValue, int decimalPlaces) {
            this.handlers.put(field, (ComponentFactory<T, F>) (t, field1, getter, setter, translationKey, parentComponent) -> {
                var name = field1.getName();

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

                if(isMap) optionComponent.horizontalSizing(Sizing.expand(50));

                var constraint = field.getAnnotation(RangeConstraint.class);
                double min = constraint.min(), max = constraint.max();

                var sliderInput = optionComponent.childById(ConfigSlider.class, "value-slider");

                var fieldId = sliderInput.id() + "-" + name;

                sliderInput.id(fieldId);

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

                sliderInput.onChanged().subscribe(newValue -> {
                    setter.accept(t, (F) parentComponent.childById(ConfigSlider.class, fieldId).parsedValue());
                });

                // ------------------------------------
                // Component handles and text box setup
                // ------------------------------------

                var sliderControls = optionComponent.childById(FlowLayout.class, "slider-controls");
                var textControls = (ParentComponent) textBoxFactory(defaultValue, Objects::toString, configTextBox -> {
                    configTextBox.configureForNumber(clazz);

                    var predicate = configTextBox.applyPredicate();
                    configTextBox.applyPredicate(predicate.and(s -> {
                        final var parsed = Double.parseDouble(s);
                        return parsed >= min && parsed <= max;
                    }));
                }).createComponent(t, field, getter, setter, translationKey, parentComponent);

                textControls.childById(FlowLayout.class, "controls-flow").positioning(Positioning.layout());

                var textInput = textControls.childById(ConfigTextBox.class, "value-box-" + name);

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

                return optionComponent;
            });

            return this;
        }

        private <F> ComponentFactory<T, F> textBoxFactory(F defaultValue, Function<F, String> toStringFunc, Consumer<ConfigTextBox> processor) {
            return (t, field1, getter, setter, translationKey, parentComponent) -> {
                var optionComponent = model.expandTemplate(FlowLayout.class,
                        "text-box-config-option",
                        OptionComponents.packParameters(translationKey, toStringFunc.apply(getter.apply(t)))
                );

                if(isMap) optionComponent.horizontalSizing(Sizing.expand(50));

                var valueBox = optionComponent.childById(ConfigTextBox.class, "value-box");
                var resetButton = optionComponent.childById(ButtonComponent.class, "reset-button");

                var fieldId = valueBox.id() + "-" + field1.getName();

                if(isMap) valueBox.horizontalSizing(Sizing.fixed(Math.round(valueBox.horizontalSizing().get().value / 1.5f)));

                valueBox.id(fieldId);

                resetButton.active = !valueBox.getValue().equals(toStringFunc.apply(defaultValue));
                resetButton.onPress(button -> {
                    valueBox.setValue(toStringFunc.apply(defaultValue));
                    button.active = false;
                });

                var onChanged = valueBox.onChanged();

                onChanged.subscribe(s -> resetButton.active = !s.equals(toStringFunc.apply(defaultValue)));
                onChanged.subscribe(s -> {
                    setter.accept(t, (F) parentComponent.childById(ConfigTextBox.class, fieldId).parsedValue());
                });

                processor.accept(valueBox);

                optionComponent.child(new SearchAnchorComponent(
                        optionComponent,
                        optionKey.child(field1.getName()),
                        () -> optionComponent.childById(LabelComponent.class, "option-name").text().getString(),
                        valueBox::getValue
                ));

                return optionComponent;
            };
        }
    }

    public interface ComponentFactory<T, F> {
        Component createComponent(T t, Field field, Function<T, F> getter, BiConsumer<T, F> setter, String translation, ParentComponent parentComponent);
    }
}
