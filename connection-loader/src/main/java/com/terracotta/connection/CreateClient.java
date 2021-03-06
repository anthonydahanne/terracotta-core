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
package com.terracotta.connection;

import com.google.common.collect.MapMaker;
import com.tc.license.ProductID;
import com.tc.net.core.SecurityInfo;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.DistributedObjectClientFactory;
import com.tc.util.UUID;
import com.terracotta.connection.client.TerracottaClientStripeConnectionConfig;

import java.util.Map;
import java.util.concurrent.Callable;


public class CreateClient implements Callable<ClientCreatorCallable> {

  static {
    /*
     * Make sure the google-collections finalizer thread is not in the TC thread group otherwise we will pin the L1
     * loader in memory since the TCThreadGroup will have been loaded by the L1Loader and hence will provide a strong
     * ref from the finalizer thread to the loader.
     */
    Map<String, Object> dummy = new MapMaker().weakValues().makeMap();
    dummy.put("dummy", new Object());
  }

  private final TerracottaClientStripeConnectionConfig stripeConnectionConfig;
  private final String              productIdName;

  private final SecurityInfo        securityInfo;

  public CreateClient(TerracottaClientStripeConnectionConfig stripeConnectionConfig, String productIdName) {
    this.stripeConnectionConfig = stripeConnectionConfig;
    this.productIdName = productIdName;
    String username = stripeConnectionConfig.getUsername();
    this.securityInfo = new SecurityInfo(username != null, username);
  }

  @Override
  public ClientCreatorCallable call() throws Exception {
    TCSecurityManager securityManager = null;

    ProductID productId = productIdName == null ? ProductID.USER : ProductID.valueOf(productIdName);
    UUID uuid = UUID.getUUID();
    final DistributedObjectClientFactory distributedObjectClientFactory = new DistributedObjectClientFactory(this.stripeConnectionConfig.getStripeMemberUris(),
                                                                                                             securityManager,
                                                                                                             securityInfo,
                                                                                                             productId,
        uuid);
    return new ClientCreatorCallableImpl(distributedObjectClientFactory, uuid);
  }

  public static class ClientCreatorCallableImpl implements ClientCreatorCallable {

    private final DistributedObjectClientFactory distributedObjectClientFactory;
    private final UUID                           uuid;

    public ClientCreatorCallableImpl(DistributedObjectClientFactory distributedObjectClientFactory,
                                     UUID uuid) {
      this.distributedObjectClientFactory = distributedObjectClientFactory;
      this.uuid = uuid;
    }

    @Override
    public Object call() throws Exception {
      return distributedObjectClientFactory.create();
    }

    @Override
    public String getUuid() {
      return uuid.toString();
    }

  }

}
