package com.example.redstonescanner;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class RedstoneScannerPlugin extends JavaPlugin implements CommandExecutor {

    private final Set<Material> REDSTONE_MATERIALS = EnumSet.of(
            Material.REDSTONE_WIRE, Material.REPEATER, Material.COMPARATOR,
            Material.PISTON, Material.STICKY_PISTON, Material.OBSERVER,
            Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH,
            Material.REDSTONE_BLOCK, Material.TARGET, Material.LEVER,
            Material.DAYLIGHT_DETECTOR, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR
    );

    @Override
    public void onEnable() {
        if (getCommand("scanredstone") != null) {
            getCommand("scanredstone").setExecutor(this);
        }
        getLogger().info("RedstoneScannerPlugin успешно запущен для Paper 1.21.1!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду может использовать только игрок.");
            return true;
        }

        if (!player.hasPermission("redstonescanner.use")) {
            player.sendMessage("§cУ вас нет прав (redstonescanner.use) на эту команду.");
            return true;
        }

        int radius = 3;
        if (args.length > 0) {
            try {
                radius = Integer.parseInt(args);
            } catch (NumberFormatException e) {
                player.sendMessage("§cРадиус должен быть числом.");
                return true;
            }
        }

        World world = player.getWorld();
        int centerChunkX = player.getLocation().getBlockX() >> 4;
        int centerChunkZ = player.getLocation().getBlockZ() >> 4;

        player.sendMessage("§6[Scanner] §7Запуск асинхронной проверки чанков в радиусе " + radius + "...");

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int targetX = centerChunkX + x;
                int targetZ = centerChunkZ + z;

                CompletableFuture<Chunk> chunkFuture = world.getChunkAtAsync(targetX, targetZ, true);

                chunkFuture.thenAccept(chunk -> {
                    getServer().getScheduler().runTaskAsynchronously(this, () -> {
                        int redstoneCount = 0;

                        for (BlockState tile : chunk.getTileEntities()) {
                            if (REDSTONE_MATERIALS.contains(tile.getType())) {
                                redstoneCount++;
                            }
                        }

                        if (redstoneCount > 5) { 
                            int blockX = chunk.getX() << 4;
                            int blockZ = chunk.getZ() << 4;
                            player.sendMessage(String.format(
                                    "§c[⚠️ Подозрительный чанк] §eX: %d, Z: %d §7| Найдено редстоуна: §4%d",
                                    blockX, blockZ, redstoneCount
                            ));
                        }
                    });
                }).exceptionally(ex -> {
                    getLogger().severe("Ошибка при прогрузке чанка: " + ex.getMessage());
                    return null;
                });
            }
        }
        return true;
    }
}
