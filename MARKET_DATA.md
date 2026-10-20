# Concurrent Java Market Data and Quote Management

Java 17+ dependency-free synthetic streaming research module.

## Features
- Concurrent producer submission of bid/ask updates
- Bounded ArrayBlockingQueue partitioned by symbol, with one worker thread per partition
- Per-symbol monotonic sequence checks rejecting out-of-order or duplicate ticks
- Bid/ask validation rejecting crossed or nonpositive quotes
- Immutable snapshots and the latest mid-price by instrument
- Queue overflow counters and explicit retry behavior in benchmark
- End-to-end queue-to-processing latency distribution (p50/p95/p99), throughput

## Run on Java 17+

```bash
mkdir -p out
javac -encoding UTF-8 -d out src/main/java/com/fintruth/credit/market/*.java src/test/java/com/fintruth/credit/market/*.java
java -ea -cp out com.fintruth.credit.market.MarketDataEngineTest
java -cp out com.fintruth.credit.market.LatencyBenchmark 8 10000 4
```

Use `-ea` when running tests; otherwise Java assertion checks are disabled.

## Benchmark interpretation

This measures JVM-local generated ticks processed through bounded queues; it does not measure market network-to-wire latency. Each successful `publish` adds a timestamp and measures until quote update finishes. p50, p95 and p99 are derived from sampled elapsed nanoseconds. Timing depends heavily on machine, load, JVM and heap/GC. The benchmark intentionally retries on full queues (spin wait); the reported `retry_overflows` counts rejected enqueue attempts, **not lost final events**. Warm-up, JMH calibration, CPU pinning, GC tuning, and realistic market-feed replay are not implemented.

## Limitations and future work

There is no live exchange feed, FIX connection, market-data entitlement, persistent event log, executable order routing, order matching, recovery from gaps, sequence reset or reconnect, or credit instrument reference-data integration. These features should not be represented as implemented. Thread-safe quote state does not guarantee cross-symbol atomic snapshots. The worker lifecycle assumes no concurrent producers during shutdown. Avoid using this prototype to place live orders.
