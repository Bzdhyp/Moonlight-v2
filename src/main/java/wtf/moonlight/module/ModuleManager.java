package wtf.moonlight.module;

import org.jspecify.annotations.Nullable;
import wtf.moonlight.bind.BindManager;
import wtf.moonlight.module.impl.combat.KillAura;
import wtf.moonlight.module.impl.movement.Sprint;
import wtf.moonlight.module.impl.player.Scaffold;
import wtf.moonlight.module.impl.render.ClickGui;
import wtf.moonlight.module.impl.render.FullBright;
import wtf.moonlight.module.impl.render.PostProcess;

import java.util.List;

public final class ModuleManager {
    private final List<Module> modules;

    public ModuleManager(BindManager binds) {
        Scaffold scaffold = new Scaffold();
        modules = List.of(
                new KillAura(scaffold),
                new Sprint(),
                scaffold,
                new FullBright(),
                new PostProcess(),
                new ClickGui()
        );
        for (Module module : modules) {
            binds.register(module.getBind());
        }
    }

    public List<Module> getModules() {
        return modules;
    }

    public @Nullable Module getModule(String name) {
        String wanted = name.replace(" ", "");
        for (Module module : modules) {
            if (module.getName().replace(" ", "").equalsIgnoreCase(wanted)) return module;
        }
        return null;
    }

    public <T extends Module> T getModule(Class<T> type) {
        for (Module module : modules) {
            if (type.isInstance(module)) return type.cast(module);
        }
        throw new IllegalArgumentException("Unknown module " + type.getSimpleName());
    }
}
