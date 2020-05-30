package me.c1oky;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.ConversationMember;
import com.vk.api.sdk.objects.messages.ConversationWithMessage;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.objects.users.UserXtrCounters;
import com.vk.api.sdk.queries.messages.MessagesGetLongPollHistoryQuery;
import net.kronos.rkon.core.Rcon;

import java.io.FileInputStream;
import java.util.*;

import static me.c1oky.Loader.*;

public class VkCore {

    private final Properties properties = new Properties();
    private static VkCore instance;
    private VkApiClient vk;
    private GroupActor actor;
    private int maxMsgId = -1;
    private int ts;

    public VkCore() {
        instance = this;

        try {
            properties.load(new FileInputStream("src/main/resources/config.properties"));

            vk = new VkApiClient(HttpTransportClient.getInstance());
            actor = new GroupActor(
                    Integer.parseInt(properties.getProperty("id")),
                    properties.getProperty("token")
            );

            ts = vk.messages().getLongPollServer(actor).execute().getTs();

            long LOAD_TIME = System.currentTimeMillis();

            getConversations().forEach(conversation -> {
                try {
                    int id = conversation.getConversation().getPeer().getId();
                    if (id > 2000000000) {
                        sendMessage(id, "&#128276; Бот запущен за " + (double) (LOAD_TIME - START_TIME) / 1000 + " сек.");
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void start() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Message message = getMessage();

                    if (message != null) {
                        if (message.getFromId() == 494366746) { //Cool system of rights :D
                            if (message.getText().startsWith("/")) {
                                Rcon rcon = new Rcon(properties.getProperty("ip"), Integer.parseInt(properties.getProperty("port")), properties.getProperty("rcon-password").getBytes());
                                String exec = rcon.command(message.getText().substring(1));
                                sendMessage(message.getPeerId(), exec);
                            }
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });

        thread.start();
    }

    public void sendMessage(Integer peerId, String text) throws ClientException, ApiException {
        vk.messages().send(actor)
                .randomId(new Random().nextInt(Integer.MAX_VALUE))
                .peerId(peerId)
                .message(text)
                .execute();
    }

    public UserXtrCounters getUser(Integer id) throws ClientException, ApiException {
        return vk
                .users()
                .get(actor)
                .userIds(id.toString())
                .execute()
                .get(0);
    }

    public List<ConversationWithMessage> getConversations() throws ClientException, ApiException {
        return vk
                .messages()
                .getConversations(actor)
                .execute()
                .getItems();
    }

    public List<ConversationMember> getAllConversationMembers(Message message) throws ClientException, ApiException {
        return vk
                .messages()
                .getConversationMembers(actor, message.getPeerId())
                .execute()
                .getItems();
    }

    public Message getMessage() throws ClientException, ApiException {
        MessagesGetLongPollHistoryQuery eventsQuery = vk.messages()
                .getLongPollHistory(actor)
                .ts(ts);

        if (maxMsgId > 0) {
            eventsQuery.maxMsgId(maxMsgId);
        }

        List<Message> messages = eventsQuery
                .execute()
                .getMessages()
                .getItems();

        if (!messages.isEmpty()) {
            try {
                ts = vk.messages()
                        .getLongPollServer(actor)
                        .execute()
                        .getTs();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        if (!messages.isEmpty() && !messages.get(0).isOut()) {
            int messageId = messages.get(0).getId();
            if (messageId > maxMsgId) {
                maxMsgId = messageId;
            }

            return messages.get(0);
        }

        return null;
    }

    public static VkApiClient getVk() {
        return instance.vk;
    }

    public static GroupActor getActor() {
        return instance.actor;
    }

    public static VkCore getInstance() {
        return instance;
    }
}