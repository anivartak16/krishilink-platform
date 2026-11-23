package com.krishilink.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Rupee amounts: two decimal places, half-up rounding. */
public final class Money {

    public static final int SCALE = 2;

    private Money() {
    }

    public static BigDecimal of(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal total(BigDecimal pricePerUnit, double quantity) {
        return of(pricePerUnit.multiply(BigDecimal.valueOf(quantity)));
    }
}
