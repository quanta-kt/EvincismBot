package bot

import base.buildCommandMap
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.ReactiveEventAdapter
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent
import discord4j.core.spec.WebhookExecuteSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.io.StringWriter

class CommandHandler(
    private val gatewayDiscordClient: GatewayDiscordClient
) : ReactiveEventAdapter() {

    companion object {
        const val DISCORD_MESSAGE_CHARACTER_LIMIT = 2000
    }

    val commands = buildCommandMap()

    override fun onApplicationCommandInteraction(event: ApplicationCommandInteractionEvent) = mono {
        val commandContext = CommandContext(event)

        try {
            commands[event.commandName]?.execute(commandContext)
        } catch (error: Exception) {
            handleCommandError(commandContext, error)
        }
    }

    /**
     * Logs error to standard error and Discord webhook if available
     */
    private suspend fun handleCommandError(context: CommandContext, error: Exception): Unit = coroutineScope {
        error.printStackTrace()

        val webhookId = GlobalConfig.LOGGING_WEBHOOK_ID ?: return@coroutineScope
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter, true)

        val stackTrace = withContext(Dispatchers.IO) {
            printWriter.println("```")
            error.printStackTrace(printWriter)
            printWriter.println("```")
            stringWriter.buffer.toString()
        }

        val webhook = gatewayDiscordClient.getWebhookById(Snowflake.of(webhookId)).awaitSingle()

        // Send as a file if content has more than allowed characters
        val spec = if (stackTrace.length < DISCORD_MESSAGE_CHARACTER_LIMIT) {
            WebhookExecuteSpec.builder().content(stackTrace).build()
        } else {
            WebhookExecuteSpec.builder().addFile("error-log.txt", stackTrace.byteInputStream()).build()
        }

        webhook.execute(spec).awaitSingleOrNull()
    }
}