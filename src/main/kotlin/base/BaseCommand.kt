package base

import bot.CommandContext
import bot.commands.Drafts
import bot.commands.Rewrites
import discord4j.discordjson.json.ApplicationCommandRequest
import data.google.docs.DraftsManager

abstract class BaseCommand {
    abstract val name: String
    abstract val description: String
    open val options: List<BaseCommandOption> = emptyList()

    abstract suspend fun execute(context: CommandContext)

    val commandRequest: ApplicationCommandRequest
        get() {
            return ApplicationCommandRequest
                .builder().apply {
                    name(name)
                    description(description)
                    options.forEach { addOption(it.commandData) }
                }.build()
        }
}

/**
 * Builds a map of all commands with their names as keys
 */
fun buildCommandMap() : Map<String, BaseCommand> {

    // TODO: Introduce dependency injection?
    val draftsManager = DraftsManager()

    val commands = listOf(
        Drafts(draftsManager),
        Rewrites(draftsManager),
    )

    return commands.associateBy { it.name }
}