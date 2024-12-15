package com.godpalace.data.database;

import com.godpalace.data.annotation.Data;
import com.godpalace.data.annotation.RemoteDatabase;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RemoteDatabaseEngine {
    private static final Object lock = new Object();

    private RemoteDatabaseEngine() {}

    private static void checkAnnotation(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(RemoteDatabase.class))
            throw new IllegalArgumentException(
                    "Class " + clazz.getName() + " is not annotated with @RemoteDatabase");
    }

    private static void writeVariable(OutputStream out, String name, Object value) throws Exception {
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
        out.flush();
    }

    private static Variable readVariable(InputStream in) throws Exception {
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

    private static void save(String ip, int port, String username, String password,
                             List<Field> fields, Object instance) {
        try {
            SocketChannel channel = SocketChannel.open(new InetSocketAddress(ip, port));
            channel.socket().setSoTimeout(5000);

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            writeUserInfo(bout, username, password);

            int request = 0; // PUT
            bout.write(request);

            for (Field field : fields) {
                writeVariable(bout, field.getName(), field.get(instance));
            }

            bout.flush();
            channel.write(ByteBuffer.wrap(bout.toByteArray()));

            bout.close();
            channel.close();
        } catch (Exception e) {
            throw new RuntimeException("Error while saving variables to remote database", e);
        }
    }

    private static void writeUserInfo(OutputStream bout,
                                      String username, String password) throws Exception {
        byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);

        int nameLength = usernameBytes.length;
        int passwordLength = passwordBytes.length;

        bout.write(nameLength >> 24 & 0xff);
        bout.write(nameLength >> 16 & 0xff);
        bout.write(nameLength >> 8 & 0xff);
        bout.write(nameLength & 0xff);

        bout.write(passwordLength >> 24 & 0xff);
        bout.write(passwordLength >> 16 & 0xff);
        bout.write(passwordLength >> 8 & 0xff);
        bout.write(passwordLength & 0xff);

        bout.write(usernameBytes, 0, nameLength);
        bout.write(passwordBytes, 0, passwordLength);
        bout.flush();
    }

    private static HashMap<String, Object> loadVariables(
            String ip, int port, String username, String password) throws Exception {

        HashMap<String, Object> variables = new HashMap<>();
        SocketChannel channel = SocketChannel.open(new InetSocketAddress(ip, port));
        channel.socket().setSoTimeout(3000);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        writeUserInfo(bout, username, password);

        int request = 1; // LOAD
        bout.write(request);
        bout.flush();

        channel.write(ByteBuffer.wrap(bout.toByteArray()));
        channel.shutdownOutput();
        bout.close();

        bout = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int len;

        while ((len = channel.read(buffer)) != -1) {
            bout.write(buffer.array(), 0, len);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(bout.toByteArray());
        int response = in.read();

        if (response != ResponseType.NO_DATA.getValue()) {
            if (response == ResponseType.OK.getValue()) {
                Variable var;

                while ((var = readVariable(in)) != null) {
                    variables.put(var.name, var.value);
                }
            } else if (response == ResponseType.PASSWORD_ERROR.getValue()) {
                throw new IllegalArgumentException("Username or password is wrong");
            } else if (response == ResponseType.ERROR.getValue()) {
                throw new Exception("Error while loading variables from remote database");
            } else {
                throw new Exception("Unknown response from remote database: " + response);
            }
        }

        in.close();
        bout.close();
        channel.close();

        return variables;
    }

    public static void init(Class<?> clazz, Object instance,
                            String ip, int port, String username, String password) throws Exception {
        checkAnnotation(clazz);
        RemoteDatabase db = clazz.getAnnotation(RemoteDatabase.class);

        if (!InetAddress.getByName(ip).isReachable(2000)) {
            throw new Exception("Cannot reach remote database at " + ip + ":" + port);
        }

        HashMap<String, Object> variables = loadVariables(ip, port, username, password);
        List<Field> fields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(Data.class)) {

                if (Serializable.class.isAssignableFrom(field.getType()) ||
                        field.getType().isPrimitive()) {

                    if ((instance != null) == Modifier.isStatic(field.getModifiers()))
                        throw new IllegalArgumentException(field.getName() + " is " +
                                (instance != null ? "not static" : "static") + " but should be");

                    if (variables.containsKey(field.getName()) &&
                            field.getAnnotation(Data.class).value()) {

                        field.set(instance, variables.get(field.getName()));
                    }

                    fields.add(field);
                } else {
                    throw new IllegalArgumentException("Field is not serializable: " + field.getName());
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                save(ip, port, username, password, fields, instance)));

        long time = db.autoSaveTime();

        if (time != 0) {
            if (time < 0) {
                throw new IllegalArgumentException("Invalid auto save time: " + time);
            }

            TimeUnit unit = db.autoSaveTimeUnit();
            long delay = unit.toMillis(time);

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    save(ip, port, username, password, fields, instance);
                }
            }, delay, delay);
        }
    }

    public record Variable(String name, Object value) {
        @Override
        public String toString() {
            return name + " = " + value;
        }
    }
}
