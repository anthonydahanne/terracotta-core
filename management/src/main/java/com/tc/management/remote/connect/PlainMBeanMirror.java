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

import java.io.IOException;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class PlainMBeanMirror implements MBeanMirror {
  private final MBeanServerConnection mbsc;
  private final ObjectName            objectName;
  private final ObjectName            localObjectName;
  private final MBeanInfo             mBeanInfo;

  public PlainMBeanMirror(MBeanServerConnection mbsc, ObjectName objectName, ObjectName localObjectName)
      throws IOException, InstanceNotFoundException, IntrospectionException {
    this.mbsc = mbsc;
    this.objectName = objectName;
    this.localObjectName = localObjectName;
    MBeanInfo beanInfo = null;
    try {
      // Don't save the mbean info for dynamic mbeans, it can change during execution
      if (!mbsc.isInstanceOf(objectName, DynamicMBean.class.getName())) {
        beanInfo = mbsc.getMBeanInfo(objectName);
      }
    } catch (ReflectionException e) {
      // Callers cannot possibly care about the difference between
      // IntrospectionException and ReflectionException
      IntrospectionException ie = new IntrospectionException(e.getMessage());
      ie.initCause(e);
      throw ie;
    }
    mBeanInfo = beanInfo;
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection() {
    return mbsc;
  }

  @Override
  public ObjectName getRemoteObjectName() {
    return objectName;
  }

  @Override
  public ObjectName getLocalObjectName() {
    return localObjectName;
  }

  @Override
  public Object getAttribute(String name) throws AttributeNotFoundException, MBeanException, ReflectionException {
    try {
      return mbsc.getAttribute(objectName, name);
    } catch (IOException e) {
      throw new MBeanException(e);
    } catch (InstanceNotFoundException e) {
      throw new MBeanException(e);
    }
  }

  @Override
  public void setAttribute(Attribute attr) throws AttributeNotFoundException, InvalidAttributeValueException,
      MBeanException, ReflectionException {
    try {
      mbsc.setAttribute(objectName, attr);
    } catch (IOException e) {
      throw new MBeanException(e);
    } catch (InstanceNotFoundException e) {
      throw new MBeanException(e);
    }
  }

  @Override
  public AttributeList getAttributes(String[] names) {
    try {
      return mbsc.getAttributes(objectName, names);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      return new AttributeList();
    }
  }

  @Override
  public AttributeList setAttributes(AttributeList attrs) {
    try {
      return mbsc.setAttributes(objectName, attrs);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      return new AttributeList();
    }
  }

  @Override
  public Object invoke(String opName, Object[] args, String[] sig) throws MBeanException, ReflectionException {
    try {
      return mbsc.invoke(objectName, opName, args, sig);
    } catch (IOException e) {
      throw new MBeanException(e);
    } catch (InstanceNotFoundException e) {
      throw new MBeanException(e);
    }
  }

  @Override
  public MBeanInfo getMBeanInfo() {
    try {
      return mBeanInfo == null ? mbsc.getMBeanInfo(objectName) : mBeanInfo;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
