package ru.isshepelev.tgrepository.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.isshepelev.tgrepository.config.BotConfig;
import ru.isshepelev.tgrepository.constant.Const;
import ru.isshepelev.tgrepository.entity.User;
import ru.isshepelev.tgrepository.repository.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    private final UserRepository userRepository;
    private final BotConfig botConfig;

    public TelegramBot(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.botConfig = new BotConfig();
        List<BotCommand> listCommands = new ArrayList();
        listCommands.add(new BotCommand("/start", "start bot"));
        listCommands.add(new BotCommand("/info", "information about you"));
        listCommands.add(new BotCommand("/project", "my repository"));
        listCommands.add(new BotCommand("/git", "my git repository (link)"));


        try {
            this.execute(new SetMyCommands(listCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("error setting bot command list" + e);
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    startCommand(chatId, update.getMessage().getChat().getFirstName());
                    registerUser(update.getMessage());
                    mainMessage(chatId);
                    break;
                case "/reg":
                    registerUser(update.getMessage());
                    break;
                case "/info":
                    infoUser(chatId);
                    break;
                case "/git":
                    gitLink(update.getMessage().getChatId());
                    break;
                case "/help":
                    helpMessage(update.getMessage().getChatId());
                    break;
                default:
                    sendMessage(chatId, "Извините, неизветсная команда!");
                    break;
            }
        } else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callBackData.equals("BACK_FOR_MAIN_MESSAGE")) {
                mainMessage(chatId);
            }
            if (callBackData.equals("ACCOUNT_BUTTON")) {
                infoUser(chatId);
            }
            if (callBackData.equals("HELP_BUTTON")){
                helpMessage(chatId);
            }


        }
    }

    private void gitLink(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Мой github");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsLine = new ArrayList<>();
        List<InlineKeyboardButton> rowLine = new ArrayList<>();
        var gitLink = new InlineKeyboardButton();
        gitLink.setText("Заглянуть!");
        gitLink.setCallbackData("GIT_LINK_BUTTON");
        gitLink.setUrl("https://github.com/isshepelev");

        rowLine.add(gitLink);
        rowsLine.add(rowLine);

        markup.setKeyboard(rowsLine);
        sendMessage.setReplyMarkup(markup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("error " + e.getMessage());
        }
    }


    public void sendMessage(long chatId, String messageText) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(messageText);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка в отправке сообщения " + e);
        }
    }

    public void startCommand(long chatId, String name) {
        String answer = "Доброго времени суток, " + name + ",\n" + Const.START_MESSAGE_TEXT;
        sendMessage(chatId, answer);

    }

    public void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            long chatId = message.getChatId();
            Chat chat = message.getChat();
            User user = new User();
            user.setDate(LocalDate.now());
            user.setFirstname(chat.getFirstName());
            user.setLastname(chat.getLastName());
            user.setId(chatId);
            user.setUsername(message.getForwardSenderName());
            userRepository.save(user);
            log.info("пользователь добавлен " + user);
        } else
            log.error("пользователь уже существует " + userRepository.findById(message.getChatId()));
    }

    private void infoUser(long chatId) {
        Optional<User> userOptional = userRepository.findById(chatId);
        if (userOptional.isPresent()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            User user = userOptional.get();
            String answer = "Информация о вас\n\n" +
                    "Ваш ID: " + user.getId() + "\n" +
                    "Имя: " + user.getFirstname() + "\n" +
                    "Дата регистарции: " + user.getDate();
            sendMessage.setText(answer);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsLine = new ArrayList<>();
            List<InlineKeyboardButton> rowLine = new ArrayList<>();
            var back = new InlineKeyboardButton();
            back.setText("Назад");
            back.setCallbackData("BACK_FOR_MAIN_MESSAGE");

            rowLine.add(back);
            rowsLine.add(rowLine);

            markup.setKeyboard(rowsLine);
            sendMessage.setReplyMarkup(markup);

            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("error " + e.getMessage());
            }
        }
    }

    private void mainMessage(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Основные возможности бота:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsLine = new ArrayList<>();
        List<InlineKeyboardButton> rowLine1 = new ArrayList<>();
        List<InlineKeyboardButton> rowLine2 = new ArrayList<>();
        var account = new InlineKeyboardButton();
        account.setText("Aккаунт");
        account.setCallbackData("ACCOUNT_BUTTON");
        var project = new InlineKeyboardButton();
        project.setText("Мои проекты");
        project.setCallbackData("PROJECT_BUTTON");
        var github = new InlineKeyboardButton();
        github.setText("Мой Github");
        github.setCallbackData("GITHUB_BUTTON");
        github.setUrl("https://github.com/isshepelev");
        var help = new InlineKeyboardButton();
        help.setText("Помощь");
        help.setCallbackData("HELP_BUTTON");

        rowLine1.add(account);
        rowLine1.add(project);
        rowsLine.add(rowLine1);


        rowLine2.add(help);
        rowLine2.add(github);
        rowsLine.add(rowLine2);

        markup.setKeyboard(rowsLine);
        sendMessage.setReplyMarkup(markup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("error " + e.getMessage());
        }
    }

    private void helpMessage(long chatId){
        String answer = "Добро пожаловать в помощь! Вы можете использовать следующие команды:\n" +
                "\n" +
                "\uD83D\uDC64 /info - Просмотр информации о вашем профиле.\n" +
                "\uD83D\uDCC2 /project - Просмотр списка ваших проектов и их описаний.\n" +
                "\uD83C\uDF10 /git - Ссылка на ваш аккаунт на GitHub.\n" +
                "\uD83D\uDE80 /start - Запустить бота.";
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(answer);
        sendMessage.setChatId(String.valueOf(chatId));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsLine = new ArrayList<>();
        List<InlineKeyboardButton> rowLine = new ArrayList<>();
        var back = new InlineKeyboardButton();
        back.setText("Назад!");
        back.setCallbackData("BACK_FOR_MAIN_MESSAGE");

        rowLine.add(back);
        rowsLine.add(rowLine);

        markup.setKeyboard(rowsLine);
        sendMessage.setReplyMarkup(markup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("error " + e.getMessage());
        }
    }

    public void mainRepositoryMessage(long chatId){
        String answer = "\uD83D\uDCC2 Выберите один из репозиториев:";
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(answer);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsLine1 = new ArrayList<>();
        List<InlineKeyboardButton> rowLine1 = new ArrayList<>();

        List<List<InlineKeyboardButton>> rowsLine2 = new ArrayList<>();
        List<InlineKeyboardButton> rowLine2 = new ArrayList<>();

        List<List<InlineKeyboardButton>> rowsLine3 = new ArrayList<>();
        List<InlineKeyboardButton> rowLine3 = new ArrayList<>();
    }
}