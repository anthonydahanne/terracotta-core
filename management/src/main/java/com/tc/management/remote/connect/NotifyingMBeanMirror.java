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
package com.tc.management.remote.connect;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class NotifyingMBeanMirror implements MBeanMirror, NotificationEmitter {
  private final MBeanMirror mirror;

  public NotifyingMBeanMirror(MBeanMirror mirror) {
    this.mirror = mirror;
  }

  @Override
  public Object getAttribute(String name) throws AttributeNotFoundException, MBeanException, ReflectionException {
    return mirror.getAttribute(name);
  }

  @Override
  public void setAttribute(Attribute attr) throws AttributeNotFoundException, InvalidAttributeValueException,
      MBeanException, ReflectionException {
    mirror.setAttribute(attr);
  }

  @Override
  public AttributeList getAttributes(String[] names) {
    return mirror.getAttributes(names);
  }

  @Override
  public AttributeList setAttributes(AttributeList attrs) {
    return mirror.setAttributes(attrs);
  }

  @Override
  public Object invoke(String opName, Object[] args, String[] sig) throws MBeanException, ReflectionException {
    return mirror.invoke(opName, args, sig);
  }

  @Override
  public MBeanInfo getMBeanInfo() {
    return mirror.getMBeanInfo();
  }

  private class NotificationIntercepter implements NotificationListener {
    private final NotificationListener listener;

    private NotificationIntercepter(NotificationListener listener) {
      this.listener = listener;
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
      notification.setSource(mirror.getLocalObjectName());
      listener.handleNotification(notification, handback);
    }
  }

  @Override
  public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
    try {
      mirror.getMBeanServerConnection()
          .addNotificationListener(getRemoteObjectName(), new NotificationIntercepter(listener), filter, handback);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
    try {
      mirror.getMBeanServerConnection().removeNotificationListener(getRemoteObjectName(), listener);
    } catch (RuntimeException e) {
      throw e;
    } catch (ListenerNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
      throws ListenerNotFoundException {
    try {
      mirror.getMBeanServerConnection().removeNotificationListener(getRemoteObjectName(), listener, filter, handback);
    } catch (RuntimeException e) {
      throw e;
    } catch (ListenerNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return mirror.getMBeanInfo().getNotifications();
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection() {
    return mirror.getMBeanServerConnection();
  }

  @Override
  public ObjectName getRemoteObjectName() {
    return mirror.getRemoteObjectName();
  }

  @Override
  public ObjectName getLocalObjectName() {
    return mirror.getLocalObjectName();
  }
}
