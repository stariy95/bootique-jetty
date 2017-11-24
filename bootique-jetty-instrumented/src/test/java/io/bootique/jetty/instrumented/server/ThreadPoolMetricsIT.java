package io.bootique.jetty.instrumented.server;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.bootique.BQRuntime;
import io.bootique.jetty.instrumented.unit.InstrumentedJettyApp;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ThreadPoolMetricsIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolMetricsIT.class);

    @Rule
    public InstrumentedJettyApp app = new InstrumentedJettyApp();

    @Test
    public void testUtilizationVsMax_2() throws InterruptedException {

        new ThreadPoolTester(app)
                .sendRequests(2)
                .unblockAfterInProgressRequests(2)
                .afterStartup(r -> checkUtilizationVsMax(r, 0))
                .afterRequestsFrozen(r -> checkUtilizationVsMax(r, 2))
                .run("classpath:threads20.yml");
    }

    @Test
    public void testUtilizationVsMax_1() throws InterruptedException {

        new ThreadPoolTester(app)
                .sendRequests(1)
                .unblockAfterInProgressRequests(1)
                .afterStartup(r -> checkUtilizationVsMax(r, 0))
                .afterRequestsFrozen(r -> checkUtilizationVsMax(r, 1))
                .run("classpath:threads20.yml");
    }

    @Test
    public void testQueuedRequests_1() throws InterruptedException {

        new ThreadPoolTester(app)
                .sendRequests(4)
                .unblockAfterInProgressRequests(3)
                .afterStartup(r -> checkQueued(r, 0))
                .afterRequestsFrozen(r -> checkQueued(r, 1))
                .run("classpath:threads7.yml");
    }

    @Test
    public void testQueuedRequests_4() throws InterruptedException {

        new ThreadPoolTester(app)
                .sendRequests(7)
                .unblockAfterInProgressRequests(3)
                .afterStartup(r -> checkQueued(r, 0))
                .afterRequestsFrozen(r -> checkQueued(r, 4))
                .run("classpath:threads7.yml");
    }

    private void checkQueued(BQRuntime runtime, int frozenRequests) {
        Gauge<Integer> gauge = findQueuedRequestsGauge(runtime);
        assertWithRetry(() -> assertEquals(Integer.valueOf(frozenRequests), gauge.getValue()));
    }

    private void checkUtilizationVsMax(BQRuntime runtime, int frozenRequests) {
        Gauge<Double> gauge = findUtilizationVsMaxGauge(runtime);

        // utilizationMax = (acceptorTh + selectorTh + active) / max
        // see more detailed explanation in InstrumentedQueuedThreadPool
        assertWithRetry(() -> assertEquals((2 + 3 + frozenRequests) / 20d, gauge.getValue(), 0.0001));
    }

    private void assertWithRetry(Runnable test) {

        for (int i = 2; i > 0; i--) {

            try {
                test.run();
                return;
            } catch (AssertionError e) {
                LOGGER.info("Test condition hasn't been reached, will retry {} more time(s)", i);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                }
            }
        }

        // fail for real
        test.run();
    }

    private Gauge<Double> findUtilizationVsMaxGauge(BQRuntime runtime) {
        return findGauge(Double.class, runtime, "utilization-max");
    }

    private Gauge<Integer> findQueuedRequestsGauge(BQRuntime runtime) {
        return findGauge(Integer.class, runtime, "queued-requests");
    }

    private <T> Gauge<T> findGauge(Class<T> type, BQRuntime runtime, String label) {

        MetricRegistry registry = runtime.getInstance(MetricRegistry.class);
        String name = MetricRegistry.name(QueuedThreadPool.class, "bootique-http", label);

        Collection<Gauge> gauges = registry.getGauges((n, m) -> name.equals(n)).values();
        assertEquals("Unexpected number of gauges for " + name, 1, gauges.size());
        return gauges.iterator().next();
    }
}
