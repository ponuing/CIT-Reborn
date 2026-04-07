package com.ponuing.defaults.cit.conditions;

import net.minecraft.util.Identifier;
import com.ponuing.api.CITConditionContainer;
import com.ponuing.cit.CITCondition;
import com.ponuing.cit.CITContext;
import com.ponuing.cit.builtin.conditions.IdentifierCondition;
import com.ponuing.cit.builtin.conditions.ListCondition;
import com.ponuing.cit.CITParsingException;
import com.ponuing.pack.format.PropertyGroup;
import com.ponuing.pack.format.PropertyKey;
import com.ponuing.pack.format.PropertyValue;

import java.util.Set;

public class ConditionEnchantments extends ListCondition<ConditionEnchantments.EnchantmentCondition> {
    public static final CITConditionContainer<ConditionEnchantments> CONTAINER = new CITConditionContainer<>(ConditionEnchantments.class, ConditionEnchantments::new,
            "enchantments", "enchantmentIDs");

    public ConditionEnchantments() {
        super(EnchantmentCondition.class, EnchantmentCondition::new);
    }

    public Identifier[] getEnchantments() {
        Identifier[] enchantments = new Identifier[this.conditions.length];

        for (int i = 0; i < this.conditions.length; i++)
            enchantments[i] = this.conditions[i].getValue(null);

        return enchantments;
    }

    @Override
    public Set<Class<? extends CITCondition>> siblingConditions() {
        return Set.of(ConditionEnchantmentLevels.class);
    }

    protected static class EnchantmentCondition extends IdentifierCondition {
        @Override
        public boolean test(CITContext context) {
            return context.enchantments().containsKey(this.value);
        }

        @Override
        protected Identifier getValue(CITContext context) {
            return this.value;
        }
    }
}
