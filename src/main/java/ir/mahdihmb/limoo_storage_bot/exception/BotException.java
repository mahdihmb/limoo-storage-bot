package ir.mahdihmb.limoo_storage_bot.exception;

import ir.mahdihmb.limoo_storage_bot.core.MessageService;

import static ir.mahdihmb.limoo_storage_bot.util.GeneralUtils.errorText;

public class BotException extends Throwable {

    public BotException(String message) {
        super(errorText(message));
    }

    public static BotException create(String message) {
        return new BotException(message);
    }

    public static BotException createWithI18n(String i18nKey) {
        return new BotException(MessageService.get(i18nKey));
    }

    public static BotException createWithI18n(String i18nKey, String msgPrefix) {
        return new BotException(msgPrefix + MessageService.get(i18nKey));
    }
}
