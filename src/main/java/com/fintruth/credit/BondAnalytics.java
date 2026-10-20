package com.fintruth.credit;

import java.util.function.DoubleUnaryOperator;

/** Plain-vanilla fixed-coupon bond analytics with deterministic annual periods. */
public final class BondAnalytics {
    private BondAnalytics() {}

    public static double cleanPrice(double face, double couponRate, int years, int paymentsPerYear, double annualYield) {
        return dirtyPrice(face, couponRate, years, paymentsPerYear, annualYield);
    }

    /** Assumes settlement on a coupon date, so accrued interest is zero. */
    public static double dirtyPrice(double face, double couponRate, int years, int paymentsPerYear, double annualYield) {
        validate(face, couponRate, years, paymentsPerYear);
        if (1.0 + annualYield / paymentsPerYear <= 0) throw new IllegalArgumentException("Invalid yield");
        int count = Math.multiplyExact(years, paymentsPerYear);
        double coupon = face * couponRate / paymentsPerYear;
        double price = 0.0;
        for (int i = 1; i <= count; i++) {
            double cash = coupon + (i == count ? face : 0.0);
            price += cash / Math.pow(1.0 + annualYield / paymentsPerYear, i);
        }
        return price;
    }

    public static double yieldToMaturity(double targetPrice, double face, double couponRate, int years, int frequency) {
        if (!(targetPrice > 0)) throw new IllegalArgumentException("Price must be positive");
        double lower = -0.999 * frequency;
        double upper = 1.0;
        while (dirtyPrice(face, couponRate, years, frequency, upper) > targetPrice && upper < 10000) upper *= 2.0;
        return bisect(y -> dirtyPrice(face, couponRate, years, frequency, y) - targetPrice, lower, upper);
    }

    /** Parallel continuously compounded zero-curve shift. Input zeroRates[i-1] is the rate for payment i. */
    public static double priceFromZeroCurve(double face, double couponRate, int years, int frequency,
                                            double[] zeroRates, double zSpread) {
        validate(face, couponRate, years, frequency);
        int n = Math.multiplyExact(years, frequency);
        if (zeroRates.length != n) throw new IllegalArgumentException("One zero rate required per cash flow");
        double pv = 0;
        for (int i = 1; i <= n; i++) {
            double t = (double) i / frequency;
            double amount = face * couponRate / frequency + (i == n ? face : 0);
            pv += amount * Math.exp(-(zeroRates[i - 1] + zSpread) * t);
        }
        return pv;
    }

    public static double zSpread(double targetPrice, double face, double couponRate,
                                 int years, int frequency, double[] zeroRates) {
        if (!(targetPrice > 0)) throw new IllegalArgumentException("Price must be positive");
        DoubleUnaryOperator residual = z -> priceFromZeroCurve(face, couponRate, years, frequency, zeroRates, z) - targetPrice;
        double low = -1, high = 1;
        while (residual.applyAsDouble(low) < 0) low *= 2;
        while (residual.applyAsDouble(high) > 0) high *= 2;
        return bisect(residual, low, high);
    }

    /** Price change for an upward one basis point yield shift (typically negative). */
    public static double pv01Signed(double face, double couponRate, int years, int frequency, double annualYield) {
        return dirtyPrice(face, couponRate, years, frequency, annualYield + 0.0001)
                - dirtyPrice(face, couponRate, years, frequency, annualYield);
    }

    private static void validate(double face, double couponRate, int years, int frequency) {
        if (!Double.isFinite(face) || face <= 0 || !Double.isFinite(couponRate) || couponRate < 0 ||
                years <= 0 || frequency <= 0) throw new IllegalArgumentException("Invalid bond inputs");
    }

    private static double bisect(DoubleUnaryOperator f, double lo, double hi) {
        if (!(f.applyAsDouble(lo) >= 0 && f.applyAsDouble(hi) <= 0))
            throw new IllegalArgumentException("Root is not bracketed");
        for (int i = 0; i < 180; i++) {
            double mid = lo + (hi - lo) / 2.0;
            if (f.applyAsDouble(mid) > 0) lo = mid; else hi = mid;
        }
        return lo + (hi - lo) / 2.0;
    }
}
