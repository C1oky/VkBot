package me.kkdevs;

import com.vk.api.sdk.objects.messages.Message;

import java.io.FileInputStream;
import java.util.Properties;

public class Loader {

    public static VkCore vkCore;

    static {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream("src/main/resources/config.properties"));
            vkCore = new VkCore(Integer.parseInt(properties.getProperty("id")), properties.getProperty("token"));
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(-1);
        }
    }

    public static void main(String[] args) throws InterruptedException {

        while (true) {
            Thread.sleep(200);

            try {
                Message message = vkCore.getMessage();
                if (message != null) {
                    //TODO
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                System.exit(-1);
            }
        }
    }
}