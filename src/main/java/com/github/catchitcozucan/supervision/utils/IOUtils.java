package com.github.catchitcozucan.supervision.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Slf4j
public class IOUtils {
    private IOUtils() {
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static Optional<InputStream> resourceToStream(Resource resource) {
        return resourceToStream(resource, null);
    }

    public static Optional<InputStream> resourceToStream(Resource resource, String subFolder) {
        try {
            InputStream in = null;
            if (StringUtils.hasContents(subFolder)) {
                in = Thread.currentThread().getContextClassLoader()
                        .getResource(subFolder + "/" + resource.getFilename()).openConnection().getInputStream();
            } else {
                in = Thread.currentThread().getContextClassLoader()
                        .getResource(resource.getFilename()).openConnection().getInputStream();
            }
            return Optional.of(in);
        } catch (IOException e) {
            log.error(String.format(String.format("Failed to load %s", resource.getFilename())), e);
        }
        return Optional.empty();
    }
}
