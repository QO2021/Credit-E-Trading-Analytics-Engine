package com.fintruth.credit;

/** Simplified constant-hazard CDS model with continuously paid premium and flat discount rate. */
public final class CdsAnalytics {
    private CdsAnalytics() {}

    public static double survival(double hazardRate, double years) {
        validate(hazardRate, years);
        return Math.exp(-hazardRate * years);
    }

    /** PV of one unit of running spread, paid until default or maturity. */
    public static double riskyAnnuity(double discountRate, double hazardRate, double years) {
        validate(hazardRate, years);
        double k = discountRate + hazardRate;
        return Math.abs(k) < 1e-12 ? years : -Math.expm1(-k * years) / k;
    }

    /** PV for a protection buyer, excluding upfront costs and accrued premium conventions. */
    public static double protectionBuyerPv(double notional, double runningSpread, double recoveryRate,
                                            double discountRate, double hazardRate, double years) {
        if (!Double.isFinite(notional) || notional <= 0 || !Double.isFinite(runningSpread) ||
                !Double.isFinite(recoveryRate) || recoveryRate < 0 || recoveryRate > 1)
            throw new IllegalArgumentException("Invalid CDS inputs");
        double annuity = riskyAnnuity(discountRate, hazardRate, years);
        return notional * ((1.0 - recoveryRate) * hazardRate - runningSpread) * annuity;
    }

    /** Fair annualized spread under a constant hazard and continuous-premium approximation. */
    public static double fairSpread(double recoveryRate, double hazardRate) {
        if (!Double.isFinite(recoveryRate) || recoveryRate < 0 || recoveryRate > 1 ||
                !Double.isFinite(hazardRate) || hazardRate < 0) throw new IllegalArgumentException("Invalid inputs");
        return (1.0 - recoveryRate) * hazardRate;
    }

    private static void validate(double hazardRate, double years) {
        if (!Double.isFinite(hazardRate) || hazardRate < 0 || !Double.isFinite(years) || years < 0)
            throw new IllegalArgumentException("Invalid hazard rate or maturity");
    }
}
