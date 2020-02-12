package me.kkdevs;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.queries.messages.MessagesGetLongPollHistoryQuery;

import java.util.List;

public class VkCore {

    private static VkApiClient vk;
    private static GroupActor actor;
    private static int maxMsgId = -1;
    private static int ts;

    public VkCore(int groupId, String accessToken) throws ClientException, ApiException {
        vk = new VkApiClient(HttpTransportClient.getInstance());
        actor = new GroupActor(
                groupId,
                accessToken
        );

        ts = vk.messages().getLongPollServer(actor).execute().getTs();
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
        return vk;
    }

    public static GroupActor getActor() {
        return actor;
    }
}