/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.souliss.internal.protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Enumeration;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.internal.SoulissUDPConstants;
import org.openhab.binding.souliss.internal.config.GatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provide to construct MaCaco and UDP frame
 *
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 * @author Alessandro Del Pex - Souliss App
 */
@NonNullByDefault
public class CommonCommands {

    private static final Logger logger = LoggerFactory.getLogger(CommonCommands.class);

    private static final String LITERAL_SEND_FRAME = "buildFORCEFrame - {}, soulissNodeIPAddressOnLAN: {}";

    public static final ArrayList<Byte> buildFORCEFrame(GatewayConfig gwConfig, int idNode, int slot,
            byte shortCommand) {
        return buildFORCEFrame(gwConfig, idNode, slot, shortCommand, null, null, null);
    }

    /*
     * used for set dimmer value. It set command at first byte and dimmerVal to
     * second byte
     */
    public static final ArrayList<Byte> buildFORCEFrame(GatewayConfig gwConfig, int idNode, int slot, byte shortCommand,
            byte lDimmer) {
        return buildFORCEFrame(gwConfig, idNode, slot, shortCommand, lDimmer, null, null);
    }

    /*
     * build force frame with command and RGB value
     */
    public static final ArrayList<Byte> buildFORCEFrame(GatewayConfig gwConfig, int idNode, int slot, byte shortCommand,
            @Nullable Byte byte1, @Nullable Byte byte2, @Nullable Byte byte3) {
        ArrayList<Byte> macacoFrame = new ArrayList<>();
        macacoFrame.add(SoulissUDPConstants.SOULISS_UDP_FUNCTION_FORCE);

        // PUTIN, STARTOFFEST, NUMBEROF
        // PUTIN
        macacoFrame.add((byte) 0x0);
        // PUTIN
        macacoFrame.add((byte) 0x0);

        macacoFrame.add((byte) (idNode));// Start Offset

        if (byte1 == null && byte2 == null && byte3 == null) {
            // Number Of
            macacoFrame.add((byte) ((byte) slot + 1));
        } else if (byte2 == null && byte3 == null) {
            // Number Of byte of payload= command + set byte
            macacoFrame.add((byte) ((byte) slot + 2));
        } else {
            // Number Of byte of payload= OnOFF + Red + Green + Blu
            macacoFrame.add((byte) ((byte) slot + 4));
        }

        for (var i = 0; i <= slot - 1; i++) {
            // I set the bytes preceding the slot to be modified to zero
            macacoFrame.add((byte) 00);
        }
        // PAYLOAD
        macacoFrame.add(shortCommand);

        if (byte1 != null && byte2 != null && byte3 != null) {
            // PAYLOAD RED
            macacoFrame.add(byte1);
            // PAYLOAD GREEN
            macacoFrame.add(byte2);
            // PAYLOAD BLUE
            macacoFrame.add(byte3);
        } else if (byte1 != null) {
            // PAYLOAD DIMMER
            macacoFrame.add(byte1);
        }

        logger.debug(LITERAL_SEND_FRAME, macacoToString(macacoFrame), gwConfig);
        return macacoFrame;
    }

    /*
     * T61 build frame to push the setpoint value
     */

    public static final ArrayList<Byte> buildFORCEFrameT61SetPoint(GatewayConfig gwConfig, int idNode, int slot,
            Byte byte1, Byte byte2) {
        ArrayList<Byte> macacoFrame = new ArrayList<>();
        macacoFrame.add(SoulissUDPConstants.SOULISS_UDP_FUNCTION_FORCE);

        // PUTIN, STARTOFFEST, NUMBEROF
        // PUTIN
        macacoFrame.add((byte) 0x00);
        // PUTIN
        macacoFrame.add((byte) 0x00);
        // Start Offset
        macacoFrame.add((byte) (idNode));
        // Number Of byte of payload= command + set byte
        macacoFrame.add((byte) ((byte) slot + 2));

        for (var i = 0; i <= slot - 1; i++) {
            // I set the bytes preceding the slot to be modified to zero
            macacoFrame.add((byte) 00);
        }
        // PAYLOAD
        // first byte Setpoint Value
        macacoFrame.add(byte1);
        // second byte Setpoint Value
        macacoFrame.add(byte2);

        logger.debug(LITERAL_SEND_FRAME, macacoToString(macacoFrame), gwConfig);

        return macacoFrame;
    }

