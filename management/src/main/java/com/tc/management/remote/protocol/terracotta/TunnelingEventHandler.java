/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.async.api.AbstractEventHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.config.DSOMBeanConfig;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.util.UUID;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;

import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.Message;

public class TunnelingEventHandler extends AbstractEventHandler<JmxRemoteTunnelMessage> implements ClientHandshakeCallback {

  private static final TCLogger logger = TCLogging.getLogger(TunnelingEventHandler.class);

  private final MessageChannel channel;

  private final DSOMBeanConfig config;

  private TunnelingMessageConnection messageConnection;

  private boolean acceptOk;

  private final Object jmxReadyLock;

  private final SetOnceFlag localJmxServerReady;

  private boolean transportConnected;

  private boolean sentReadyMessage;

  private boolean stopAccept = false;

  private final UUID uuid;

  public TunnelingEventHandler(MessageChannel channel, DSOMBeanConfig config, UUID uuid) {
    this.channel = channel;
    this.config = config;
    acceptOk = false;
    jmxReadyLock = new Object();
    localJmxServerReady = new SetOnceFlag();
    transportConnected = false;
    sentReadyMessage = false;
    this.uuid = uuid;
  }

  public MessageChannel getMessageChannel() {
    return channel;
  }

  public UUID getUUID() {
    return uuid;
  }

  @Override
  public void handleEvent(JmxRemoteTunnelMessage messageEnvelope) {
    if (messageEnvelope.getCloseConnection()) {
      reset();
    } else {
      final Message message = (Message) messageEnvelope.getTunneledMessage();
      synchronized (this) {
        if (messageEnvelope.getInitConnection()) {
          if (messageConnection != null) {
            logger.warn("Received a client connection initialization, resetting existing connection");
            reset();
          }
          messageConnection = new TunnelingMessageConnection(channel, true);
          acceptOk = true;
          notifyAll();
        } else if (messageConnection == null) {
          logger.warn("Received unexpected data message, connection is not yet established");
        } else {
          if (message != null) {
            messageConnection.incomingNetworkMessage(message);
          } else {
            logger.warn("Received tunneled message with no data, resetting connection");
            reset();
          }
        }
      }
    }
  }

  protected synchronized MessageConnection accept() throws IOException {
    while (!acceptOk && !stopAccept) {
      try {
        wait();
      } catch (InterruptedException ie) {
        logger.warn("Interrupted while waiting for a new connection", ie);
        throw new IOException("Interrupted while waiting for new connection: " + ie.getMessage());
      }
    }
    acceptOk = false;

    MessageConnection rv = messageConnection;
    if (rv == null) {
      // if we return null here it will cause an uncaught exception and trigger VM exit prematurely
      throw new IOException("no connection");
    }

    return rv;
  }

  protected synchronized void stopAccept() {
    stopAccept = true;
    notifyAll();
  }

  private synchronized void reset() {
    if (messageConnection != null) {
      messageConnection.close();
    }
    messageConnection = null;
    acceptOk = false;
    synchronized (jmxReadyLock) {
      sentReadyMessage = false;
    }
    notifyAll();
  }

  public void jmxIsReady() {
    synchronized (jmxReadyLock) {
      localJmxServerReady.set();
    }

    sendJmxReadyMessageIfNecessary();
  }

  public boolean isTunnelingReady() {
    synchronized (jmxReadyLock) {
      return localJmxServerReady.isSet() && transportConnected;
    }
  }

  /**
   * Once the local JMX server has successfully started (this happens in a background thread as DSO is so early in the startup process that
   * the system JMX server in 1.5+ can't be created inline with other initialization) we send a 'ready' message to the L2 each time we
   * connect to it. This tells the L2 that they can connect to our local JMX server and see the beans we have in the DSO client.
   */
  private void sendJmxReadyMessageIfNecessary() {
    final boolean send;
    synchronized (jmxReadyLock) {
      send = isTunnelingReady() && !sentReadyMessage;
      if (send) {
        sentReadyMessage = true;
      }
    }

    // Doing this message send outside of the jmxReadyLock to avoid a
    // deadlock (CDV-132)
    if (send) {
      logger.info("Client JMX server ready; sending notification to L2 server");
      L1JmxReady readyMessage = (L1JmxReady) channel.createMessage(TCMessageType.CLIENT_JMX_READY_MESSAGE);
      readyMessage.initialize(uuid, config.getTunneledDomains());
      readyMessage.send();
    }

  }

  @Override
  public void initializeHandshake(ClientHandshakeMessage handshakeMessage) {
    // Ignore
  }

  @Override
  public void pause() {
    reset();
    synchronized (jmxReadyLock) {
      transportConnected = false;
    }
  }

  @Override
  public void unpause() {
    synchronized (jmxReadyLock) {
      // MNK-2553: Flip the transportConnected switch here in case we receive the transport connected notification
      // late (i.e. after ClientHandshakeManager).
      transportConnected = true;
    }
    sendJmxReadyMessageIfNecessary();
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    // Ignore
  }

}
