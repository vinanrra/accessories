package io.wispforest.accessories.criteria;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.data.SlotGroupLoader;
import net.minecraft.Util;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AccessoryChangedCriterion extends SimpleCriterionTrigger<AccessoryChangedCriterion.Conditions> {

    private final ResourceLocation id;

    public AccessoryChangedCriterion(ResourceLocation id) {
        this.id = id;
    }

    public void trigger(ServerPlayer player, ItemStack accessory, SlotReference reference, Boolean cosmetic) {
        this.trigger(player, conditions -> {
            var data = conditions.data;

            return data.itemPredicates().map(predicates -> predicates.stream().allMatch(predicate -> predicate.matches(accessory))).orElse(true)
                    && data.groups().flatMap(groups -> SlotGroupLoader.INSTANCE.findGroup(false, reference.slotName()).map(group -> groups.stream().noneMatch(s -> s.equals(group.name())))).orElse(true)
                    && data.slots().map(slots -> slots.stream().noneMatch(reference.slotName()::equals)).orElse(true)
                    && data.indices().map(indices -> indices.stream().noneMatch(index -> index == reference.slot())).orElse(true)
                    && data.cosmetic().map(isCosmetic -> isCosmetic && cosmetic).orElse(true);
        });
    }

    @Override
    protected Conditions createInstance(JsonObject jsonObject, ContextAwarePredicate contextAwarePredicate, DeserializationContext deserializationContext) {
        var data = Util.getOrThrow(CONDITIONS_DATA_CODEC.decode(JsonOps.INSTANCE, jsonObject), IllegalStateException::new).getFirst();

        return new Conditions(this.getId(), contextAwarePredicate, data);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    public static final Codec<ItemPredicate> ITEM_PREDICATE_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ItemPredicate, T>> decode(DynamicOps<T> ops, T input) {
            var json = ops.convertTo(JsonOps.INSTANCE, input);

            try {
                return DataResult.success(new Pair<>(ItemPredicate.fromJson(json), input));
            } catch (JsonSyntaxException e) {
                return DataResult.error(e::getMessage);
            }
        }

        @Override
        public <T> DataResult<T> encode(ItemPredicate input, DynamicOps<T> ops, T prefix) {
            return DataResult.success(JsonOps.INSTANCE.convertTo(ops, input.serializeToJson()));
        }
    };

    public static final Codec<ConditionsData> CONDITIONS_DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEM_PREDICATE_CODEC.listOf().optionalFieldOf("items").forGetter(ConditionsData::itemPredicates),
            Codec.STRING.listOf().optionalFieldOf("groups").forGetter(ConditionsData::groups),
            Codec.STRING.listOf().optionalFieldOf("slots").forGetter(ConditionsData::slots),
            Codec.INT.listOf().optionalFieldOf("indices").forGetter(ConditionsData::indices),
            Codec.BOOL.optionalFieldOf("cosmetic").forGetter(ConditionsData::cosmetic)
    ).apply(instance, ConditionsData::new));

    public record ConditionsData(Optional<List<ItemPredicate>> itemPredicates, Optional<List<String>> groups, Optional<List<String>> slots, Optional<List<Integer>> indices, Optional<Boolean> cosmetic){}

    public static final class Conditions extends AbstractCriterionTriggerInstance {

        private final ConditionsData data;

        public Conditions(ResourceLocation id, ContextAwarePredicate player, ConditionsData data) {
            super(id, player);
            this.data = data;
        }
    }
}