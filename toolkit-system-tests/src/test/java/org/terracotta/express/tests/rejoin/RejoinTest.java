/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.cluster.RejoinClusterEvent;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import junit.framework.Assert;

public class RejoinTest extends AbstractToolkitRejoinTest {

  public RejoinTest(TestConfig testConfig) {
    super(testConfig, RejoinTestClient.class);
  }

  public static class RejoinTestClient extends AbstractToolkitRejoinTestClient {

    private static final int NUM_ELEMENTS = 10;
    private ToolkitLogger    logger;

    public RejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Exception {
      debug("Creating first toolkit");
      Properties properties = new Properties();
      properties.put("rejoin", "true");
      Toolkit tk = ToolkitFactory.createToolkit("toolkit:terracotta://" + getTerracottaUrl(), properties);
      final List<ClusterEvent> receivedEvents = new CopyOnWriteArrayList<ClusterEvent>();
      final ClusterNode beforeRejoinNode = tk.getClusterInfo().getCurrentNode();
      logger = ((ToolkitInternal) tk).getLogger("com.tc.AppLogger");

      tk.getClusterInfo().addClusterListener(new ClusterListener() {

        @Override
        public void onClusterEvent(ClusterEvent event) {
          doDebug("Received cluster event: " + event);
          receivedEvents.add(event);
        }
      });

      doDebug("Adding values to list before rejoin");
      ToolkitList<String> list = tk.getList("someList", null);
      ToolkitBlockingQueue<String> queue = tk.getBlockingQueue("someTBQ", null);
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        list.add("value-" + i);
        queue.add("value-" + i);
      }

      doDebug("Asserting values before rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertEquals("value-" + i, list.get(i));
        Assert.assertTrue(queue.contains("value-" + i));
      }

      ((ToolkitInternal) tk).waitUntilAllTransactionsComplete();

      doDebug("Crashing first active...");
      testHandlerMBean.crashActiveAndWaitForPassiveToTakeOver(0);
      doDebug("Passive must have taken over as ACTIVE");

      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          doDebug("Processing received events (waiting till rejoin happens for node: " + beforeRejoinNode + ")");
          for (ClusterEvent e : receivedEvents) {
            if (e instanceof RejoinClusterEvent) {
              RejoinClusterEvent re = (RejoinClusterEvent) e;
              doDebug("Rejoin event - oldNode: " + re.getNodeBeforeRejoin() + ", newNode: " + re.getNodeAfterRejoin());
              if (re.getNodeBeforeRejoin().getId().equals(beforeRejoinNode.getId())) {
                doDebug("Rejoin received for expected node - " + beforeRejoinNode);
                return true;
              }
            }
          }
          return false;
        }
      });

      doDebug("Rejoin happened successfully");
      doDebug("Asserting old values after rejoin");

      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertEquals("value-" + i, list.get(i));
        Assert.assertTrue(queue.contains("value-" + i));
      }


      doDebug("Adding new values after rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        list.add("value-after-rejoin-" + (i + NUM_ELEMENTS));
        queue.add("value-after-rejoin-" + (i + NUM_ELEMENTS));
      }


      for (int i = 0; i < list.size(); i++) {
        doDebug("Got value for i: " + i + ", value: " + list.get(i));
        doDebug("Got value for i: " + i + ", value: " + (queue.contains("value-" + i) ? "value-" + i : null));
      }

      doDebug("Asserting new values inserted after rejoin");
      Assert.assertEquals(2 * NUM_ELEMENTS, list.size());
      Assert.assertEquals(2 * NUM_ELEMENTS, queue.size());
      for (int i = 0; i < 2 * NUM_ELEMENTS; i++) {
        final String expected;
        if (i < NUM_ELEMENTS) {
          expected = "value-" + i;
        } else {
          expected = "value-after-rejoin-" + i;
        }
        Assert.assertEquals(expected, list.get(i));
        Assert.assertTrue(queue.contains(expected));
      }
      doDebug("Asserted new values");


    }

    private void doDebug(String string) {
      debug(string);
      if (logger != null) {
        logger.info("___XXX___: " + string);
      }
    }

  }

}
