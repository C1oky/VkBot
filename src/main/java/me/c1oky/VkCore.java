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
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import me.c1oky.console.Console;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.util.*;

import static me.c1oky.VkBot.*;

@Log4j2
public class VkCore {

    @Getter
    private static VkCore instance;

    @Getter
    private final String dataPath;

    @Getter
    private final VkApiClient vkApiClient;
    @Getter
    private GroupActor groupActor;
    private String groupToken;
    private int groupId;
    private int maxMsgId = -1;
    private int ts;

    private Thread handlerThread;

    private final Console console;
    private final ConsoleThread consoleThread;

    private Properties properties;

    public VkCore(String dataPath) {
        VkCore.instance = this;
        this.dataPath = dataPath;
        this.console = new Console();
        this.consoleThread = new ConsoleThread();
        this.vkApiClient = new VkApiClient(HttpTransportClient.getInstance());
    }

    public void boot() {
        this.consoleThread.start();
        this.properties = new Properties();

        if (!new File(this.dataPath + "bot.properties").exists()) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Добро пожаловать в мастер настройки VkBot!");
            this.properties.setProperty("groupId", this.getValues(scanner, "Введите айди группы (буквенный айди в настоящее время не поддерживается!): "));
            this.properties.setProperty("ownerId", this.getValues(scanner, "Введите айди профиля владельца (буквенный айди в настоящее время не поддерживается!): "));
            this.properties.setProperty("token", this.getValues(scanner, "Введите токен группы: "));
            log.info("Настройка завершена!");
        }

        this.loadProperties();

        this.groupToken = this.properties.getProperty("token", "accessToken");
        this.groupId = Integer.parseInt(this.properties.getProperty("groupId", "0"));
        this.groupActor = new GroupActor(this.groupId, this.groupToken);
        log.info("Создан объект GroupActor для сообщества со следующим индентификатором: " + this.groupActor.getGroupId());

        log.info("DataPath Directory: {}", this.dataPath);

        this.initHandler();
        this.start();
    }

    private void start() {
        this.handlerThread.start();
        log.info("VkBot started (" + (Math.round(System.currentTimeMillis() - START_TIME) / 1000d) + " sec.)");
    }

    private void initHandler() {
        handlerThread = new Thread(() -> {
            try {
                while (true) {
                    Message message = getMessage();

                    if (message != null) {
                        //TODO: Обработка сообщений в боте
                    }
                }
            } catch (Exception exception) {
                log.fatal("Handler error", exception);
            }
        }, "Handler Thread");
    }

    public void sendMessage(Integer peerId, String text) throws ClientException, ApiException {
        vkApiClient.messages().send(groupActor)
                .randomId(new Random().nextInt(Integer.MAX_VALUE))
                .peerId(peerId)
                .message(text)
                .execute();
    }

    public UserXtrCounters getUser(Integer id) throws ClientException, ApiException {
        return vkApiClient
                .users()
                .get(groupActor)
                .userIds(id.toString())
                .execute()
                .get(0);
    }

    public List<ConversationWithMessage> getConversations() throws ClientException, ApiException {
        return vkApiClient
                .messages()
                .getConversations(groupActor)
                .execute()
                .getItems();
    }

    public List<ConversationMember> getAllConversationMembers(Message message) throws ClientException, ApiException {
        return vkApiClient
                .messages()
                .getConversationMembers(groupActor, message.getPeerId())
                .execute()
                .getItems();
    }

    public Message getMessage() throws ClientException, ApiException {
        this.ts = vkApiClient.messages().getLongPollServer(groupActor).execute().getTs();

        MessagesGetLongPollHistoryQuery eventsQuery = vkApiClient.messages()
                .getLongPollHistory(groupActor)
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
                ts = vkApiClient.messages()
                        .getLongPollServer(groupActor)
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

    private String getValues(Scanner scanner, String message) {
        while (true) {
            System.out.println(message);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
    }

    private void loadProperties() {
        log.info("Загрузка файла конфигурации...");
        File propertiesFile = new File(this.dataPath, "bot.properties");

        try (InputStream stream = new FileInputStream(propertiesFile)) {
            this.properties.load(stream);
        } catch (FileNotFoundException | NoSuchFileException e) {
            this.saveProperties();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveProperties() {
        File propertiesFile = new File(this.dataPath, "bot.properties");

        try (OutputStream stream = new FileOutputStream(propertiesFile)) {
            this.properties.store(stream, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class ConsoleThread extends Thread {
        @Override
        public void run() {
            try {
                console.start();
            } catch (Exception exception) {
                log.fatal("Console crash", exception);
            }
        }
    }
}