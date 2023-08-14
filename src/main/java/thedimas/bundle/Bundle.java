package thedimas.bundle;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.Structs;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Mod;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

@SuppressWarnings("unused")
public class Bundle {

    private static final ObjectMap<Locale, StringMap> bundles = new ObjectMap<>();

    private static final ObjectMap<Locale, MessageFormat> formats = new ObjectMap<>();

    public static Locale[] supportedLocales;

    public static Locale defaultLocale() {
        return Structs.find(supportedLocales, l -> l.toString().equals("en"));
    }

    private Bundle() {
    }

    //region load
    /**
     * Loads the bundle for the specified mod and make class ready to be used.
     *
     * @param mod the mod for which to load the bundle
     */
    public static void load(Class<? extends Mod> mod) {
        Fi[] files = Vars.mods.getMod(mod).root.child("bundles").list();
        supportedLocales = new Locale[files.length + 1];
        supportedLocales[supportedLocales.length - 1] = new Locale("router"); // router

        for (int i = 0; i < files.length; i++) {
            String code = files[i].nameWithoutExtension();
            supportedLocales[i] = parseLocale(code.substring("bundle_".length()));
        }

        Log.debug("Loaded locales: @", Arrays.toString(supportedLocales));
    }

    /**
     * Returns the bundle for the specified locale, loading it if necessary.
     *
     * @param locale the locale for which to retrieve the bundle
     * @return the bundle for the specified locale
     */
    private static StringMap getOrLoad(Locale locale) {
        StringMap bundle = bundles.get(locale);
        if (bundle == null) {
            if (locale.getDisplayName().equals("router")) {
                StringMap router = new StringMap();
                getOrLoad(defaultLocale()).each((k, v) -> router.put(k, "router"));
                bundles.put(locale, bundle = router);
            } else if (Structs.contains(supportedLocales, locale)) {
                bundles.put(locale, bundle = load(locale));
            } else {
                bundle = getOrLoad(defaultLocale());
            }
        }
        return bundle;
    }

    /**
     * Loads the bundle for the specified locale from the resource bundle.
     *
     * @param locale the locale for which to load the bundle
     * @return the loaded bundle for the specified locale
     */
    private static StringMap load(Locale locale) {
        StringMap properties = new StringMap();
        ResourceBundle bundle = ResourceBundle.getBundle("bundles.bundle", locale);
        for (String s : bundle.keySet()) {
            properties.put(s, bundle.getString(s));
        }
        return properties;
    }
    //endregion

    //region locale utils
    /**
     * Parses the given locale code into a Locale object.
     *
     * @param code the locale code, in the format "language_country" or "language"
     * @return the parsed Locale object
     */
    public static Locale parseLocale(String code) {
        if (code.contains("_")) {
            String[] codes = code.split("_");
            return new Locale(codes[0], codes[1]);
        }
        return new Locale(code);
    }

    /**
     * Finds the Locale object that matches the given locale code.
     * If the exact match is not found, it returns the default locale.
     *
     * @param code the locale code, in the format "language_country" or "language"
     * @return the matching Locale object, or the default Locale if not found
     */
    public static Locale findLocale(String code) {
        Locale locale = Structs.find(supportedLocales, l -> l.toString().equals(code) ||
                code.startsWith(l.toString()));
        return locale != null ? locale : defaultLocale();
    }
    //endregion

    //region raw
    /**
     * Returns the localized string value associated with the specified key and locale, or a fallback string with the key surrounded by question marks if the key is not found in the bundle.
     *
     * @param key the key to lookup in the bundle
     * @param locale the locale to use for the lookup
     * @return the localized string value associated with the key and locale, or a fallback string if the key is not found
     */
    public static String get(String key, Locale locale) {
        StringMap bundle = getOrLoad(locale);
        return bundle.containsKey(key) ? bundle.get(key) : "???" + key + "???";
    }

    /**
     * Checks if the specified key is present in the bundle for the given locale.
     *
     * @param key the key to check in the bundle
     * @param locale the locale to use for the lookup
     * @return {@code true} if the key is present in the bundle for the given locale, {@code false} otherwise
     */
    public static boolean has(String key, Locale locale) {
        StringMap props = getOrLoad(locale);
        return props.containsKey(key);
    }

