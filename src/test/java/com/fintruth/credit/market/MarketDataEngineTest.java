package com.fintruth.credit.market;

public final class MarketDataEngineTest {
    public static void main(String[] args) throws Exception {
        try (MarketDataEngine engine = new MarketDataEngine(2, 128)) {
            assert engine.publish(new MarketDataEngine.Tick("CREDIT-A", 10, 99.0, 101.0, 1));
            assert engine.publish(new MarketDataEngine.Tick("CREDIT-A", 9, 99.0, 100.0, 2));
            assert engine.publish(new MarketDataEngine.Tick("CREDIT-A", 11, 100.0, 102.0, 3));
            assert engine.publish(new MarketDataEngine.Tick("CREDIT-B", 1, 98.0, 100.0, 4));
            engine.awaitIdle();
            assert engine.quote("CREDIT-A").orElseThrow().sequence() == 11;
            assert engine.quote("CREDIT-A").orElseThrow().mid() == 101.0;
            assert engine.stats().processed() == 3;
            assert engine.stats().rejected() == 1;
            assert engine.snapshot().size() == 2;
            assert engine.latencySamplesNanos().size() == 4;
            assert engine.stats().accepted() == 4;
        }
        boolean crossed = false;
        try { new MarketDataEngine.Tick("X", 1, 101, 100, 0); }
        catch (IllegalArgumentException ok) { crossed = true; }
        assert crossed;
        MarketDataEngine closedEngine = new MarketDataEngine(1, 8);
        closedEngine.close();
        boolean closed = false;
        try { closedEngine.publish(new MarketDataEngine.Tick("X", 1, 99, 100, 0)); }
        catch (IllegalStateException ok) { closed = true; }
        assert closed;
        LatencyBenchmark.Report report = LatencyBenchmark.run(4, 500, 2);
        assert report.processed() == 2000;
        assert report.rejected() == 0;
        assert report.p95Micros() >= 0;
        System.out.println("Market data and latency benchmark tests passed.");
    }
}
