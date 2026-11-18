package fun.applecube.core.command;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Stops a command and shows a language entry to the sender. Thrown by commands and by
 * {@link CommandContext} helpers; never logged.
 */
public final class CommandException extends Exception {

    private final String key;
    private final TagResolver[] placeholders;
    private final String permission;

    public CommandException(String key, TagResolver... placeholders) {
        this(key, null, placeholders);
    }

    private CommandException(String key, String permission, TagResolver... placeholders) {
        super(key, null, false, false);
        this.key = key;
        this.permission = permission;
        this.placeholders = placeholders;
    }

    /** Missing permission; rendered through the permission service (with the donation-rank hint). */
    public static CommandException noPermission(String permission) {
        return new CommandException("errors.no-permission", permission);
    }

    public String key() {
        return key;
    }

    public TagResolver[] placeholders() {
        return placeholders;
    }

    /** Non-null when this is a permission failure. */
    public String permission() {
        return permission;
    }
}
