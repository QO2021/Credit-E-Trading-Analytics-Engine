package com.fintruth.credit;

import java.util.Arrays;

/** Dependency-free assertions; enable with java -ea. */
public final class CreditAnalyticsTest {
    private CreditAnalyticsTest() {}
    public static void main(String[] args) {
        double par = BondAnalytics.dirtyPrice(100.0, 0.05, 5, 2, 0.05);
        assert Math.abs(par - 100.0) < 1e-10 : "At-par fixed rate bond";

        double discounted = BondAnalytics.dirtyPrice(100, 0.05, 5, 2, 0.06);
        assert discounted < 100;
        assert Math.abs(BondAnalytics.yieldToMaturity(discounted, 100, 0.05, 5, 2) - 0.06) < 1e-9;
        assert BondAnalytics.pv01Signed(100, 0.05, 5, 2, 0.05) < 0;

        double[] zeros = new double[10];
        Arrays.fill(zeros, 0.04);
        double target = BondAnalytics.priceFromZeroCurve(100, 0.05, 5, 2, zeros, 0.012);
        assert Math.abs(BondAnalytics.zSpread(target, 100, 0.05, 5, 2, zeros) - 0.012) < 1e-9;

        double fair = CdsAnalytics.fairSpread(0.40, 0.02);
        assert Math.abs(fair - 0.012) < 1e-12;
        assert Math.abs(CdsAnalytics.protectionBuyerPv(1_000_000, fair, 0.40, 0.03, 0.02, 5)) < 1e-7;
        assert CdsAnalytics.protectionBuyerPv(1_000_000, 0.01, 0.40, 0.03, 0.02, 5) > 0;
        System.out.println("Credit analytics tests passed.");
    }
}
