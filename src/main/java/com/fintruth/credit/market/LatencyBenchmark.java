package com.fintruth.credit.market;

import java.util.*;
import java.util.concurrent.*;

/** Synthetic producer throughput and queue-to-quote-update latency benchmark. */
public final class LatencyBenchmark {
    private LatencyBenchmark() {}
    public record Report(long attempts, long accepted, long dropped, long processed, long rejected,
                         double throughputPerSecond, double p50Micros, double p95Micros, double p99Micros) {}

    public static Report run(int symbols, int eventsPerSymbol, int partitions) throws Exception {
        if (symbols < 1 || eventsPerSymbol < 1) throw new IllegalArgumentException("positive workload required");
        ExecutorService producers = Executors.newFixedThreadPool(Math.min(symbols, Runtime.getRuntime().availableProcessors()));
        try (MarketDataEngine engine = new MarketDataEngine(partitions, Math.max(1024, symbols * 512))) {
            long start = System.nanoTime();
            List<Future<?>> futures = new ArrayList<>();
            for (int s = 0; s < symbols; s++) {
                final String symbol = "BOND-" + s;
                futures.add(producers.submit(() -> {
                    for (int i = 0; i < eventsPerSymbol; i++) {
                        double mid = 100.0 + (i % 100) * 0.001;
                        MarketDataEngine.Tick tick = new MarketDataEngine.Tick(symbol, i, mid - 0.01, mid + 0.01, System.nanoTime());
                        while (!engine.publish(tick)) Thread.onSpinWait(); // retry on backpressure
                    }
                }));
            }
            for (Future<?> f : futures) f.get();
            engine.awaitIdle();
            double seconds = (System.nanoTime() - start) / 1e9;
            MarketDataEngine.Stats stats = engine.stats();
            long[] times = engine.latencySamplesNanos().stream().mapToLong(Long::longValue).sorted().toArray();
            return new Report((long) symbols * eventsPerSymbol, stats.accepted(), stats.dropped(), stats.processed(), stats.rejected(),
                    stats.processed() / seconds, percentile(times, .50), percentile(times, .95), percentile(times, .99));
        } finally { producers.shutdownNow(); }
    }
    private static double percentile(long[] sorted, double fraction) {
        if (sorted.length == 0) return Double.NaN;
        int index = (int) Math.ceil(fraction * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(index, sorted.length - 1))] / 1000.0;
    }
    public static void main(String[] args) throws Exception {
        int symbols = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        int events = args.length > 1 ? Integer.parseInt(args[1]) : 10000;
        int partitions = args.length > 2 ? Integer.parseInt(args[2]) : 4;
        Report r = run(symbols, events, partitions);
        System.out.printf(Locale.ROOT,
                "Synthetic benchmark: accepted=%d processed=%d rejected=%d retry_overflows=%d throughput=%.0f events/s p50=%.2f us p95=%.2f us p99=%.2f us%n",
                r.accepted(), r.processed(), r.rejected(), r.dropped(), r.throughputPerSecond(), r.p50Micros(), r.p95Micros(), r.p99Micros());
    }
}
