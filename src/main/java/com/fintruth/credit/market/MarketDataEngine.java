package com.fintruth.credit.market;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Bounded partitioned synthetic market-data processor; NOT exchange connectivity. */
public final class MarketDataEngine implements AutoCloseable {
    public record Tick(String symbol, long sequence, double bid, double ask, long sourceTimestampNanos) {
        public Tick {
            if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol required");
            if (sequence < 0 || !Double.isFinite(bid) || !Double.isFinite(ask) || bid <= 0 || ask <= 0 || bid > ask)
                throw new IllegalArgumentException("Invalid market-data tick");
        }
    }
    public record Quote(String symbol, long sequence, double bid, double ask, double mid, long updatedAtNanos) {}
    public record Stats(long accepted, long rejected, long dropped, long processed) {}
    private record Envelope(Tick tick, long acceptedAtNanos, CompletableFuture<Void> barrier) {}
    private final List<ArrayBlockingQueue<Envelope>> queues = new ArrayList<>();
    private final List<Thread> workers = new ArrayList<>();
    private final ConcurrentHashMap<String, Quote> quotes = new ConcurrentHashMap<>();
    private final LongAdder accepted = new LongAdder(), rejected = new LongAdder(), dropped = new LongAdder(), processed = new LongAdder();
    private final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean open = new AtomicBoolean(true);

    public MarketDataEngine(int partitions, int capacityPerPartition) {
        if (partitions < 1 || capacityPerPartition < 1) throw new IllegalArgumentException("positive partitions and capacity required");
        for (int i = 0; i < partitions; i++) {
            ArrayBlockingQueue<Envelope> queue = new ArrayBlockingQueue<>(capacityPerPartition);
            queues.add(queue);
            Thread worker = new Thread(() -> runLoop(queue), "market-data-" + i);
            worker.start();
            workers.add(worker);
        }
    }
    private int partition(String symbol) { return Math.floorMod(symbol.hashCode(), queues.size()); }

    /** Non-blocking: false means queue overflow; caller may retry. */
    public boolean publish(Tick tick) {
        Objects.requireNonNull(tick);
        if (!open.get()) throw new IllegalStateException("Engine closed");
        boolean enqueued = queues.get(partition(tick.symbol())).offer(new Envelope(tick, System.nanoTime(), null));
        if (enqueued) accepted.increment(); else dropped.increment();
        return enqueued;
    }
    private void runLoop(ArrayBlockingQueue<Envelope> queue) {
        try {
            while (true) {
                Envelope e = queue.take();
                if (e.tick() == null) { e.barrier().complete(null); continue; }
                Tick t = e.tick();
                final boolean[] updated = {false};
                quotes.compute(t.symbol(), (symbol, current) -> {
                    if (current != null && t.sequence() <= current.sequence()) return current;
                    updated[0] = true;
                    return new Quote(symbol, t.sequence(), t.bid(), t.ask(), (t.bid() + t.ask()) / 2.0, System.nanoTime());
                });
                if (updated[0]) processed.increment(); else rejected.increment();
                latencies.add(System.nanoTime() - e.acceptedAtNanos());
            }
        } catch (InterruptedException stop) { Thread.currentThread().interrupt(); }
    }

    public Map<String, Quote> snapshot() { return Map.copyOf(quotes); }
    public Optional<Quote> quote(String symbol) { return Optional.ofNullable(quotes.get(symbol)); }
    public Stats stats() { return new Stats(accepted.sum(), rejected.sum(), dropped.sum(), processed.sum()); }
    public List<Long> latencySamplesNanos() { return List.copyOf(latencies); }

    /** Call after producers have completed, to ensure previously enqueued ticks are processed. */
    public void awaitIdle() throws InterruptedException, ExecutionException {
        List<CompletableFuture<Void>> completions = new ArrayList<>();
        for (ArrayBlockingQueue<Envelope> q : queues) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            q.put(new Envelope(null, 0, future));
            completions.add(future);
        }
        for (CompletableFuture<Void> completion : completions) completion.get();
    }

    @Override public void close() {
        if (!open.compareAndSet(true, false)) return;
        try { awaitIdle(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (ExecutionException e) { throw new IllegalStateException(e); }
        workers.forEach(Thread::interrupt);
        for (Thread worker : workers) {
            try { worker.join(5000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
    }
}
