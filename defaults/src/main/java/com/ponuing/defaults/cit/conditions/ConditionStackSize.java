package com.ponuing.defaults.cit.conditions;

import com.ponuing.api.CITConditionContainer;
import com.ponuing.cit.CITContext;
import com.ponuing.cit.builtin.conditions.IntegerCondition;

public class ConditionStackSize extends IntegerCondition {
    public static final CITConditionContainer<ConditionStackSize> CONTAINER = new CITConditionContainer<>(ConditionStackSize.class, ConditionStackSize::new,
            "stack_size", "stackSize", "amount");

    public ConditionStackSize() {
        super(true, false, false);
    }

    @Override
    protected int getValue(CITContext context) {
        return context.stack.getCount();
    }
}
