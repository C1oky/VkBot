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
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

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
        Thread.currentThread().setName("Main Thread");

        // Extract information from the manifest
        String buildVersion = "dev/unsupported";
        InputStream inputStream = VkBot.class.getClassLoader().getResourceAsStream("git.properties");
        try {
            Properties properties = new Properties();
            properties.load(inputStream);
            if ((buildVersion = properties.getProperty("git.commit.id.abbrev")) == null) {
                buildVersion = "dev/unsupported";
            }
        } catch (IOException e) {/**/}

        VkBot.setGitHash(buildVersion);

        log.info("Starting VkBot git-{}", VkBot.getGitHash());

        this.properties = new Properties();

        if (!new File(this.dataPath + "bot.properties").exists()) {
            log.info("Welcome to the VkBot setup wizard!");
            LineReader lineReader = LineReaderBuilder.builder().terminal(Console.getTerminal()).build();
            this.properties.setProperty("groupId", this.getValues(lineReader, "Enter the group’s ID (letter ID is currently not supported!):"));
            this.properties.setProperty("ownerId", this.getValues(lineReader, "Enter the owner profile’s idi (letter id is currently not supported!):"));
            this.properties.setProperty("token", this.getValues(lineReader, "Enter the group token:"));
            log.info("Setup is complete!");
        }

        this.consoleThread.start();

        this.loadProperties();

        this.groupToken = this.properties.getProperty("token", "accessToken");
        this.groupId = Integer.parseInt(this.properties.getProperty("groupId", "00000000"));
        this.groupActor = new GroupActor(this.groupId, this.groupToken);
        log.info("A GroupActor object has been created for the community with the following identifier: {}", this.groupActor.getGroupId());

        log.info("DataPath Directory: {}", this.dataPath);

        this.initHandler();
        this.start();
    }

    private void start() {
        this.handlerThread.start();
        log.info("VkBot started ({} sec.)", Math.round(System.currentTimeMillis() - START_TIME) / 1000d);
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
                log.fatal("Error processing requests!", exception);
                System.exit(0);
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

    private String getValues(LineReader lineReader, String message) {
        while (true) {
            System.out.println(message);
            String value = lineReader.readLine("> ").trim();
            if (!value.isEmpty()) {
                System.out.println("\u001B[32mData accepted. Move on...\u001B[0m");
                return value;
            }
        }
    }

    private void loadProperties() {
        log.info("Loading configuration file ...");
        File propertiesFile = new File(this.dataPath, "bot.properties");

        try (InputStream stream = new FileInputStream(propertiesFile)) {
            this.properties.load(stream);
        } catch (FileNotFoundException | NoSuchFileException exception) {
            this.saveProperties();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void saveProperties() {
        File propertiesFile = new File(this.dataPath, "bot.properties");

        try (OutputStream stream = new FileOutputStream(propertiesFile)) {
            this.properties.store(stream, "");
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private class ConsoleThread extends Thread {
        @Override
        public synchronized void run() {
            console.start();
        }
    }
}