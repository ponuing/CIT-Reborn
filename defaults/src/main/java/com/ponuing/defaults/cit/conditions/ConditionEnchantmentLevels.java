package com.ponuing.defaults.cit.conditions;

import net.minecraft.util.Identifier;
import com.ponuing.api.CITConditionContainer;
import com.ponuing.cit.CITCondition;
import com.ponuing.cit.CITContext;
import com.ponuing.cit.builtin.conditions.IntegerCondition;
import com.ponuing.cit.builtin.conditions.ListCondition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConditionEnchantmentLevels extends ListCondition<ConditionEnchantmentLevels.EnchantmentLevelCondition> {
    public static final CITConditionContainer<ConditionEnchantmentLevels> CONTAINER = new CITConditionContainer<>(ConditionEnchantmentLevels.class, ConditionEnchantmentLevels::new,
            "enchantment_levels", "enchantmentLevels");

    protected Set<Identifier> enchantments = null;

    public ConditionEnchantmentLevels() {
        super(EnchantmentLevelCondition.class, EnchantmentLevelCondition::new);
    }

    @Override
    public Set<Class<? extends CITCondition>> siblingConditions() {
        return Set.of(ConditionEnchantments.class);
    }

    @Override
    public <T extends CITCondition> T modifySibling(T sibling) {
        if (sibling instanceof ConditionEnchantments conditionEnchantments) {
            if (enchantments == null) {
                enchantments = new HashSet<>();
                for (EnchantmentLevelCondition subCondition : this.conditions)
                    subCondition.enchantments = enchantments;
            }
            enchantments.addAll(Arrays.asList(conditionEnchantments.getEnchantments()));
        }

        return sibling;
    }

    protected static class EnchantmentLevelCondition extends IntegerCondition {
        protected Set<Identifier> enchantments = null;

        protected EnchantmentLevelCondition() {
            super(true, false, false);
        }

        @Override
        public boolean test(CITContext context) {
            for (Map.Entry<Identifier, Integer> entry : context.enchantments().entrySet())
                if ((enchantments == null || enchantments.contains(entry.getKey())) && entry.getValue() != null && (range ? min <= entry.getValue() && entry.getValue() <= max : entry.getValue() == min))
                    return true;

            return false;
        }
    }
}
