package bot.commands

import base.BaseCommand
import bot.CommandContext
import base.SubCommand
import bot.buildDraftsEmbed
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.spec.InteractionReplyEditSpec
import data.google.docs.DraftsManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.jvm.optionals.getOrNull

class Drafts(
    private val draftsManager: DraftsManager
) : BaseCommand() {
    companion object {
        private const val SUB_COMMAND_SCAN = "scan"
        private const val SUB_COMMAND_LIST = "list"
    }

    override val name: String get() = "drafts"

    override val options = listOf(
        SubCommand(SUB_COMMAND_LIST, "List drafts"),
        SubCommand(SUB_COMMAND_SCAN, "Rescan documents in drafts folder to fetch latest data")
    )

    override val description: String = "Manage drafts"

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun execute(context: CommandContext): Unit = coroutineScope {

        val options = context.event.interaction.commandInteraction.getOrNull()?.options
        val subCommand = options?.find { it.type == ApplicationCommandOption.Type.SUB_COMMAND }

        when (subCommand?.name) {
            SUB_COMMAND_SCAN -> scanSubCommand(context)
            SUB_COMMAND_LIST -> listSubCommand(context)
        }
    }

    private suspend fun scanSubCommand(context: CommandContext): Unit = coroutineScope {
        context.deferReply()
        val drafts = draftsManager.scanDraftsFromDrive()

        // asynchronously write updated data to sheets, so we don't block before replying to command
        launch { draftsManager.updateDraftData(drafts) }

        val reply = InteractionReplyEditSpec
            .builder()
            .addEmbed(drafts.buildDraftsEmbed("Drafts"))
            .build()

        context.editReply(reply)
    }

    private suspend fun listSubCommand(context: CommandContext) {
        context.deferReply()

        val reply = InteractionReplyEditSpec
            .builder()
            .addEmbed(draftsManager.getDraftsFromSpreadsheet().buildDraftsEmbed("Drafts"))
            .build()

        context.editReply(reply)
    }
}