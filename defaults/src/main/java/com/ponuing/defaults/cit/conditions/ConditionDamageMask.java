package com.ponuing.defaults.cit.conditions;

import com.ponuing.api.CITConditionContainer;
import com.ponuing.cit.CITCondition;
import com.ponuing.cit.CITContext;
import com.ponuing.cit.builtin.conditions.IntegerCondition;

import java.util.Set;

public class ConditionDamageMask extends IntegerCondition {
    public static final CITConditionContainer<ConditionDamageMask> CONTAINER = new CITConditionContainer<>(ConditionDamageMask.class, ConditionDamageMask::new,
            "damage_mask", "damageMask");

    public ConditionDamageMask() {
        super(false, false, false);
    }

    @Override
    protected int getValue(CITContext context) {
        return 0;
    }

    @Override
    public boolean test(CITContext context) {
        return true;
    }

    public int getMask() {
        return this.min;
    }

    @Override
    public Set<Class<? extends CITCondition>> siblingConditions() {
        return Set.of(ConditionDamage.class);
    }
}
