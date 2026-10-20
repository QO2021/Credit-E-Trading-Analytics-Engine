# Credit eTrading Analytics Engine (Java)

A Java 17+ quantitative credit analytics prototype designed as a foundation for Credit eTrading Quant Developer portfolio work.

## Implemented

- Fixed-rate bullet bond pricing with periodic coupon cash flows
- Yield-to-maturity solver using bisection
- Z-spread valuation against a supplied zero-rate curve
- Signed PV01 based on a one-basis-point yield shock
- Simplified constant-hazard CDS survival, fair-spread and protection-buyer valuation
- Dependency-free numerical tests

## Build and test

From the repository root with JDK 17 or newer:

```bash
mkdir -p out
javac -encoding UTF-8 -d out src/main/java/com/fintruth/credit/*.java src/test/java/com/fintruth/credit/*.java
java -ea -cp out com.fintruth.credit.CreditAnalyticsTest
```

Expected output: `Credit analytics tests passed.`

On Windows Command Prompt, replace `mkdir -p out` with `mkdir out` if necessary.

See [CREDIT_ANALYTICS.md](CREDIT_ANALYTICS.md) for numerical assumptions and limitations.

## Project status

Educational quantitative pricing prototype, not a production eTrading platform. No market data streaming, order processing, concurrency benchmarks, ISDA-calibrated CDS curves, or production conventions are implemented yet.
