package com.springbot.springbot.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.vdurmont.emoji.EmojiParser;

import lombok.extern.slf4j.Slf4j;
import com.springbot.springbot.config.BotConfig;
import com.springbot.springbot.model.User;
import com.springbot.springbot.model.UserRepository;

@Slf4j
@Component
public class TeleBot extends TelegramLongPollingBot{
    @Autowired
    private UserRepository userRepository;
    final BotConfig config;

    static final String HELP_TEXT = "There is help message. This bot can be used to send commands, save data about users, etc.";
    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";

    public TeleBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList();
        listOfCommands.add(new BotCommand("/start", "Get welcome message."));
        listOfCommands.add(new BotCommand("/register", "Register as new user."));
        listOfCommands.add(new BotCommand("/mydata", "Info about user."));
        listOfCommands.add(new BotCommand("/removedata", "Remove info about user."));
        listOfCommands.add(new BotCommand("/settings", "Change chat setting."));
        listOfCommands.add(new BotCommand("/help", "How to use bot."));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            log.error("Error setting bot commands list: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update arg0) {
        if(arg0.hasMessage() && arg0.getMessage().hasText()) {
            String messageText = arg0.getMessage().getText();
            long chatId = arg0.getMessage().getChatId();

            if (messageText.contains("/send") && config.getBotOwner() == chatId)  {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user : users) {
                    sendMessage(user.getChatId(), textToSend, user.getUserName());
                }
            } else {
                switch (messageText) {
                    case "/start":
                        registerUser(arg0.getMessage());
                        commandStartRecieve(chatId, arg0.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        sendMessage(chatId, HELP_TEXT, arg0.getMessage().getChat().getFirstName()); 
                        break;
                    case "/register":
                        commandRegisterNewUser(arg0.getMessage().getChatId());
                        break;    
                    default:
                        sendMessage(chatId, "Sorry, this don`t work yet.", arg0.getMessage().getChat().getFirstName());
                    }   
            }
        } else if (arg0.hasCallbackQuery()) {
            String callbackData = arg0.getCallbackQuery().getData();
            long messageId = arg0.getCallbackQuery().getMessage().getMessageId();
            long chatId = arg0.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals(YES_BUTTON)) {
                String textAnswer = "You pressed YES";
                executeMessageCallBack(textAnswer, chatId, messageId);
            } 
            else if (callbackData.equals(NO_BUTTON)) {
                String textAnswer = "You pressed NO";
                executeMessageCallBack(textAnswer, chatId, messageId);
            }
        }
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }
    
    private void commandStartRecieve(long chatId, String name) {
        SendMessage answer = new SendMessage();
        answer.setChatId(chatId);
        answer.setText(EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you." + "  :blush:"));
        keyboardMainWindow(answer);
        executeMessageSend(answer);
    }

    private void commandRegisterNewUser(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you want to register as new user?");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        var yesButtonInLine = new InlineKeyboardButton();
        yesButtonInLine.setText("Yes");
        yesButtonInLine.setCallbackData(YES_BUTTON);
        var noButtonInLine = new InlineKeyboardButton();
        noButtonInLine.setText("No");
        noButtonInLine.setCallbackData(NO_BUTTON);
        
        row1.add(yesButtonInLine);        
        row1.add(noButtonInLine);
        rowsInLine.add(row1);

        keyboardMarkup.setKeyboard(rowsInLine);
        message.setReplyMarkup(keyboardMarkup);

        executeMessageSend(message);
    }

    private void sendMessage(long chatId, String textToSend, String name) {
        SendMessage messageText = new SendMessage();
        messageText.setChatId(String.valueOf(chatId));
        messageText.setText(textToSend);      

        executeMessageSend(messageText);
    }

    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();
            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisterAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User saved: " + user.getUserName() + " registered.");
        }
    }

    private void keyboardMainWindow(SendMessage messageText) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Weather");
        row.add("Get random joke");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("Register");
        row.add("Check my data");
        row.add("Delete my data");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        messageText.setReplyMarkup(keyboardMarkup);
    }

    private void executeMessageCallBack(String textAnswer, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
                message.setChatId(String.valueOf(chatId));
                message.setText(textAnswer);
                message.setMessageId((int) messageId);

                try {
                    execute(message);
                }
                catch (TelegramApiException e) {
                    log.error("Error occured: " + e.getMessage());
                }
    }

    private void executeMessageSend(SendMessage message) {
        try {
            execute(message);
            log.info("Replied to user " + message.getChatId() + ":::" + message.getText());
        }
        catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }
}
