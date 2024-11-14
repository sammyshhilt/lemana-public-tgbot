package lemana.practice.tgbot.bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.contact
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import lemana.practice.tgbot.session.UserSession
import lemana.practice.tgbot.session.context.UserSessionContext
import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class TgBot(
    @Value("\${bot.token}") private val token: String,
    @Value("\${bot.name}") private val botName: String
) {

    companion object : KLogging()

    @Bean
    fun getBot(): Bot {
        val bot = bot {
            token = this@TgBot.token
            timeout = 60

            dispatch {
                command("start") {
                    val chatId = update.message?.chat?.id ?: return@command
                    val nickname = update.message?.from?.username ?: "Без имени"
                    val userSession = UserSessionContext.getOrCreateSession(chatId, nickname)
                    userSession.lastMessageId = update.message?.messageId

                    val requestContactButton = KeyboardButton(
                        text = "Поделиться номером",
                        requestContact = true
                    )

                    val keyboardMarkup = KeyboardReplyMarkup(
                        keyboard = listOf(listOf(requestContactButton)),
                        resizeKeyboard = true,
                        oneTimeKeyboard = true
                    )

                    bot.sendMessage(
                        ChatId.fromId(chatId),
                        "Пожалуйста, нажмите кнопку \"Поделиться номером\" для продолжения диалога. Продолжая диалог, Вы предоставляете компании ООО «ЛЕ МОНЛИД» свое согласие на коммуникацию, сбор, обработку и хранение персональных данных в соответствии с условиями, размещенными на нашем официальном сайте - https://lemanapro.ru/legal/soglasie-na-obrabotku-personalnyh-dannyh/",
                        replyMarkup = keyboardMarkup
                    )
                }

                command("equeue") {
                    val chatId = update.message?.chat?.id ?: return@command
                    handleUserEntriesCommand(chatId, bot)
                    val messageId = update.message!!.messageId
                    bot.deleteMessage(ChatId.fromId(chatId), messageId)
                }

                text {
                    val chatId = update.message?.chat?.id ?: return@text
                    val messageText = update.message?.text

                    if (messageText == "Мои записи на проектирование")
                    {
                        handleUserEntriesCommand(chatId, bot)
                        val messageId = update.message!!.messageId
                        bot.deleteMessage(ChatId.fromId(chatId), messageId)
                    }
                }

                callbackQuery {
                    val callbackData = update.callbackQuery?.data
                    logger.info { "callbackQuery - callbackData: $callbackData" }
                    val chatId = update.callbackQuery?.message?.chat?.id ?: return@callbackQuery
                    val currentUserSession = UserSessionContext.getSession(chatId)
                    val callbackQueryId = update.callbackQuery?.id
                    currentUserSession?.lastMessageId = update.callbackQuery?.message?.messageId

                    when {
                        callbackData?.startsWith("prevPage_") == true -> handlePaginationCallback(chatId, callbackData, bot)
                        callbackData?.startsWith("nextPage_") == true -> { handlePaginationCallback(chatId, callbackData, bot); logger.info { "nextPage callbackQuery: ${callbackData.removePrefix("nextPage_")}" }}
                        callbackData == "close" -> handleDeleteMessageCallback(chatId, bot)
                        callbackData?.startsWith("entry_") == true -> handleEntryCallback(chatId, callbackData, bot)
                        callbackData?.startsWith("confirm_delete_") == true -> handleConfirmDeleteCallback(chatId, callbackData, bot)
                        callbackData == "cancel_delete" -> handleCancelDeleteCallback(chatId, callbackQueryId, bot)
                    }
                }

                contact {
                    val contact: Contact = update.message?.contact ?: return@contact
                    val chatId = update.message?.chat?.id ?: return@contact
                    val nickname = update.message?.from?.username ?: "Без имени"
                    val userSession = UserSessionContext.getSession(chatId)

                    userSession?.apply {
                        phoneNumber = contact.phoneNumber
                      //temporaryFormList.add("Запись на проектирование Кухни...")
                        handleHaveForms(ChatId.fromId(chatId), bot)
                    }
                }
            }
        }
        bot.startPolling()
        return bot
    }



    private fun handlePaginationCallback(chatId: Long, callbackData: String, bot: Bot) {
        val newPage = when {
            callbackData.startsWith("prevPage_") -> callbackData.removePrefix("prevPage_").toIntOrNull()
            callbackData.startsWith("nextPage_") -> callbackData.removePrefix("nextPage_").toIntOrNull()
            else -> null
        } ?: return

        logger.info { "handlePaginationCallback newPage = $newPage, currentUserForms: ${UserSessionContext.getSession(chatId)?.temporaryFormList.toString()}" }

        val currentUserSession = UserSessionContext.getSession(chatId)
        val newKeyboard = currentUserSession?.let { showUserEntries(it, newPage) }
        bot.editMessageReplyMarkup(
            chatId = ChatId.fromId(chatId),
            messageId = currentUserSession?.lastMessageId ?: return,
            replyMarkup = newKeyboard
        )
    }




    private fun handleDeleteMessageCallback(chatId: Long, bot: Bot) {
        val currentUserSession = UserSessionContext.getSession(chatId)
        bot.deleteMessage(
            chatId = ChatId.fromId(chatId),
            messageId = currentUserSession?.lastMessageId ?: return
        )
    }

    private fun handleHaveForms(chatId: ChatId, bot: Bot) {
        val replyHaveFormsButton = KeyboardButton(
            text = "Мои записи на проектирование"
        )

        val keyboardMarkup = KeyboardReplyMarkup(
            keyboard = listOf(listOf(replyHaveFormsButton)),
            resizeKeyboard = true,
            oneTimeKeyboard = true
        )

        bot.sendMessage(
            chatId,
            "Напишите свой вопрос :)\nСотрудник вам ответит как можно скорее!\n" +
                    "Если вы хотите отменить свою запись в ЭО, напишите /equeue или выберите пункт в меню 🔽",
            replyMarkup = keyboardMarkup
        )
    }


    private fun handleEntryCallback(chatId: Long, callbackData: String, bot: Bot) {
        val entry = callbackData.removePrefix("entry_")
        val confirmationKeyboard = InlineKeyboardMarkup.createSingleRowKeyboard(
            InlineKeyboardButton.CallbackData(text = "Да", callbackData = "confirm_delete_$entry"),
            InlineKeyboardButton.CallbackData(text = "Нет", callbackData = "cancel_delete")
        )

        val currentUserSession = UserSessionContext.getSession(chatId)

        val (response, exception) = bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = "Вы действительно хотите удалить запись \"$entry\"?",
            replyMarkup = confirmationKeyboard
        )

        var checkId: Long? = null

        response?.body()?.result?.let { message ->
            if (currentUserSession != null) {
                checkId = message.messageId
            }
            logger.info{"MessageId : $checkId -- Вы действительно хотите удалить запись \"$entry\"?"}
        } ?: run {
            exception?.printStackTrace()
        }

    }

    private fun handleConfirmDeleteCallback(chatId: Long, callbackData: String, bot: Bot) {
        val entry = callbackData.removePrefix("confirm_delete_")
        val currentUserSession = UserSessionContext.getSession(chatId)
        currentUserSession?.temporaryFormList?.remove(entry)

        val lastMessageId = currentUserSession?.lastMessageId ?: return
        logger.info { "fun handleConfirmDeleteCallback : lastMessageId = $lastMessageId" }

        bot.editMessageText(
            chatId = ChatId.fromId(chatId),
            messageId = lastMessageId,
            text = "Запись \"$entry\" была удалена.\nВаши текущие записи:"
        )

        val updatedKeyboard = showUserEntries(currentUserSession)
        bot.editMessageReplyMarkup(
            chatId = ChatId.fromId(chatId),
            messageId = lastMessageId,
            replyMarkup = updatedKeyboard
        )

        logger.info { "handleConfirmDeleteCallback Запись \"$entry\" была удалена.\n" +
                "Ваши текущие записи: edited message: ${(lastMessageId)}" }

        bot.deleteMessage(
            chatId = ChatId.fromId(chatId),
            messageId = lastMessageId - 1
        )
        bot.deleteMessage(
            chatId = ChatId.fromId(chatId),
            messageId = lastMessageId - 1
        )

//        logger.info { "handleConfirmDeleteCallback deleted message: ${(lastMessageId - 1)}" }
    }

    private fun handleCancelDeleteCallback(chatId: Long, callbackQueryId: String?, bot: Bot) {
        val currentUserSession = UserSessionContext.getSession(chatId)
        val lastMessageId = currentUserSession?.lastMessageId ?: return

        for (i in 0..2){
            bot.deleteMessage(
                chatId = ChatId.fromId(chatId),
                messageId = lastMessageId - i
            )
        }

        bot.answerCallbackQuery(
            callbackQueryId = callbackQueryId ?: return,
            text = "Удаление отменено.",
            showAlert = true
        )
        val updatedKeyboard = showUserEntries(currentUserSession)
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = "Ваши текущие записи:",
            replyMarkup = updatedKeyboard
        )
    }


