package dev.xiaoyu.useonce.rules;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;

public final class ServerTasks {
    private ServerTasks() {
    }

    // 将任务推迟到下个服务器 tick 执行
    public static void defer(ServerLevel level, Runnable task) {
        MinecraftServer server = level.getServer();
        server.tell(new TickTask(server.getTickCount(), task));
    }
}
