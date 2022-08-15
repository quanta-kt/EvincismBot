package base

import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.discordjson.json.ApplicationCommandOptionData

abstract class BaseCommandOption {
    abstract val name: String

    abstract val description: String

    abstract val type: ApplicationCommandOption.Type

    val commandData : ApplicationCommandOptionData get() {
        return ApplicationCommandOptionData.builder()
            .type(type.value)
            .name(name)
            .description(description)
            .build()
    }
}