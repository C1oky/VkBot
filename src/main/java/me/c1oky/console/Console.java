package me.c1oky.console;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

@Log4j2
public class Console {

    @Getter
    private static Terminal terminal;

    static {
        try {
            terminal = TerminalBuilder.terminal();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private LineReader reader;

    public Console() {
        this.setReader(this.buildReader(LineReaderBuilder.builder().terminal(terminal)));
    }

    public void start() {
        String line;
        while (true) {
            try {
                line = this.reader.readLine();
            } catch (EndOfFileException ignored) {
                // Continue reading after EOT
                continue;
            }

            if (line == null) {
                break;
            }

            this.processInput(line);
        }

        this.setReader(null);
    }

    private void setReader(LineReader newReader) {
        if (newReader != null && newReader.getTerminal() != terminal) {
            throw new IllegalArgumentException("Reader was not created with terminal");
        }

        this.reader = newReader;
    }

    private void processInput(String input) {
        String inputString = input.trim();
        if (!inputString.isEmpty()) {
            //TODO: Обработка введённых данных в консоли
        }
    }

    private LineReader buildReader(LineReaderBuilder builder) {
        builder.appName("VkBot");
        builder.option(LineReader.Option.HISTORY_BEEP, false);
        builder.option(LineReader.Option.HISTORY_IGNORE_DUPS, true);
        builder.option(LineReader.Option.HISTORY_IGNORE_SPACE, true);

        LineReader reader = builder.build();
        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);
        reader.unsetOpt(LineReader.Option.INSERT_TAB);
        return reader;
    }
}