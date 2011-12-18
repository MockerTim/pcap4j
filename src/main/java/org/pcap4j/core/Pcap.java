/*_##########################################################################
  _##
  _##  Copyright (C) 2011  Kaito Yamada
  _##
  _##########################################################################
*/

package org.pcap4j.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.pcap4j.core.NativeMappings.PcapLibrary;
import org.pcap4j.core.NativeMappings.PcapErrbuf;
import org.pcap4j.core.NativeMappings.pcap_if;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public final class Pcap {

//  #define PCAP_ERROR      -1  /* generic error code */
//  #define PCAP_ERROR_BREAK    -2  /* loop terminated by pcap_breakloop */
//  #define PCAP_ERROR_NOT_ACTIVATED  -3  /* the capture needs to be activated */
//  #define PCAP_ERROR_ACTIVATED    -4  /* the operation can't be performed on already activated captures */
//  #define PCAP_ERROR_NO_SUCH_DEVICE -5  /* no such device exists */
//  #define PCAP_ERROR_RFMON_NOTSUP   -6  /* this device doesn't support rfmon (monitor) mode */
//  #define PCAP_ERROR_NOT_RFMON    -7  /* operation supported only in monitor mode */
//  #define PCAP_ERROR_PERM_DENIED    -8  /* no permission to open the device */
//  #define PCAP_ERROR_IFACE_NOT_UP   -9  /* interface isn't up */
//  #define PCAP_WARNING      1 /* generic warning code */
//  #define PCAP_WARNING_PROMISC_NOTSUP 2 /* this device doesn't support promiscuous mode */

  private static final Logger logger = Logger.getLogger(Pcap.class);

  private Pcap() { throw new AssertionError(); }

  public static
  List<PcapNetworkInterface> findAllDevs() throws PcapNativeException {
    PointerByReference alldevsPP = new PointerByReference();
    PcapErrbuf errbuf = new PcapErrbuf();

    int rc = PcapLibrary.INSTANCE.pcap_findalldevs(alldevsPP, errbuf);
    if (rc != 0) {
      throw new PcapNativeException(
              "Return code: " + rc + ", Message: " + errbuf.getMessage()
            );
    }
    if (errbuf.length() != 0) {
      logger.warn(errbuf.getMessage());
    }

    Pointer alldevsp = alldevsPP.getValue();
    if (alldevsp == null) {
      logger.info("No NIF was found.");
      return new ArrayList<PcapNetworkInterface>(0);
    }

    pcap_if pcapIf = new pcap_if(alldevsp);

    List<PcapNetworkInterface> ifList = new ArrayList<PcapNetworkInterface>();
    for (pcap_if pif = pcapIf; pif != null; pif = pif.next) {
      ifList.add(PcapNetworkInterface.newInstance(pif));
    }

    PcapLibrary.INSTANCE.pcap_freealldevs(pcapIf.getPointer());

    logger.info(ifList.size() + " NIF(s) found.");
    return ifList;
  }

  public static
  PcapNetworkInterface getNifBy(InetAddress addr) throws PcapNativeException {
    List<PcapNetworkInterface> allDevs = Pcap.findAllDevs();

    for (PcapNetworkInterface pif: allDevs) {
      for (PcapAddress paddr: pif.getAddresses()) {
        if (paddr.getAddress().equals(addr)) {
          return pif;
        }
      }
    }

    return null;
  }

  public static String lookupDev() throws PcapNativeException {
    PcapErrbuf errbuf = new PcapErrbuf();
    Pointer result = PcapLibrary.INSTANCE.pcap_lookupdev(errbuf);

    if (result == null || errbuf.length() != 0) {
      throw new PcapNativeException(errbuf.getMessage());
    }

    return result.getString(0, true);
  }

  public static
  PcapNetworkInterface selectNetworkInterface(Reader in, Writer out)
  throws IOException {
    BufferedReader reader = new BufferedReader(in);
    BufferedWriter writer = new BufferedWriter(out);

    List<PcapNetworkInterface> allDevs = null;
    try {
      allDevs = Pcap.findAllDevs();
    } catch (PcapNativeException e) {
      logger.error(e);
    }
    if (allDevs == null || allDevs.size() == 0) {
      throw new IOException("No NIF to capture.");
    }

    int nifIdx = 0;
    for (PcapNetworkInterface nif: allDevs) {
      writer.write("NIF[" + nifIdx + "]: " + nif.getName());
      writer.newLine();
      writer.write("      : description: " + nif.getDescription());
      writer.newLine();

      for (PcapAddress addr: nif.getAddresses()) {
        writer.write(
          "      : address: " + addr.getAddress()
        );
        writer.newLine();
      }
      nifIdx++;
    }
    writer.flush();

    while (true) {
      writer.write("Select a device number to capture packets, or enter 'q' to quite > ");
      writer.flush();

      String input;
      if ((input = reader.readLine()) == null) {
        continue;
      }

      if (input.equals("q")) {
        return null;
      }

      try {
        nifIdx = Integer.parseInt(input);
        if (nifIdx < 0 || nifIdx >= allDevs.size()) {
          writer.write("Invalid input.");
          continue;
        }
        else {
          break;
        }
      } catch (NumberFormatException e) {
        writer.write("Invalid input.");
        continue;
      }
    }

    return allDevs.get(nifIdx);
  }

}