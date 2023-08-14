package thedimas.bundle;

import arc.util.CommandHandler;
import arc.util.Structs;
import mindustry.gen.Player;
import mindustry.mod.Plugin;

public class ExamplePlugin extends Plugin {
    @Override
    public void init() {
        Bundle.load(ExamplePlugin.class);
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("logs", "Test logs", (args, player) -> {
            Structs.each(level -> {
                Bundle.bundled(player, level, "logs.msg", level.name());
            }, Level.values());
        });
    }
}
