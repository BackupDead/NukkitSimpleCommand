package com.rmpi.nukkit.simplecommand.test;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import com.rmpi.nukkit.simplecommand.ExactPlayerSearch;
import com.rmpi.nukkit.simplecommand.ParameterDefine;

public class killYeah {
    public static void onCommand(CommandSender sender, @ParameterDefine(name = "toKill") @ExactPlayerSearch Player toKill) {
        toKill.kill();
    }
}

// com.rmpi.nukkit.simplecommand.CommandRegisterer.register(killYeah.class);