package me.c1oky;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Log4j2
public class VkBot {

    public static final String DATA_PATH = System.getProperty("user.dir") + "/";
    public static final long START_TIME = System.currentTimeMillis();

    @Getter
    private static String gitHash = "dev/unsupported";

    public static void main(String[] args) {
        System.setProperty("log4j.skipJansi", "false");

        // Extract information from the manifest
        InputStream inputStream = VkBot.class.getClassLoader().getResourceAsStream("git.properties");
        try {
            Properties properties = new Properties();
            properties.load(inputStream);
            String abbrev = properties.getProperty("git.commit.id.abbrev");
            if (abbrev != null) {
                VkBot.gitHash = abbrev;
            }
        } catch (IOException e) {/**/}

        VkCore vkCore = new VkCore(DATA_PATH);

        try {
            vkCore.boot();
        } catch (Exception exception) {
            log.fatal("VkBot crashed!", exception);
            System.exit(0);
        }
    }
}