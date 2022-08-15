package bot

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec
import discord4j.core.spec.InteractionReplyEditSpec
import kotlinx.coroutines.reactor.awaitSingleOrNull

/**
 * A light coroutine based wrapper around [ApplicationCommandInteractionEvent]
 */
class CommandContext(val event: ApplicationCommandInteractionEvent) {

    suspend fun reply(contents: String) {
        event.reply(contents).awaitSingleOrNull()
    }

    suspend fun reply(spec: InteractionApplicationCommandCallbackSpec) {
        event.reply(spec).awaitSingleOrNull()
    }

    suspend fun deferReply() = event.deferReply().awaitSingleOrNull()

    suspend fun editReply(contents: String) = event.editReply(contents).awaitSingleOrNull()
    suspend fun editReply(spec: InteractionReplyEditSpec) = event.editReply(spec).awaitSingleOrNull()

    suspend fun createFollowup(contents: String) = event.createFollowup(contents).awaitSingleOrNull()
}