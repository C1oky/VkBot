package me.c1oky;

import com.vk.api.sdk.client.actors.GroupActor;

public class VkBot {

    public final static String DATA_PATH = System.getProperty("user.dir") + "/";
    public static final long START_TIME = System.currentTimeMillis();

    public static void main(String[] args) {
        GroupActor groupActor = new GroupActor(Integer.MAX_VALUE, "accessToken");
        VkCore vkCore = new VkCore(DATA_PATH, groupActor);

        try {
            vkCore.boot();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}