    /**
     * Formats the localized string value associated with the specified key and locale using the provided values.
     *
     * @param key the key to lookup in the bundle
     * @param locale the locale to use for the lookup
     * @param values the values to use for formatting the localized string
     * @return the formatted localized string value associated with the key and locale, or the key surrounded by question marks if the key is not found in the bundle
     */
    public static String format(String key, Locale locale, Object... values) {
        String pattern = get(key, locale);
        if (values.length == 0) {
            return pattern;
        }

        MessageFormat format = formats.get(locale);
        if (!Structs.contains(supportedLocales, locale)) {
            format = formats.get(defaultLocale(), () -> new MessageFormat(pattern, defaultLocale()));
            format.applyPattern(pattern);
        } else if (format == null) {
            format = new MessageFormat(pattern, locale);
            formats.put(locale, format);
        } else {
            format.applyPattern(pattern);
        }
        return format.format(values);
    }
    //endregion

    //region bundled Message
    /**
     * Sends a localized message to the specified player based on the given condition, using the provided keys and values for formatting.
     *
     * @param player the player to whom the message should be sent
     * @param condition the condition determining which key to use for the message
     * @param keyTrue the key to use if the condition is true
     * @param keyFalse the key to use if the condition is false
     * @param values the values to use for formatting the localized message
     */
    public static void bundled(Player player, boolean condition, String keyTrue, String keyFalse, Object... values) {
        String key = condition ? keyTrue : keyFalse;
        bundled(player, key, values);
    }

    /**
     * Sends a localized message to all players based on the given condition, using the provided keys and values for formatting.
     *
     * @param condition the condition determining which key to use for the message
     * @param keyTrue the key to use if the condition is true
     * @param keyFalse the key to use if the condition is false
     * @param values the values to use for formatting the localized message
     */
    public static void bundled(boolean condition, String keyTrue, String keyFalse, Object... values) {
        String key = condition ? keyTrue : keyFalse;
        bundled(key, values);
    }

    /**
     * Sends a localized message to the specified player using the provided key and values for formatting.
     *
     * @param player the player to whom the message should be sent
     * @param key the key to use for the message
     * @param values the values to use for formatting the localized message
     */
    public static void bundled(Player player, String key, Object... values) {
        player.sendMessage(format(key, findLocale(player.locale), values));
    }

    /**
     * Sends a localized message to all players using the provided key and values for formatting.
     *
     * @param key the key to use for the message
     * @param values the values to use for formatting the localized message
     */
    public static void bundled(String key, Object... values) {
        Groups.player.each(p -> bundled(p, key, values));
    }
    //endregion

    //region bundled HUD
    /**
     * Sets the HUD text of the specified player to a localized message based on the given condition, using the provided keys and values for formatting.
     *
     * @param player the player whose HUD text should be set
     * @param condition the condition determining which key to use for the message
     * @param keyTrue the key to use if the condition is true
     * @param keyFalse the key to use if the condition is false
     * @param values the values to use for formatting the localized message
     */
    public static void bundledHud(Player player, boolean condition, String keyTrue, String keyFalse, Object... values) {
        String key = condition ? keyTrue : keyFalse;
        bundledHud(player, key, values);
    }

    /**
     * Sets the HUD text of all players to a localized message based on the given condition, using the provided keys and values for formatting.
     *
     * @param condition the condition determining which key to use for the message
     * @param keyTrue the key to use if the condition is true
     * @param keyFalse the key to use if the condition is false
     * @param values the values to use for formatting the localized message
     */
    public static void bundledHud(boolean condition, String keyTrue, String keyFalse, Object... values) {
        String key = condition ? keyTrue : keyFalse;
        bundledHud(key, values);
    }

    /**
     * Sets the HUD text of the specified player to a localized message using the provided key and values for formatting.
     *
     * @param player the player whose HUD text should be set
     * @param key the key to use for the message
     * @param values the values to use for formatting the localized message
     */
    public static void bundledHud(Player player, String key, Object... values) {
        Call.setHudText(player.con(), format(key, findLocale(player.locale), values));
    }

    /**
     * Sets the HUD text of all players to a localized message based on the given condition, using the provided key and values for formatting.
     *
     * @param key the key to use for the message
     * @param values the values to use for formatting the localized message
     */
    public static void bundledHud(String key, Object... values) {
        Groups.player.each(p -> bundledHud(p, key, values));
    }
    //endregion

