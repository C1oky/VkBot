package me.c1oky;

import com.vk.api.sdk.client.actors.GroupActor;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VkBot {

    public static final String DATA_PATH = System.getProperty("user.dir") + "/";
    public static final long START_TIME = System.currentTimeMillis();

    public static void main(String[] args) {
        System.setProperty("log4j.skipJansi", "false");

        GroupActor groupActor = new GroupActor(Integer.MAX_VALUE, "accessToken");
        VkCore vkCore = new VkCore(DATA_PATH, groupActor);

        try {
            vkCore.boot();
        } catch (Exception exception) {
            log.fatal("VkBot crashed!", exception);
        }
    }
}