    /*
     * T31 build force frame with command and setpoint float
     */
    public static final ArrayList<Byte> buildFORCEFrameT31SetPoint(GatewayConfig gwConfig, int idNode, int slot,
            byte shortCommand, Byte byte1, Byte byte2) {
        ArrayList<Byte> macacoFrame = new ArrayList<>();
        macacoFrame.add(SoulissUDPConstants.SOULISS_UDP_FUNCTION_FORCE);

        // PUTIN, STARTOFFEST, NUMBEROF
        // PUTIN
        macacoFrame.add((byte) 0x00);
        // PUTIN
        macacoFrame.add((byte) 0x00);

        // Start Offset
        macacoFrame.add((byte) (idNode));
        // Number Of byte of payload= command + set byte
        macacoFrame.add((byte) ((byte) slot + 5));

        for (var i = 0; i <= slot - 1; i++) {
            // prvious byte to zero
            macacoFrame.add((byte) 00);
            // slot to be changed
        }
        // PAYLOAD
        macacoFrame.add(shortCommand);

        // Empty - Temperature Measured Value
        macacoFrame.add((byte) 0x0);
        // Empty - Temperature Measured Value
        macacoFrame.add((byte) 0x0);
        // Temperature Setpoint Value
        macacoFrame.add(byte1);
        // Temperature Setpoint Value
        macacoFrame.add(byte2);

        logger.debug(LITERAL_SEND_FRAME, macacoToString(macacoFrame), gwConfig);
        return macacoFrame;
    }

    public static final ArrayList<Byte> buildDBStructFrame(GatewayConfig gwConfig) {
        ArrayList<Byte> macacoFrame = new ArrayList<>();
        macacoFrame.add((byte) SoulissUDPConstants.SOULISS_UDP_FUNCTION_DBSTRUCT_REQ);
        // PUTIN
        macacoFrame.add((byte) 0x0);
        // PUTIN
        macacoFrame.add((byte) 0x0);
        // Start Offset
        macacoFrame.add((byte) 0x0);
        // Number Of
        macacoFrame.add((byte) 0x0);

        logger.debug("buildDBStructFrame - {}, soulissNodeIPAddressOnLAN: {}", macacoToString(macacoFrame), gwConfig);
        return macacoFrame;
    }

