/*_##########################################################################
  _##
  _##  Copyright (C) 2011  Kaito Yamada
  _##
  _##########################################################################
*/

package org.pcap4j.packet.namedvalue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.pcap4j.packet.ArpPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.ByteArrays;

public
final class EtherType
extends NamedNumber<Short> implements Comparable<Short> {

  //http://www.iana.org/assignments/ethernet-numbers
  public static final EtherType IPV4
    = new EtherType((short)0x0800, "IPv4");
  public static final EtherType ARP
    = new EtherType((short)0x0806, "ARP");
  public static final EtherType RARP
    = new EtherType((short)0x8035, "RARP");
  public static final EtherType APPLETALK
    = new EtherType((short)0x809b, "Appletalk");
  public static final EtherType IPV6
    = new EtherType((short)0x86dd, "IPv6");
  public static final EtherType PPP
    = new EtherType((short)0x880b, "PPP");
  public static final EtherType MPLS
    = new EtherType((short)0x8847, "MPLS");
  public static final EtherType PPPOE_DISCOVERY_STAGE
    = new EtherType((short)0x8863, "PPPoE Discovery Stage");
  public static final EtherType PPPOE_SESSION_STAGE
    = new EtherType((short)0x8864, "PPPoE Session Stage");

  private static Map<Short, EtherType> registry
    = new HashMap<Short, EtherType>();
  private static Map<Class<? extends Packet>, EtherType> etherTypeOfPacket
    = new HashMap<Class<? extends Packet>, EtherType>();

  static {
    for (Field field: EtherType.class.getFields()) {
      if (field.getType().isAssignableFrom(EtherType.class)) {
        try {
          EtherType typeCode = (EtherType)field.get(null);
          registry.put(typeCode.value(), typeCode);
        } catch (IllegalArgumentException e) {
          throw new AssertionError(e);
        } catch (IllegalAccessException e) {
          throw new AssertionError(e);
        } catch (NullPointerException e) {
          continue;
        }
      }
    }

    etherTypeOfPacket.put(ArpPacket.class, ARP);
    etherTypeOfPacket.put(IpV4Packet.class, IPV4);
  }

  private EtherType(Short value, String name) {
    super(value, name);
  }

  public static EtherType getInstance(Short value) {
    if (registry.containsKey(value)) {
      return registry.get(value);
    }
    else {
      return new EtherType(value, "unknown");
    }
  }

  public static EtherType getInstance(Class<? extends Packet> clazz) {
    return etherTypeOfPacket.get(clazz);
  }

  @Override
  public String valueAsString() {
    return "0x" + ByteArrays.toHexString(value(), "");
  }

  public int compareTo(Short o) {
    return value().compareTo(o);
  }

}