    //region bundled Info Message
    /**
     * Sends a localized info message (alert) to the specified player based on the given condition, using the provided keys and values for formatting.
     *
     * @param player the player to whom the message should be sent
     * @param condition the condition determining which key to use for the message
     * @param keyTrue the key to use if the condition is true
     * @param keyFalse the key to use if the condition is false
     * @param values the values to use for formatting the localized message
     */
    public static void bundledInfo(Player player, boolean condition, String keyTrue, String keyFalse, Object... values) {
        String key = condition ? keyTrue : keyFalse;
        bundledInfo(player, key, values);
    }

    /**
     * Sends a localized info message (alert) to the all players based on the given condition, using the provided keys and values for formatting.
     *
     * @param condition the condition determining which key to use for the message
     * @param keyTrue the key to use if the condition is true
     * @param keyFalse the key to use if the condition is false
     * @param values the values to use for formatting the localized message
     */
    public static void bundledInfo(boolean condition, String keyTrue, String keyFalse, Object... values) {
        String key = condition ? keyTrue : keyFalse;
        bundledInfo(key, values);
    }

    /**
     * Sends a localized info message (alert) to the specified player using the provided key and values for formatting.
     *
     * @param player the player to whom the message should be sent
     * @param key the key to use for the message
     * @param values the values to use for formatting the localized message
     */
    public static void bundledInfo(Player player, String key, Object... values) {
        Call.infoMessage(player.con(), format(key, findLocale(player.locale), values));
    }

    /**
     * Sends a localized info message (alert) to all players using the provided key and values for formatting.
     *
     * @param key the key to use for the message
     * @param values the values to use for formatting the localized message
     */
    public static void bundledInfo(String key, Object... values) {
        Groups.player.each(p -> bundledInfo(p, key, values));
    }
    //endregion

    //region bundled Label
    /**
     * Creates a label for the specified player with a localized message based on the given condition, using the provided keys and values for formatting.
     *
     * @param player the player to create the label to
     * @param condition the condition determining which key to use for the message
     * @param keyTrue the key to use if the condition is true
     * @param keyFalse the key to use if the condition is false
     * @param duration the duration (in seconds) that the label will be displayed
     * @param x the x-coordinate of the label
     * @param y the y-coordinate of the label
     * @param values the values to use for formatting the localized message
     */
    public static void bundledLabel(Player player, boolean condition, String keyTrue, String keyFalse, int duration, float x, float y, Object... values) {
        String key = condition ? keyTrue : keyFalse;
        bundledLabel(player, key, duration, x, y);
    }

    /**
     * Creates a label for all players with a localized message based on the given condition, using the provided keys and values for formatting.
     *
     * @param condition the condition determining which key to use for the message
     * @param keyTrue the key to use if the condition is true
     * @param keyFalse the key to use if the condition is false
     * @param duration the duration (in seconds) that the label will be displayed
     * @param x the x-coordinate of the label
     * @param y the y-coordinate of the label
     * @param values the values to use for formatting the localized message
     */
    public static void bundledLabel(boolean condition, String keyTrue, String keyFalse, int duration, float x, float y, Object... values) {
        String key = condition ? keyTrue : keyFalse;
        bundledLabel(key, duration, x, y, values);
    }

    /**
     * Creates a label for the specified player with a localized message using the provided key and values for formatting.
     *
     * @param player the player to create the label to
     * @param key the key to use for the message
     * @param duration the duration (in seconds) that the label will be displayed
     * @param x the x-coordinate of the label
     * @param y the y-coordinate of the label
     * @param values the values to use for formatting the localized message
     */
    public static void bundledLabel(Player player, String key, int duration, float x, float y, Object... values) {
        Call.label(player.con(), format(key, findLocale(player.locale), values), duration, x, y);
    }

    /**
     * Creates a label for all players with a localized message using the provided key and values for formatting.
     *
     * @param key the key to use for the message
     * @param duration the duration (in seconds) that the label will be displayed
     * @param x the x-coordinate of the label
     * @param y the y-coordinate of the label
     * @param values the values to use for formatting the localized message
     */
    public static void bundledLabel(String key, int duration, float x, float y, Object... values) {
        Groups.player.each(p -> bundledLabel(p, key, duration, x, y, values));
    }
    //endregion

    /* TODO
     * Call.announce
     * Call.infoToast
     * Call.warningToast - Call.warningToast(Iconc.codes.get("redo"), "Redo")
     * Call.infoPopup
     * Call.menu
     * Call.followUpMenu
     * Call.textInput
     */
}
