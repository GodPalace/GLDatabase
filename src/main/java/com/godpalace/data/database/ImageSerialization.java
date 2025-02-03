package com.godpalace.data.database;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Objects;

public final class ImageSerialization implements Serializable {
    public final byte[] image;

    public ImageSerialization(byte[] image) {
        this.image = image;
    }

    public ImageSerialization(Class<?> clazz, String path) {
        this(Objects.requireNonNull(clazz.getResource(path)));
    }

    public ImageSerialization(URL url) {
        try (InputStream in = url.openStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            ImageIO.write(ImageIO.read(in), "PNG", out);
            this.image = out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ImageSerialization(InputStream in) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedImage image = ImageIO.read(in);
            ImageIO.write(image, "PNG", out);
            this.image = out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ImageSerialization(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", out);
            this.image = out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public BufferedImage getImage() {
        try (ByteArrayInputStream in = new ByteArrayInputStream(image)) {
            return ImageIO.read(in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
