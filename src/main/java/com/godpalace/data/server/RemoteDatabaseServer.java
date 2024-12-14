package com.godpalace.data.server;

import com.godpalace.data.database.RequestType;
import com.godpalace.data.database.ResponseType;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RemoteDatabaseServer {
    private final ServerSocketChannel server;
    private final HashMap<String, String> users;
    private final HashMap<String, Object> database;
    private final File databaseFile;

    private boolean isRunning = false;

    public RemoteDatabaseServer(InetSocketAddress address, File databaseFile) throws Exception {
        this.users = new HashMap<>();
        this.database = new HashMap<>();
        this.databaseFile = databaseFile;

        this.server = ServerSocketChannel.open();
        this.server.socket().bind(address);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                save();
                server.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public void addUser(String name, String password) {
        users.put(name, password);
    }

    public void removeUser(String name) {
        users.remove(name);
    }

    public void stop() {
        isRunning = false;
    }

    private Variable readVariable(InputStream in) throws Exception {
        int len;
        int nameLength, valueLength;
        byte[] lengthBytes, nameBytes, valueBytes;

        lengthBytes = new byte[4];
        len = in.read(lengthBytes, 0, lengthBytes.length);
        if (len == -1) return null;
        nameLength = (lengthBytes[0] & 0xff) << 24 | (lengthBytes[1] & 0xff) << 16 |
                (lengthBytes[2] & 0xff) << 8 | (lengthBytes[3] & 0xff);

        lengthBytes = new byte[4];
        len = in.read(lengthBytes, 0, lengthBytes.length);
        if (len == -1) return null;
        valueLength = (lengthBytes[0] & 0xff) << 24 | (lengthBytes[1] & 0xff) << 16 |
                (lengthBytes[2] & 0xff) << 8 | (lengthBytes[3] & 0xff);

        nameBytes = new byte[nameLength];
        len = in.read(nameBytes, 0, nameLength);
        if (len == -1) return null;

        valueBytes = new byte[valueLength];
        len = in.read(valueBytes, 0, valueLength);
        if (len == -1) return null;

        String name = new String(nameBytes, 0, nameLength, StandardCharsets.UTF_8);
        ByteArrayInputStream bin = new ByteArrayInputStream(valueBytes);
        ObjectInputStream oin = new ObjectInputStream(bin);
        Object value = oin.readObject();

        oin.close();
        bin.close();

        return new Variable(name, value);
    }

    private void writeVariable(OutputStream out, String name, Object value) throws Exception {
        byte[] lengthBytes, nameBytes, valueBytes;
        int nameLength, valueLength;

        nameBytes = name.getBytes(StandardCharsets.UTF_8);
        nameLength = nameBytes.length;

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(value);
        oout.flush();
        valueBytes = bout.toByteArray();
        valueLength = valueBytes.length;

        oout.close();
        bout.close();

        lengthBytes = new byte[4];
        lengthBytes[0] = (byte) (nameLength >> 24 & 0xff);
        lengthBytes[1] = (byte) (nameLength >> 16 & 0xff);
        lengthBytes[2] = (byte) (nameLength >> 8 & 0xff);
        lengthBytes[3] = (byte) (nameLength & 0xff);
        out.write(lengthBytes, 0, lengthBytes.length);

        lengthBytes = new byte[4];
        lengthBytes[0] = (byte) (valueLength >> 24 & 0xff);
        lengthBytes[1] = (byte) (valueLength >> 16 & 0xff);
        lengthBytes[2] = (byte) (valueLength >> 8 & 0xff);
        lengthBytes[3] = (byte) (valueLength & 0xff);
        out.write(lengthBytes, 0, lengthBytes.length);

        out.write(nameBytes, 0, nameLength);
        out.write(valueBytes, 0, valueLength);
    }

    private boolean checkUser(InputStream in) throws Exception {
        int len;
        int nameLength, passwordLength;
        byte[] lengthBytes, nameBytes, passwordBytes;

        lengthBytes = new byte[4];
        len = in.read(lengthBytes, 0, lengthBytes.length);
        if (len == -1) return false;
        nameLength = (lengthBytes[0] & 0xff) << 24 | (lengthBytes[1] & 0xff) << 16 |
                (lengthBytes[2] & 0xff) << 8 | (lengthBytes[3] & 0xff);

        lengthBytes = new byte[4];
        len = in.read(lengthBytes, 0, lengthBytes.length);
        if (len == -1) return false;
        passwordLength = (lengthBytes[0] & 0xff) << 24 | (lengthBytes[1] & 0xff) << 16 |
                (lengthBytes[2] & 0xff) << 8 | (lengthBytes[3] & 0xff);

        nameBytes = new byte[nameLength];
        len = in.read(nameBytes, 0, nameLength);
        if (len == -1) return false;

        passwordBytes = new byte[passwordLength];
        len = in.read(passwordBytes, 0, passwordLength);
        if (len == -1) return false;

        String name = new String(nameBytes, 0, nameLength, StandardCharsets.UTF_8);
        String password = new String(passwordBytes, 0, passwordLength, StandardCharsets.UTF_8);

        if (users.containsKey(name)) {
            return users.get(name).equals(password);
        } else {
            return false;
        }
    }

    public void save() {
        // TODO: save variables to file
        try (FileOutputStream out = new FileOutputStream(databaseFile);
             GZIPOutputStream gout = new GZIPOutputStream(out)) {

            for (Map.Entry<String, Object> entry : database.entrySet()) {
                writeVariable(gout, entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        isRunning = true;

        if (databaseFile.exists()) {
            try (FileInputStream in = new FileInputStream(databaseFile);
                 GZIPInputStream gin = new GZIPInputStream(in)) {

                Variable var;
                while ((var = readVariable(gin)) != null) {
                    database.put(var.name, var.value);
                }
            } catch (Exception e) {
                isRunning = false;
                throw new RuntimeException(e);
            }
        }

        try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
            while (isRunning) {
                SocketChannel channel = server.accept();
                System.out.println("New connection from " + channel.getRemoteAddress());

                ByteBuffer buffer = ByteBuffer.allocate(4);
                int len = channel.read(buffer);

                if (len == -1) continue;
                int size = buffer.getInt(0);

                buffer.clear();
                buffer = ByteBuffer.allocate(size);
                len = channel.read(buffer);

                if (len == -1) continue;
                byte[] bytes = buffer.array();
                bout.write(bytes, 0, len);

                GZIPInputStream in = new GZIPInputStream(
                        new ByteArrayInputStream(bout.toByteArray()));
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                GZIPOutputStream gout = new GZIPOutputStream(stream);

                if (checkUser(in)) {
                    int request = in.read();

                    if (request == RequestType.PUT.getValue()) {
                        Variable var = readVariable(in);

                        if (var != null) {
                            database.put(var.name, var.value);
                            gout.write(ResponseType.OK.getValue());
                        }
                    } else if (request == RequestType.LOAD.getValue()) {
                        if (!database.isEmpty()) {
                            for (Map.Entry<String, Object> entry : database.entrySet()) {
                                writeVariable(gout, entry.getKey(), entry.getValue());
                            }
                        } else {
                            gout.write(ResponseType.NO_DATA.getValue());
                        }
                    }
                } else {
                    gout.write(ResponseType.ERROR.getValue());
                }

                channel.write(ByteBuffer.wrap(stream.toByteArray()));
                gout.close();
                stream.close();

                in.close();
                channel.close();
                bout.reset();
            }
        } catch (Exception e) {
            isRunning = false;
            throw new RuntimeException(e);
        }
    }

    protected record Variable(String name, Object value) {
        @Override
        public String toString() {
            return name + " = " + value;
        }
    }
}
