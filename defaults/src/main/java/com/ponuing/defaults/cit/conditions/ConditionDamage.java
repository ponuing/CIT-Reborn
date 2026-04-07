package com.ponuing.defaults.cit.conditions;

import com.ponuing.api.CITConditionContainer;
import com.ponuing.cit.CITCondition;
import com.ponuing.cit.CITContext;
import com.ponuing.cit.builtin.conditions.IntegerCondition;

import java.util.Set;

public class ConditionDamage extends IntegerCondition {
    public static final CITConditionContainer<ConditionDamage> CONTAINER = new CITConditionContainer<>(ConditionDamage.class, ConditionDamage::new,
            "damage");

    protected Integer mask = null;

    public ConditionDamage() {
        super(true, false, true);
    }

    @Override
    protected int getValue(CITContext context) {
        int value = context.stack.isDamageable() ? context.stack.getDamage() : 0;
        if (mask != null)
            value &= mask;
        return value;
    }

    @Override
    protected int getPercentageTotalValue(CITContext context) {
        return context.stack.isDamageable() ? context.stack.getMaxDamage() : 0;
    }

    @Override
    public Set<Class<? extends CITCondition>> siblingConditions() {
        return Set.of(ConditionDamageMask.class);
    }

    @Override
    public <T extends CITCondition> T modifySibling(T sibling) {
        if (sibling instanceof ConditionDamageMask damageMask)
            this.mask = damageMask.getMask();
        return null;
    }
}
