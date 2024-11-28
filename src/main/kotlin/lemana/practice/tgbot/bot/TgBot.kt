
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
import lemana.practice.tgbot.dto.converter.UserFormsConverter
import lemana.practice.tgbot.model.UserFormsModel
import lemana.practice.tgbot.repo.form.UserFormsRepository
import lemana.practice.tgbot.repo.message.InlineMessageRepository
import lemana.practice.tgbot.repo.message.UserRepository
import lemana.practice.tgbot.service.create.ContactMessageCreateService
import lemana.practice.tgbot.service.create.InlineMessageCreateService
import lemana.practice.tgbot.service.create.TextMessageCreateService
import lemana.practice.tgbot.service.create.UserCreateService
import lemana.practice.tgbot.session.UserSession
import lemana.practice.tgbot.session.context.UserSessionContext


import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.util.*

@Component
class TgBot(
    @Value("\${bot.token}") private val token: String,
    @Value("\${bot.name}") private val botName: String,
    private val userCreateService: UserCreateService,
    private val inlineMessageCreateService: InlineMessageCreateService,
    private val userRepository: UserRepository,
    private val textMessageCreateService: TextMessageCreateService,
    private val contactMessageCreateService: ContactMessageCreateService,
    private val userFormsRepository: UserFormsRepository,
    private val inlineMessageRepository: InlineMessageRepository
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

                    val string = "Пожалуйста, нажмите кнопку \"Поделиться номером\" для продолжения диалога. " +
                            "Продолжая диалог, Вы предоставляете компании ООО «ЛЕ МОНЛИД» свое согласие на коммуникацию," +
                            " сбор, обработку и хранение персональных данных в соответствии с условиями, размещенными " +
                            "на нашем официальном сайте - https://lemanapro.ru/legal/soglasie-na-obrabotku-personalnyh-dannyh/"

                    val (response, exception) = bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = string,
                        replyMarkup = keyboardMarkup
                    )

                    response?.body()?.result?.let { message ->
                        textMessageCreateService.addTextMessage(chatId, message.messageId, bot, string.length)
                        logger.info { "textMessageCreateService(${message.text}})" }
                    }
                }

                command("equeue") {
                    val chatId = update.message?.chat?.id ?: return@command
                    val messageId = update.message!!.messageId
                    bot.deleteMessage(ChatId.fromId(chatId), messageId)
                    handleUserEntriesCommand(chatId, bot)
                }


                text {
                    val chatId = update.message?.chat?.id ?: return@text
                    val messageText = update.message?.text

                    if (messageText != null) {
                        textMessageCreateService.addTextMessage(chatId, message.messageId, bot, messageText.length)

                    logger.info { "textMessageCreateService.addTextMessage($messageText)" }}

                    if (messageText == "Мои записи на проектирование")
                    {
                        val messageId = update.message!!.messageId
                        handleUserEntriesCommand(chatId, bot)
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
                    try {
                        userCreateService.createUser(nickname, chatId, contact.phoneNumber)
                        logger.info { "userCreateService.createUser($nickname, $chatId, ${contact.phoneNumber})" }
                        handleHaveForms(chatId, bot)
                        logger.info { "contactMessageCreateService.addContactMessage($chatId, ${update.message!!.messageId}, ${contact.phoneNumber}, bot)"}
                        contactMessageCreateService.addContactMessage(chatId, update.message!!.messageId, contact.phoneNumber, bot)
                    }
                    catch(e: IllegalArgumentException)
                    {
                        logger.warn{e.message}
                        handleHaveForms(chatId, bot)
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

        val lastMessageId = inlineMessageRepository.findByUserChatId(chatId).maxByOrNull { it.id }!!.messageId
        val newKeyboard = showUserEntries(newPage, chatId, bot)
        bot.editMessageReplyMarkup(
            chatId = ChatId.fromId(chatId),
            messageId = lastMessageId,
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

    private fun handleHaveForms(chatId: Long, bot: Bot) {
        val replyHaveFormsButton = KeyboardButton(text = "Мои записи на проектирование")
        val keyboardMarkup = KeyboardReplyMarkup(
            keyboard = listOf(listOf(replyHaveFormsButton)),
            resizeKeyboard = true,
            oneTimeKeyboard = true
        )
        val string = "Напишите свой вопрос :)\nСотрудник вам ответит как можно скорее!\n" +
                "Если вы хотите отменить свою запись в ЭО, напишите /equeue или выберите пункт в меню 🔽"

        val (response, exception) = bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = string,
            replyMarkup = keyboardMarkup
        )
        response?.body()?.result?.let { message ->
            textMessageCreateService.addTextMessage(chatId, message.messageId, bot, string.length)
            logger.info { "textMessageCreateService(${message.messageId}, ${message.text}})" }
        }?: run {
            exception?.printStackTrace()
        }
    }

    private fun handleEntryCallback(chatId: Long, callbackData: String, bot: Bot) {
        logger.info{"handleEntryCallback"}
        val session = UserSessionContext.getSession(chatId)
        val entry = callbackData.removePrefix("entry_")
        val confirmationKeyboard = InlineKeyboardMarkup.createSingleRowKeyboard(
            InlineKeyboardButton.CallbackData(text = "Да", callbackData = "confirm_delete_$entry"),
            InlineKeyboardButton.CallbackData(text = "Нет", callbackData = "cancel_delete")
        )

        val (response, exception) = bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = "Вы действительно хотите удалить запись ?",
            replyMarkup = confirmationKeyboard
        )

        response?.body()?.result?.let { message ->
            // inlineMessageCreateService.addInlineMessage(chatId, message.messageId, bot)
            if (session != null) {
                session.setLastInlineChoice(chatId, bot, message.messageId)
            }
            else{
                logger.info { "KILL" }
            }
            logger.info{"MessageId : ${message.messageId}, ${message.text},  - с запозданием"}}
        ?: run {
            exception?.printStackTrace() }
        }

    private fun handleConfirmDeleteCallback(chatId: Long, callbackData: String, bot: Bot) {
        logger.info { "handleConfirmDeleteCallback" }
        val entry = callbackData.removePrefix("confirm_delete_")

        val session = UserSessionContext.getSession(chatId)
        val form = userFormsRepository.findById(entry.toLong())
            .orElseThrow { NoSuchElementException("Форма с id $entry не найдена") }
        val data = form.data
        userFormsRepository.deleteById(entry.toLong())
        val user = userRepository.findByChatId(chatId)
        val forms = userFormsRepository.findAllByUserId(user!!.id)
        // val forms = userFormsRepository.findAllByUserId(chatId)
        val lastMessageId = inlineMessageRepository.findByUserChatId(chatId).maxByOrNull { it.id }!!.messageId
        val timedLastMessageId = session?.lastInlineChoice
        if (forms.isEmpty()){
            bot.deleteMessage(ChatId.fromId(chatId), lastMessageId)
            if (timedLastMessageId != null) {
                bot.deleteMessage(
                    chatId = ChatId.fromId(chatId),
                    messageId = timedLastMessageId
                )
            }
            handleHaveNoForms(chatId, bot)
            handleHaveForms(chatId, bot)
        }
        else {
            bot.editMessageText(
                chatId = ChatId.fromId(chatId),
                messageId = lastMessageId,
                text = "Запись \"${data}\" была удалена.\nВаши текущие записи:"
            )
            //logger.info { "lastId: $lastMessageId" }
            val updatedKeyboard = showUserEntries(chatId = chatId, bot = bot)

            bot.editMessageReplyMarkup(
                chatId = ChatId.fromId(chatId),
                messageId = lastMessageId,
                replyMarkup = updatedKeyboard
            )

            if (timedLastMessageId != null) {
                bot.deleteMessage(
                    chatId = ChatId.fromId(chatId),
                    messageId = timedLastMessageId
                )
            }
        }
    }

    private fun handleCancelDeleteCallback(chatId: Long, callbackQueryId: String?, bot: Bot) {
        val lastMessageId = inlineMessageRepository.findByUserChatId(chatId).maxByOrNull { it.id }!!.messageId
        val session = UserSessionContext.getSession(chatId)
        logger.info { "lastMessageId: $lastMessageId" }

        val updatedKeyboard = showUserEntries(chatId=chatId, bot=bot)
        val (response, exception) = bot.editMessageReplyMarkup(
            chatId = ChatId.fromId(chatId),
            messageId = lastMessageId,
            replyMarkup = updatedKeyboard
        )

        bot.answerCallbackQuery(
            callbackQueryId = callbackQueryId ?: return,
            text = "Удаление отменено.",
            showAlert = true
        )

        response?.body()?.result?.let { message ->
            inlineMessageCreateService.addInlineMessage(chatId, message.messageId, bot)
        }?: run {
            exception?.printStackTrace()
        }

        val timedLastMessageId = session?.lastInlineChoice
        if (timedLastMessageId != null) {
            bot.deleteMessage(
                chatId = ChatId.fromId(chatId),
                messageId = timedLastMessageId
            )
        }
        logger.info{"timedLastMessageId: $timedLastMessageId"}

    }


/////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////


    private fun handleUserEntriesCommand(chatId: Long, bot: Bot) {

        val user = userRepository.findByChatId(chatId)
        val checkForms = userFormsRepository.findAllByUserId(user!!.id)
        if (checkForms.isEmpty()){
//            val string: String = """
//                        У вас пока нет активных записей.
//                        Вы можете создать запись, позвонив по номеру телефона: +7 (123) 456-78-90,
//                        либо на нашем сайте: https://lemanapro.create_forms.ru
//                    """.trimIndent()
//
//            val (responseResponse, exceptionException) = bot.sendMessage(
//                chatId = ChatId.fromId(chatId),
//                text = string)
//
//            responseResponse?.body()?.result?.let { message ->
//                textMessageCreateService.addTextMessage(chatId, message.messageId, bot, string.length)
//                logger.info { "textMessageCreateService(${message.messageId}, ${message.text}})" }
//            }?: run {
//                exceptionException?.printStackTrace()
//            }
            handleHaveNoForms(chatId, bot)
        }

        else{
            val keyboard = showUserEntries(bot = bot, chatId = chatId)
            logger.info { "private fun handleUserEntriesCommand" }

            val (response, exception) = bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Выберите интересующую вас запись:",
                replyMarkup = keyboard
            )
                response?.body()?.result?.let { message ->
                inlineMessageCreateService.addInlineMessage(chatId, message.messageId, bot)
                logger.info { "inlineMessageCreateService: (${message.messageId}, ${message.text})" }
            }?: run {
                    exception?.printStackTrace()
                }
    }}

    private fun showUserEntries(page: Int = 0, chatId: Long, bot: Bot): ReplyMarkup {
        logger.info("showUserEntries started")
        val pageSize = 3
        val startIndex = page * pageSize
        val user = userRepository.findByChatId(chatId)
        val userForms = userFormsRepository.findAllByUserId(user!!.id)
        val dtoForms = UserFormsConverter.toDTOList(userForms)
        logger.info("dtoForms: $dtoForms")

        val formMap: MutableMap<Long, String> = try { dtoForms
                .associate { it.id to it.data }
                .toMutableMap()

        } catch (ex: Exception) {
            logger.error("Error fetching user forms for chatId $chatId", ex)
            mutableMapOf()
        }

        val missingEntries = (3 - formMap.size % 3) % 3 // Вычисляем количество недостающих записей
        val currentMaxKey = formMap.keys.maxOrNull() ?: 0L // Находим текущий максимальный ключ
        //logger.info { "Missing entries to align size: $missingEntries" }
        //logger.info { "First formMap size: ${formMap.size}, content: $formMap" }
        repeat(missingEntries) {
            formMap[currentMaxKey + it + 1] = " " // Добавляем недостающие пустые записи
        }
        //logger.info { "Final formMap size: ${formMap.size}, content: $formMap" }
        val endIndex = (startIndex + pageSize).coerceAtMost(formMap.size)
        val currentEntries = formMap.entries
            .sortedBy { it.key }
            .drop(startIndex)
            .take(pageSize)

        if (currentEntries.isEmpty()){
            handleHaveForms(chatId, bot)
        }

        val entryButtons = currentEntries.map { entry ->
            val buttonText = if (entry.value.isNotBlank()) entry.value else " "
            val callbackData = "entry_${entry.key}"
            listOf(InlineKeyboardButton.CallbackData(buttonText, callbackData))
        }.toMutableList()

        //logger.info { "page: $page - current entries: $currentEntries\nendIndex: $endIndex\tsessionList.size: ${formMap.size}" }

        val navigationButtons = mutableListOf<InlineKeyboardButton.CallbackData>().apply {
            add(InlineKeyboardButton.CallbackData("⬅️ Назад", if (page > 0) "prevPage_${page - 1}" else "ignore"))
            add(InlineKeyboardButton.CallbackData("❌ Закрыть", "close"))
            add(InlineKeyboardButton.CallbackData("Вперед ➡️", if (endIndex < formMap.size) "nextPage_${page + 1}" else "ignore"))
        }

        return InlineKeyboardMarkup.create(entryButtons + listOf(navigationButtons))

//        // var sessionList: MutableList<String> = userFormsRepository.findAllByUserChatId(chatId).map{it.data}.toMutableList()
//        logger.info{"sessionList: $sessionList"}
//        while (sessionList.size % pageSize != 0) {
//            sessionList.add("")
//        }
//        logger.info { "sessionList: $sessionList, size = ${sessionList.size}" }
//
//        if (sessionList.count { it -> it.isEmpty() } >= 3){
//            sessionList = sessionList.subList(0, sessionList.size - 3)
//            //logger.info { "sessionList: $sessionList, size = ${sessionList.size}" }
//        }
//
//
//        val endIndex = (startIndex + pageSize).coerceAtMost(sessionList.size)
//        val currentEntries = sessionList.subList(startIndex, endIndex)
//        val entryButtons = currentEntries.map { entry ->
//            if (entry.isNotBlank()) {
//                listOf(InlineKeyboardButton.CallbackData(entry, "entry_$entry"))
//            } else {
//                listOf(InlineKeyboardButton.CallbackData(" ", "ignore"))
//            }
//        }.toMutableList()
//
//        logger.info { "page: $page - current entries: $currentEntries\nendIndex: $endIndex\tsessionList.size: ${sessionList.size}" }
//
//        val navigationButtons = mutableListOf<InlineKeyboardButton.CallbackData>().apply {
//            add(InlineKeyboardButton.CallbackData("⬅️ Назад", if (page > 0) "prevPage_${page - 1}" else "ignore"))
//            add(InlineKeyboardButton.CallbackData("❌ Закрыть", "close"))
//            add(InlineKeyboardButton.CallbackData("Вперед ➡️", if (endIndex < sessionList.size) "nextPage_${page + 1}" else "ignore"))
//        }
////        session.lastMessageId?.let {
////            chatId?.let { it1 -> ChatId.fromId(it1) }?.let { it2 ->
////                bot?.deleteMessage(
////                    chatId = it2,
////                    messageId = it
////                )
////            }
////            logger.info { "Deleted message: ${session.lastMessageId}" }
////        }
//      return InlineKeyboardMarkup.create(entryButtons + listOf(navigationButtons))
    }

    private fun handleHaveNoForms(chatId: Long, bot: Bot){
        val string: String = """
                        У вас пока нет активных записей. 
                        Вы можете создать запись, позвонив по номеру телефона: +7 (123) 456-78-90, 
                        либо на нашем сайте: https://lemanapro.create_forms.ru
                    """.trimIndent()

        val (responseResponse, exceptionException) = bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = string)

        responseResponse?.body()?.result?.let { message ->
            textMessageCreateService.addTextMessage(chatId, message.messageId, bot, string.length)
            logger.info { "textMessageCreateService(${message.messageId}, ${message.text}})" }
        }?: run {
            exceptionException?.printStackTrace()
        }
    }


}

