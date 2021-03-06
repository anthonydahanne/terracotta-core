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
package com.tc.object;

import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.management.ManagementServicesManager;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.locks.ClientLockManager;

public class ClientConfigurationContext extends ConfigurationContextImpl {

  public final static String             LOCK_RESPONSE_STAGE                         = "lock_response_stage";
  public final static String             HYDRATE_MESSAGE_STAGE                       = "hydrate_message_stage";
  public static final String             CLIENT_COORDINATION_STAGE                   = "client_coordination_stage";
  public static final String             CLUSTER_EVENTS_STAGE                        = "cluster_events_stage";
  public static final String             JMXREMOTE_TUNNEL_STAGE                      = "jmxremote_tunnel_stage";
  public static final String             CLUSTER_MEMBERSHIP_EVENT_STAGE              = "cluster_membership_event_stage";
  public static final String             MANAGEMENT_STAGE                            = "management_stage";
  public static final String             VOLTRON_ENTITY_RESPONSE_STAGE                      = "request_ack_stage";
  public static final String             SERVER_ENTITY_MESSAGE_STAGE                 = "server_entity_message_stage";

  private final ClientLockManager         lockManager;
  private final ClientEntityManager       entityManager;
  private final ClientHandshakeManager    clientHandshakeManager;
  private final ManagementServicesManager managementServicesManager;

  public ClientConfigurationContext(StageManager stageManager, ClientLockManager lockManager,
                                    ClientEntityManager entityManager,
                                    ClientHandshakeManager clientHandshakeManager,
                                    ManagementServicesManager managementServicesManager) {
    super(stageManager);
    this.lockManager = lockManager;
    this.entityManager = entityManager;
    this.clientHandshakeManager = clientHandshakeManager;
    this.managementServicesManager = managementServicesManager;
  }

  public ClientLockManager getLockManager() {
    return this.lockManager;
  }

  public ClientEntityManager getEntityManager() {
    return this.entityManager;
  }

  public ClientHandshakeManager getClientHandshakeManager() {
    return this.clientHandshakeManager;
  }

  public ManagementServicesManager getManagementServicesManager() {
    return managementServicesManager;
  }
}
