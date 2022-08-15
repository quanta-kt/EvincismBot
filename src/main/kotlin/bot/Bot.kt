package bot

import GlobalConfig
import discord4j.core.DiscordClient
import discord4j.gateway.intent.IntentSet
import reactor.core.publisher.Mono

fun startBot() {
    val restClient = DiscordClient.create(GlobalConfig.BOT_TOKEN)

    val gatewayClient = restClient.gateway()
        .setEnabledIntents(IntentSet.none())
        .login()
        .block() ?: error("Unable to login to Discord")

    try {
        val applicationId = restClient.applicationId.block() ?: error("Unable to get application ID")
        val commandHandler = CommandHandler(gatewayClient)

        // Delete all old commands before startup
        restClient.applicationService
            .getGuildApplicationCommands(applicationId, GlobalConfig.GUILD_ID)
            .flatMap { commandData ->
                val commandId = commandData.id().toLongOrNull()

                if (commandId != null) {
                    restClient.applicationService.deleteGuildApplicationCommand(
                        applicationId,
                        GlobalConfig.GUILD_ID,
                        commandId
                    )
                } else {
                    Mono.empty()
                }
            }.blockLast()

        // Create commands
        commandHandler.commands.values.forEach {
            restClient.applicationService.createGuildApplicationCommand(
                applicationId,
                GlobalConfig.GUILD_ID,
                it.commandRequest
            ).block()
        }

        gatewayClient.on(commandHandler).blockLast()
    } finally {
        gatewayClient.logout().block()
    }
}
