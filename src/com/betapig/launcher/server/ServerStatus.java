package com.betapig.launcher.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ServerStatus {
    private String hostname;
    private int port;
    private boolean isOnline;
    private int playerCount;
    private int maxPlayers;
    private List<String> players;
    private long ping;
    private String statusMessage;
    private Exception lastError;
    private int protocolVersion;
    private String gameVersion;
    
    // Minecraft protocol constants
    private static final int PACKET_ID = 0x00;
    private static final int PROTOCOL_VERSION = 47; // Minecraft 1.8.9 protocol version
    private static final int HANDSHAKE_STATE = 1;

    public ServerStatus(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.players = new ArrayList<>();
        this.statusMessage = "\u041f\u0440\u043e\u0432\u0435\u0440\u043a\u0430..."; // "Checking..."
        this.isOnline = false;
        this.ping = -1;
    }

    public void queryServer() {
        try (Socket socket = new Socket()) {
            long startTime = System.currentTimeMillis();
            socket.connect(new InetSocketAddress(hostname, port), 5000);
            
            // Send handshake packet
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            
            // Handshake packet
            ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(handshakeBytes);
            
            writeVarInt(handshake, PACKET_ID); // Packet ID
            writeVarInt(handshake, PROTOCOL_VERSION); // Protocol version
            writeString(handshake, hostname); // Server address
            handshake.writeShort(port); // Server port
            writeVarInt(handshake, HANDSHAKE_STATE); // Next state (1 for status)
            
            // Send handshake packet
            writeVarInt(out, handshakeBytes.size());
            out.write(handshakeBytes.toByteArray());
            
            // Send status request
            out.writeByte(0x01); // Packet length
            out.writeByte(0x00); // Packet ID
            
            // Read response
            int length = readVarInt(in);
            int packetId = readVarInt(in);
            
            if (packetId == 0x00) {
                String json = readString(in);
                parseServerInfo(json);
                
                // Send ping
                out.writeByte(0x09); // Packet length
                out.writeByte(0x01); // Packet ID
                out.writeLong(System.currentTimeMillis());
                
                // Read pong
                length = readVarInt(in);
                packetId = readVarInt(in);
                long pongTime = in.readLong();
                
                ping = System.currentTimeMillis() - pongTime;
                isOnline = true;
                statusMessage = String.format("Версия: %s - Пинг: %dms", gameVersion, ping);
            } else if (packetId == 0xFF) {
                // Check for protocol version mismatch
                int protocolVersion = readVarInt(in);
                if (protocolVersion != PROTOCOL_VERSION) {
                    isOnline = false;
                    ping = -1;
                    statusMessage = String.format("Несовпадение версии протокола: ожидается %d, но получено %d", PROTOCOL_VERSION, protocolVersion);
                    lastError = new Exception("Protocol version mismatch");
                    resetStats();
                    return;
                }
            }
            
            lastError = null;
            
        } catch (UnknownHostException e) {
            isOnline = false;
            ping = -1;
            statusMessage = "\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u0430\u0434\u0440\u0435\u0441 \u0441\u0435\u0440\u0432\u0435\u0440\u0430";
            lastError = e;
            resetStats();
        } catch (ConnectException e) {
            isOnline = false;
            ping = -1;
            statusMessage = "\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d";
            lastError = e;
            resetStats();
        } catch (SocketTimeoutException e) {
            isOnline = false;
            ping = -1;
            statusMessage = "\u0422\u0430\u0439\u043c\u0430\u0443\u0442 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f";
            lastError = e;
            resetStats();
        } catch (NoRouteToHostException e) {
            isOnline = false;
            ping = -1;
            statusMessage = "\u041d\u0435\u0442 \u043c\u0430\u0440\u0448\u0440\u0443\u0442\u0430 \u043a \u0441\u0435\u0440\u0432\u0435\u0440\u0443";
            lastError = e;
            resetStats();
        } catch (IOException e) {
            isOnline = false;
            ping = -1;
            statusMessage = "\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f: " + e.getMessage();
            lastError = e;
            resetStats();
        }
    }

    public boolean isOnline() {
        return isOnline;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Exception getLastError() {
        return lastError;
    }

    private void resetStats() {
        playerCount = 0;
        maxPlayers = 0;
        players.clear();
    }

    public List<String> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public long getPing() {
        return ping;
    }
    
    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        while (true) {
            if ((value & 0xFFFFFF80) == 0) {
                out.writeByte(value);
                return;
            }
            out.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }
    }
    
    private int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 0x80) != 0);
        return result;
    }
    
    private void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }
    
    private String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    private void parseServerInfo(String json) {
        try {
            // Manual JSON parsing
            int versionStart = json.indexOf("\"version\":{\"name\":\"");
            if (versionStart != -1) {
                versionStart += 19; // Length of "version":{"name":"
                int versionEnd = json.indexOf("\"", versionStart);
                if (versionEnd != -1) {
                    gameVersion = json.substring(versionStart, versionEnd);
                }
            }

            int playersStart = json.indexOf("\"players\":{\"max\":");
            if (playersStart != -1) {
                playersStart += 17; // Length of "players":{"max":
                int playersEnd = json.indexOf(",", playersStart);
                if (playersEnd != -1) {
                    maxPlayers = Integer.parseInt(json.substring(playersStart, playersEnd));
                }

                int onlineStart = json.indexOf("\"online\":", playersEnd);
                if (onlineStart != -1) {
                    onlineStart += 9; // Length of "online":
                    int onlineEnd = json.indexOf("}", onlineStart);
                    if (onlineEnd != -1) {
                        playerCount = Integer.parseInt(json.substring(onlineStart, onlineEnd));
                    }
                }
            }
        } catch (Exception e) {
            gameVersion = "Unknown";
            maxPlayers = 0;
            playerCount = 0;
        }
    }
}
