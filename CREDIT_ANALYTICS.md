# Java Credit Analytics Extension

This module extends FinTruth with self-contained Java pricing calculations applicable to introductory credit markets analytics.

## Features

- Fixed-rate bullet bond present value and yield-to-maturity (periodic compounding)
- Z-spread pricing against a user-provided continuously compounded zero curve
- Signed PV01 via a +1bp yield bump
- Constant-hazard survival probabilities and continuous-premium CDS valuation
- Dependency-free numerical tests

## Compile and run (Java 17+)

```bash
mkdir -p out
javac -encoding UTF-8 -d out src/main/java/com/fintruth/credit/*.java src/test/java/com/fintruth/credit/*.java
java -ea -cp out com.fintruth.credit.CreditAnalyticsTest
```

## Scope and limitations

These are educational deterministic analytics, **not** production eTrading models. Bond calculations assume settlement on a coupon date, fixed annual maturity, no day-count conventions, calendar handling, accrued interest, embedded options, or settlement lag. Z-spread uses continuous compounding on provided zero-rate nodes. CDS calculations assume flat hazard and risk-free rates with continuously paid spreads; they do not include ISDA standard-model conventions, step-in dates, fee accrual, term-structured bootstrapping, counterparty credit risk, or market-calibrated spreads. Real-time market data, trading order management, concurrency, and latency benchmarking are not yet implemented.

The separate disclosure-credibility project remains independent. Use the new module to demonstrate Java numerical algorithms, basic credit valuation logic, and testing, rather than claiming the repository is a fully functioning credit eTrading platform.
