package base

import discord4j.core.`object`.command.ApplicationCommandOption

data class SubCommand(
    override val name: String,
    override val description: String
) : BaseCommandOption() {

    override val type = ApplicationCommandOption.Type.SUB_COMMAND
}