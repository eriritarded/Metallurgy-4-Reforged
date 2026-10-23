package dev.miles.metallurgy.qa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Separate disposable QA mod: native fresh-world flow, actual connection and rendered frames. */
@Mod("metallurgy_worldload_qa")
public final class WorldLoadSmoke {
    private static final Path REPORT = Path.of(System.getProperty("metallurgy.qa.report"));
    private static final AtomicLong SERVER_TICKS = new AtomicLong();
    private long frames, ticks, worldTicks, firstFrame, firstServerTick, entered;
    private int phase;
    private String lastScreen = "";

    public WorldLoadSmoke() {
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::frame);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
        record("harness_loaded", "production JAR remains separate");
    }
    private void serverTick(ServerTickEvent.Post event) { SERVER_TICKS.incrementAndGet(); }
    private void frame(RenderFrameEvent.Post event) { frames++; }
    private void record(String state, String detail) {
        try {
            String json = "{\"time\":\""+Instant.now()+"\",\"state\":\""+state+"\",\"detail\":\""+
                detail.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")+"\",\"ticks\":"+ticks+
                ",\"worldTicks\":"+worldTicks+",\"frames\":"+frames+",\"serverTicks\":"+SERVER_TICKS.get()+"}\n";
            Files.writeString(REPORT, json, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            System.out.println("METALLURGY_QA "+json.strip());
        } catch (Exception failure) { throw new RuntimeException(failure); }
    }
    private void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance(); ticks++;
        String screen = mc.screen == null ? "none" : mc.screen.getClass().getName();
        if (!screen.equals(lastScreen)) { lastScreen = screen; record("screen", screen+(mc.screen == null ? "" : " title="+mc.screen.getTitle().getString())); }
        if (ticks % 100 == 0) record("heartbeat", "phase="+phase+" screen="+screen);
        if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null && ticks > 50) {
            phase = 1; record("opening_creation", "native CreateWorldScreen.openFresh");
            CreateWorldScreen.openFresh(mc, () -> mc.setScreen(new TitleScreen()));
        } else if (phase == 1 && mc.screen instanceof CreateWorldScreen creation && mc.getOverlay() == null) {
            creation.getUiState().setName("Metallurgy world-load QA");
            creation.getUiState().setSeed("701726122");
            creation.getUiState().setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE);
            String label = Component.translatable("selectWorld.create").getString();
            for (var child : creation.children()) if (child instanceof AbstractButton button &&
                button.active && button.getMessage().getString().equals(label)) {
                phase = 2; record("creating_world", "normal preset / creative / seed 701726122 / native create action");
                button.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
                break;
            }
        }
        if (phase == 2 && mc.screen instanceof ConfirmScreen confirmation &&
            confirmation.getTitle().getString().equals(Component.translatable("selectWorld.warning.experimental.title").getString())) {
            String yes = Component.translatable("gui.yes").getString();
            for (var child : confirmation.children()) if (child instanceof AbstractButton button &&
                button.active && button.getMessage().getString().equals(yes)) {
                record("experimental_confirmation", "accepted native experimental-world generation warning for disposable modded world");
                button.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
                break;
            }
        }
        if (phase == 2 && mc.level != null && mc.player != null && mc.getConnection() != null && mc.getSingleplayerServer() != null && mc.screen == null) {
            phase = 3; entered = System.nanoTime(); firstFrame = frames; firstServerTick = SERVER_TICKS.get();
            record("world_entered", "actual client connected to integrated server; "+mc.level.dimension().identifier());
        }
        if (phase == 3) {
            if (mc.level == null || mc.player == null || mc.getConnection() == null) {
                record("failed", "disconnected after entering world"); mc.stop(); phase = 9; return;
            }
            worldTicks++;
            if (worldTicks % 20 == 0) {
                // Exercise the production particle factory and ten animated atlas frames, without changing it.
                var pos = mc.player.blockPosition().above();
                for (String id : new String[]{"adamantine", "atlarus", "eximite"}) {
                    var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("metallurgy", id+"_ore"));
                    if (block == net.minecraft.world.level.block.Blocks.AIR) throw new IllegalStateException("Missing ore "+id);
                    block.animateTick(block.defaultBlockState(), mc.level, pos, net.minecraft.util.RandomSource.create(worldTicks));
                }
            }
            long elapsed = (System.nanoTime()-entered)/1_000_000_000L;
            if (elapsed >= 90 && worldTicks >= 1600 && frames-firstFrame >= 600 && SERVER_TICKS.get()-firstServerTick >= 1600 && !mc.isPaused() && mc.screen == null && mc.player.isAlive()) {
                phase = 4;
                record("passed", "90+ seconds connected and unpaused; client/server ticks and rendered frames advancing; ore particle production path exercised");
                Screenshot.grab(mc.gameDirectory, "world-loaded.png", mc.getMainRenderTarget(), 1, message -> record("screenshot", message.getString()));
                mc.disconnectWithSavingScreen();
                record("world_closed", "native save-and-disconnect completed");
                mc.stop();
            }
        }
    }
}
