package me.kkdevs;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.messages.*;
import com.vk.api.sdk.objects.users.UserXtrCounters;

import java.io.FileInputStream;
import java.util.*;

import static me.kkdevs.VkCore.getActor;
import static me.kkdevs.VkCore.getVk;

public class Loader {

    public static final long START_TIME = System.currentTimeMillis();
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

    public static void main(String[] args) throws Exception {

        long LOAD_TIME = System.currentTimeMillis();

        getVk()
                .messages()
                .getConversations(getActor())
                .execute()
                .getItems()
                .forEach((conversation) -> {
                    try {
                        int id = conversation.getConversation().getPeer().getId();
                        if (id > 2000000000) {
                            System.out.println(id);
                            sendMessage(id, "&#128276; Бот запущен за " + (double) (LOAD_TIME - START_TIME) / 1000 + " сек.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        while (true) {
            Message message = vkCore.getMessage();

            if (message != null) {
                ArrayList<String> list = new ArrayList<String>();
                for (ConversationMember member : getAllConversationMembers(message)) {
                    if (member.getMemberId() > 0) {
                        UserXtrCounters userXtrCounters = getUser(member.getMemberId());
                        if (userXtrCounters != null) {
                            list.add("[id" + userXtrCounters.getId() + "|" + userXtrCounters.getFirstName() + " " + userXtrCounters.getLastName() + "]");
                            //System.out.println(userXtrCounters.getFirstName());
                        }
                    }
                }

                String text = String.join(", ", list);

                System.out.println(text);
                sendMessage(message.getPeerId(), text + "\n\nВсего: " + list.size() + " участников");
            }
        }
    }

    public static List<ConversationMember> getAllConversationMembers(Message message) throws ClientException, ApiException {
        return getVk()
                .messages()
                .getConversationMembers(getActor(), message.getPeerId())
                .execute()
                .getItems();
    }

    public static void sendMessage(Integer peerId, String text) throws ClientException, ApiException {
        getVk().messages().send(getActor())
                .randomId(new Random().nextInt(Integer.MAX_VALUE))
                .peerId(peerId)
                .message(text)
                .execute();
    }

    public static UserXtrCounters getUser(Integer id) throws ClientException, ApiException {
        return getVk()
                .users()
                .get(getActor())
                .userIds(id.toString())
                .execute()
                .get(0);
    }
}