package com.rmpi.nukkit.simplecommand.test;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;

import com.rmpi.nukkit.simplecommand.ExactPlayerSearch;

public class kill {
    public static void onCommand(CommandSender sender, @ExactPlayerSearch Player toKill) {
        toKill.kill();
    }
}

// CommandRegisterer.register(kill.class);