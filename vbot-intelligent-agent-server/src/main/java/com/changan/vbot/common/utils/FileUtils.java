package com.changan.vbot.common.utils;

import io.micrometer.core.instrument.util.IOUtils;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class FileUtils {
    private FileUtils() {
    }

    public static String readFile(File file) throws FileNotFoundException {
        FileInputStream chartFileInputStream = new FileInputStream(file);
        return IOUtils.toString(chartFileInputStream, StandardCharsets.UTF_8);
    }

    public static String readFile(InputStream fileInputStream) {
        return IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
    }

    public static String readJarFile(String filePath) {
        InputStream inputStream = FileUtils.class.getClassLoader().getResourceAsStream(filePath);
        if (Objects.nonNull(inputStream)) {
            return FileUtils.readFile(inputStream);
        } else {
            try {
                return readFile(ResourceUtils.getFile(filePath));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
