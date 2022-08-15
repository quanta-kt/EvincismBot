package bot

import discord4j.core.spec.EmbedCreateSpec
import data.models.Draft

fun List<Draft>.buildDraftsEmbed(title: String): EmbedCreateSpec {
    val description = if (isNotEmpty()) {
        joinToString("\n\n") { draft ->
            val status = if (draft.isCompleted) {
                "Completed"
            } else {
                "Assigned"
            }

            "[**${draft.title}**](${draft.docUrl})\n" +
                    "**Author:** ${draft.author}\n" +
                    "**Status:** $status"
        }
    } else {
        "No drafts found"
    }

    return EmbedCreateSpec.builder()
        .title(title)
        .description(description)
        .build()
}