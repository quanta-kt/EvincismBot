import kotlin.reflect.KProperty

object GlobalConfig {

    /**
     * A utility class for delegating kotlin properties to environment variables with the same name as the property.
     * The environment variable is lazily read every time the property is accessed and not cached
     *
     * @param required whether the environment variable is required to be set. Throws [IllegalStateException] if this
     * is true and the environment variable is not set
     *
     * @param converter A callback function for applying any type conversion on the source value. This is called each
     * time the property is read
     */
    @Suppress("ClassName")
    private class envConfig<T>(private val required: Boolean = true, private val converter: (String) -> T) {

        operator fun getValue(globalConfig: GlobalConfig, property: KProperty<*>): T {
            val value = System.getenv(property.name)

            if (required && value == null) {
                error("Environment variable ${property.name} not set")
            }

            return converter(value)
        }
    }

    private fun envConfig(required: Boolean = true) = envConfig(required = required) { it }

    // Discord
    val BOT_TOKEN: String by envConfig()
    val GUILD_ID: Long by envConfig { it.toLongOrNull() ?: error("Invalid GUILD_ID") }
    val LOGGING_WEBHOOK_ID: Long? by envConfig(required = false, converter = String::toLongOrNull)

    // Google docs
    val DRAFTS_FOLDER_ID: String by envConfig()
    val DRAFTS_SHEET_ID: String by envConfig()
    val REWRITES_FOLDER_ID: String by envConfig()
    val REWRITES_SHEET_ID: String by envConfig()
}