    /*
     * send broadcast UDP frame - unused in this version
     */
    private static final void sendBroadcastNow(ArrayList<Byte> macacoFrame) {
        byte iUserIndex = (byte) 120;
        byte iNodeIndex = (byte) 70;

        // Broadcast the message over all the network interfaces
        Enumeration<@Nullable NetworkInterface> interfaces;
        DatagramSocket sender = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                var networkInterface = interfaces.nextElement();
                if (networkInterface != null) {
                    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                        continue;
                    }
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        var broadcast = new InetAddress[3];
                        broadcast[0] = InetAddress.getByName("224.0.0.1");
                        broadcast[1] = InetAddress.getByName("255.255.255.255");
                        broadcast[2] = interfaceAddress.getBroadcast();
                        for (InetAddress bc : broadcast) {
                            // Send the broadcast package!
                            if (bc != null) {
                                try {
                                    ArrayList<Byte> buf = buildVNetFrame(macacoFrame, "255.255.255.255", iUserIndex,
                                            iNodeIndex);
                                    byte[] merd = toByteArray(buf);
                                    var packet = new DatagramPacket(merd, merd.length, bc,
                                            SoulissUDPConstants.SOULISS_GATEWAY_DEFAULT_PORT);
                                    // Datagramsocket creation
                                    var channel = DatagramChannel.open();
                                    sender = channel.socket();
                                    sender.setReuseAddress(true);
                                    sender.setBroadcast(true);

                                    var sa = new InetSocketAddress(230);
                                    sender.bind(sa);

                                    sender.send(packet);
                                    logger.debug("Request packet sent to: {} Interface: {}", bc.getHostAddress(),
                                            networkInterface.getDisplayName());

                                } catch (IOException e) {
                                    logger.debug("IO error: {}", e.getMessage());
                                } catch (Exception e) {
                                    logger.debug("{}", e.getMessage(), e);
                                } finally {
                                    if ((sender != null) && (!sender.isClosed())) {
                                        sender.close();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SocketException | UnknownHostException e) {
            logger.warn("{}", e.getMessage());
        }
    }

    /*
     * Build VNet Frame
     */
    public static final ArrayList<Byte> buildVNetFrame(ArrayList<Byte> macacoFrame2, @Nullable String gatewayLanAddress,
            byte iUserIndex, byte iNodeIndex) {
        if (gatewayLanAddress != null) {
            ArrayList<Byte> frame = new ArrayList<>();
            InetAddress ip;
            try {
                ip = InetAddress.getByName(gatewayLanAddress);
            } catch (UnknownHostException e) {
                logger.warn("{}", e.getMessage());
                return frame;
            }
            byte[] dude = ip.getAddress();

            // Port
            frame.add((byte) 23);
            // es 192.168.1.XX BOARD
            frame.add((byte) (dude[3] & 0xFF));

            // n broadcast : communication by Ip
            // 255.255.255.255 to associate vNet 0xFFFF address.
            frame.add(gatewayLanAddress.compareTo(SoulissUDPConstants.BROADCASTADDR) == 0 ? dude[2] : 0);
            // NODE INDEX - source vNet address User Interface
            frame.add(iNodeIndex);
            // USER INDEX - source vNet address User Interface
            frame.add(iUserIndex);

            // adds the calculation in the head
            // Length
            frame.add(0, (byte) (frame.size() + macacoFrame2.size() + 1));
            // Length Check 2
            frame.add(0, (byte) (frame.size() + macacoFrame2.size() + 1));

            frame.addAll(macacoFrame2);
            return frame;
        } else {
            throw new IllegalArgumentException("Cannot build VNet Frame . Null Souliss IP address");
        }
    }

    /**
     * Builds old-school byte array
     *
     * @param buf
     * @return
     */
    public static final byte[] toByteArray(ArrayList<Byte> buf) {
        var merd = new byte[buf.size()];
        for (var i = 0; i < buf.size(); i++) {
            merd[i] = buf.get(i);
        }
        return merd;
    }

    /**
     * Build MULTICAST FORCE Frame
     */
    public static final ArrayList<Byte> buildMULTICASTFORCEFrame(GatewayConfig gwConfig, byte typical,
            byte shortCommand) {
        ArrayList<Byte> macacoFrame = new ArrayList<>();
        macacoFrame.add(SoulissUDPConstants.SOULISS_UDP_FUNCTION_FORCE_MASSIVE);

        // PUTIN, STARTOFFEST, NUMBEROF
        // PUTIN
        macacoFrame.add((byte) 0x0);
        // PUTIN
        macacoFrame.add((byte) 0x0);
        // Start Offset
        macacoFrame.add(typical);
        // Number Of
        macacoFrame.add((byte) 1);
        // PAYLOAD
        macacoFrame.add(shortCommand);
        logger.debug("buildMULTICASTFORCEFrame - {}, soulissNodeIPAddressOnLAN: {}", macacoToString(macacoFrame),
                gwConfig);
        return macacoFrame;
    }

    /**
     * Build PING Frame
     */
    public static final ArrayList<Byte> buildPingFrame(@Nullable GatewayConfig gwConfig) {
        ArrayList<Byte> macacoFrame = new ArrayList<>();
        if (gwConfig != null) {
            macacoFrame.add(SoulissUDPConstants.SOULISS_UDP_FUNCTION_PING_REQ);

            // PUTIN, STARTOFFEST, NUMBEROF
            // PUTIN
            macacoFrame.add((byte) 0x00);
            // PUTIN
            macacoFrame.add((byte) 0x00);
            // Start Offset
            macacoFrame.add((byte) 0x00);
            // Number Of
            macacoFrame.add((byte) 0x00);
            logger.debug("buildPingFrame - {}, IP: {} ", macacoToString(macacoFrame), gwConfig);

        } else {
            logger.warn("Cannot send Souliss Ping -  Ip null");
        }
        return macacoFrame;
    }

    /**
     * Build BROADCAST PING Frame
     */
    public static final void sendBroadcastGatewayDiscover() {
        ArrayList<Byte> macacoFrame = new ArrayList<>();
        macacoFrame.add(SoulissUDPConstants.SOULISS_UDP_FUNCTION_DISCOVER_GW_NODE_BCAST_REQ);

        // PUTIN, STARTOFFEST, NUMBEROF
        // PUTIN
        macacoFrame.add((byte) 0x05);
        // PUTIN
        macacoFrame.add((byte) 0x00);
        // Start Offset
        macacoFrame.add((byte) 0x00);
        // Number Of
        macacoFrame.add((byte) 0x00);
        logger.debug("sendBroadcastPing - {} ", macacoToString(macacoFrame));
        sendBroadcastNow(macacoFrame);
    }

    /**
     * Build SUBSCRIPTION Frame
     */
    public static final ArrayList<Byte> buildSUBSCRIPTIONframe(GatewayConfig gwConfig, int iNodes) {
        ArrayList<Byte> macacoFrame = new ArrayList<>();
        macacoFrame.add(SoulissUDPConstants.SOULISS_UDP_FUNCTION_SUBSCRIBE_REQ);

        // PUTIN, STARTOFFEST, NUMBEROF
        // PUTIN
        macacoFrame.add((byte) 0x00);
        // PUTIN
        macacoFrame.add((byte) 0x00);
        macacoFrame.add((byte) 0x00);

        macacoFrame.add((byte) iNodes);
        logger.debug("buildSUBSCRIPTIONframe - {}, IP: {} ", macacoToString(macacoFrame), gwConfig);
        return macacoFrame;
    }

    /**
     * Build HEALTHY REQUEST Frame
     */
    public static final ArrayList<Byte> buildHealthyRequestFrame(GatewayConfig gwConfig, int iNodes) {
        ArrayList<Byte> macacoFrame = new ArrayList<>();
        macacoFrame.add(SoulissUDPConstants.SOULISS_UDP_FUNCTION_HEALTHY_REQ);

        // PUTIN, STARTOFFSET, NUMBEROF
        // PUTIN
        macacoFrame.add((byte) 0x00);
        // PUTIN
        macacoFrame.add((byte) 0x00);
        macacoFrame.add((byte) 0x00);
        macacoFrame.add((byte) iNodes);
        logger.debug("buildHealthyRequestFrame - {}, IP: {} ", macacoToString(macacoFrame), gwConfig);
        return macacoFrame;
    }

    /**
     * Build TYPICAL REQUEST Frame
     */
    public static final ArrayList<Byte> buildTypicalRequestFrame(GatewayConfig gwConfig, int nodes) {
        ArrayList<Byte> macacoFrame = new ArrayList<>();
        macacoFrame.add(SoulissUDPConstants.SOULISS_UDP_FUNCTION_TYP_REQ);
        // PUTIN, STARTOFFEST, NUMBEROF
        // PUTIN
        macacoFrame.add((byte) 0x00);
        // PUTIN
        macacoFrame.add((byte) 0x00);
        // startOffset
        macacoFrame.add((byte) 0x00);
        // iNodes
        macacoFrame.add((byte) nodes);
        logger.debug("buildTypicalRequestFrame - {}, IP: {} ", macacoToString(macacoFrame), gwConfig.gatewayLanAddress);
        return macacoFrame;
    }

    /**
     * Build TYPICAL REQUEST Frame with start offset
     */
    public static final ArrayList<Byte> buildTypicalRequestFrame(GatewayConfig gwConfig, int start, int nodes) {
        ArrayList<Byte> macacoFrame = new ArrayList<>();
        macacoFrame.add(SoulissUDPConstants.SOULISS_UDP_FUNCTION_TYP_REQ);
        // PUTIN, STARTOFFEST, NUMBEROF
        // PUTIN
        macacoFrame.add((byte) 0x00);
        // PUTIN
        macacoFrame.add((byte) 0x00);
        // startOffset
        macacoFrame.add((byte) start);
        // iNodes
        macacoFrame.add((byte) nodes);
        logger.debug("buildTypicalRequestFrame - {}, IP: {} ", macacoToString(macacoFrame), gwConfig.gatewayLanAddress);
        return macacoFrame;
    }

    private static final String macacoToString(ArrayList<Byte> mACACOframe) {
        // I copy arrays to avoid concurrent changes
        ArrayList<Byte> mACACOframe2 = new ArrayList<>();
        mACACOframe2.addAll(mACACOframe);
        var sb = new StringBuilder();
        sb.append("HEX: [");
        for (byte b : mACACOframe2) {
            sb.append(String.format("%02X ", b));
        }
        sb.append("]");
        return sb.toString();
    }
}