/////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////




    private fun handleUserEntriesCommand(chatId: Long, bot: Bot) {
        val currentUserSession = UserSessionContext.getSession(chatId)
        if (currentUserSession != null) {
            val keyboard = showUserEntries(currentUserSession, bot = bot, chatId = chatId)

            val (response, exception) = bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Выберите интересующую вас запись:",
                replyMarkup = keyboard
            )

            response?.body()?.result?.let { message ->
                currentUserSession.lastMessageId = message.messageId
            } ?: run {
                exception?.printStackTrace()
            }

            logger.info { "1st message needed to be deleted: ${response?.body()?.result?.messageId} : ${response?.body()?.result?.text}" }

        } else {
            bot.sendMessage(ChatId.fromId(chatId), "Сессия не найдена.")
        }
    }





    private fun showUserEntries( session: UserSession, page: Int = 0, chatId: Long? = null, bot: Bot? = null): InlineKeyboardMarkup {
        val pageSize = 3
        val startIndex = page * pageSize
        var sessionList = session.temporaryFormList

        while (sessionList.size % pageSize != 0) {
            sessionList.add("")
        }
        logger.info { "sessionList: $sessionList, size = ${sessionList.size}" }

        if (sessionList.count { it -> it.isEmpty() } >= 3){
            sessionList = sessionList.subList(0, sessionList.size - 3)
            logger.info { "sessionList: $sessionList, size = ${sessionList.size}" }
        }


        val endIndex = (startIndex + pageSize).coerceAtMost(sessionList.size)
        val currentEntries = sessionList.subList(startIndex, endIndex)
        val entryButtons = currentEntries.map { entry ->
            if (entry.isNotBlank()) {
                listOf(InlineKeyboardButton.CallbackData(entry, "entry_$entry"))
            } else {
                listOf(InlineKeyboardButton.CallbackData(" ", "ignore"))
            }
        }.toMutableList()

        logger.info { "page: $page - current entries: $currentEntries\nendIndex: $endIndex\tsessionList.size: ${sessionList.size}" }

        val navigationButtons = mutableListOf<InlineKeyboardButton.CallbackData>().apply {
            add(InlineKeyboardButton.CallbackData("⬅️ Назад", if (page > 0) "prevPage_${page - 1}" else "ignore"))
            add(InlineKeyboardButton.CallbackData("❌ Закрыть", "close"))
            add(InlineKeyboardButton.CallbackData("Вперед ➡️", if (endIndex < sessionList.size) "nextPage_${page + 1}" else "ignore"))
        }
//        session.lastMessageId?.let {
//            chatId?.let { it1 -> ChatId.fromId(it1) }?.let { it2 ->
//                bot?.deleteMessage(
//                    chatId = it2,
//                    messageId = it
//                )
//            }
//            logger.info { "Deleted message: ${session.lastMessageId}" }
//        }
      return InlineKeyboardMarkup.create(entryButtons + listOf(navigationButtons))
    }
}
