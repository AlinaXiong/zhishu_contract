package com.hero.middleware.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class MoneyUtils {
    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private MoneyUtils() {}

    public static BigDecimal toDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        String text = String.valueOf(value).trim().replace(",", "");
        return text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text);
    }

    public static BigDecimal toMoney(Object value) {
        return toDecimal(value).setScale(MONEY_SCALE, ROUNDING);
    }

    public static BigDecimal toRate(Object value) {
        BigDecimal rate = toDecimal(value).setScale(RATE_SCALE, ROUNDING);
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("汇率必须大于0");
        }
        return rate;
    }

    /** 默认：exchangeRate 表示 sourceCurrency -> targetCurrency，目标金额 = 源金额 * 汇率 */
    public static BigDecimal convert(Object amount, String sourceCurrency, String targetCurrency, Object exchangeRate) {
        BigDecimal sourceAmount = toDecimal(amount);
        if (sameCurrency(sourceCurrency, targetCurrency)) {
            return sourceAmount.setScale(MONEY_SCALE, ROUNDING);
        }
        return sourceAmount.multiply(toRate(exchangeRate)).setScale(MONEY_SCALE, ROUNDING);
    }

    /** 反向汇率：目标金额 = 源金额 / 汇率 */
    public static BigDecimal convertByReverseRate(Object amount, String sourceCurrency, String targetCurrency, Object exchangeRate) {
        BigDecimal sourceAmount = toDecimal(amount);
        if (sameCurrency(sourceCurrency, targetCurrency)) {
            return sourceAmount.setScale(MONEY_SCALE, ROUNDING);
        }
        return sourceAmount.divide(toRate(exchangeRate), MONEY_SCALE, ROUNDING);
    }

    public static boolean moneyEquals(Object left, Object right) {
        return toMoney(left).compareTo(toMoney(right)) == 0;
    }

    private static boolean sameCurrency(String left, String right) {
        return Objects.equals(normalizeCurrency(left), normalizeCurrency(right));
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? "" : currency.trim().toUpperCase();
    }
}
