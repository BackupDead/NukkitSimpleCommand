package com.rmpi.nukkit.simplecommand;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.permission.Permission;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandRegisterer {
    public static void register(Class<?> commandClass) throws CommandClassCorruptedException {
        Method[] methods = Arrays.stream(commandClass.getMethods())
                .filter(m -> m.getName().equals("onCommand"))
                .collect(Collectors.toList())
                .toArray(new Method[0]);

        for (Method method : methods) {
            Parameter[] params = method.getParameters();
            if (params.length == 0)
                throw CommandClassCorruptedException.factory(method.getName(), "First parameter is not a CommandSender.");
            if (!CommandSender.class.isAssignableFrom(params[0].getType()))
                throw CommandClassCorruptedException.factory(method.getName(), "First parameter is not a CommandSender.");
            else if (params[0].getAnnotation(ParameterDefine.class) != null)
                throw CommandClassCorruptedException.factory(method.getName(), "CommandSender can not have ParameterDefine annotation.");
            else if (params[0].getAnnotation(ExactPlayerSearch.class) != null)
                throw CommandClassCorruptedException.factory(method.getName(), "CommandSender can not have ExactPlayerSearch annotation.");
            else if (params[0].getAnnotation(Whitespaceable.class) != null)
                throw CommandClassCorruptedException.factory(method.getName(), "CommandSender can not have Whitespaceable annotation.");
            for (int i = 1; i < params.length; i++)
                if (params[i].getAnnotation(ParameterDefine.class) == null)
                    throw CommandClassCorruptedException.factory(method.getName(), "All command parameters must have Parameter definition.");
                else if (params[i].getType().isPrimitive()) {
                    if (params[i].getAnnotation(ExactPlayerSearch.class) != null)
                        throw CommandClassCorruptedException.factory(method.getName(), "Primitive can not have ExactPlayerSearch annotation.");
                    else if (params[i].getAnnotation(Whitespaceable.class) != null)
                        throw CommandClassCorruptedException.factory(method.getName(), "Primitive can not have Whitespaceable annotation.");
                } else if (params[i].getType() == String.class) {
                    if (params[i].getAnnotation(ExactPlayerSearch.class) != null)
                        throw CommandClassCorruptedException.factory(method.getName(), "String can not have ExactPlayerSearch annotation.");
                    else if (i != params.length - 1 && params[i].getAnnotation(Whitespaceable.class) != null)
                        throw CommandClassCorruptedException.factory(method.getName(), "String which is not the last parameter can not have Whitespaceable annotation.");
                } else if (params[i].getType() == Player.class) {
                    if (params[i].getAnnotation(Whitespaceable.class) != null)
                        throw CommandClassCorruptedException.factory(method.getName(), "Player can not have Whitespaceable annotation.");
                } else {
                    throw CommandClassCorruptedException.factory(method.getName(), "Parameter at " + (i + 1) + " is not supported type");
                }
        }

        String name;

        try {
            name = (String) commandClass.getField("name").get(null);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            name = commandClass.getSimpleName();
        }

        final String _name = name;

        String description;

        try {
            description = (String) commandClass.getField("description").get(null);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            description = "executes " + name;
        }

        String usage;

        try {
            usage = (String) commandClass.getField("usageMessage").get(null);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            usage = Arrays.stream(methods)
                    .map(Method::getParameters)
                    .map(pa -> {
                        StringBuilder tempBuilder = new StringBuilder();
                        if (pa[0].getType() != CommandSender.class) tempBuilder
                                .append("Only ")
                                .append(pa[0].getType().getSimpleName().toLowerCase())
                                .append(" can use this command. - ");
                        tempBuilder
                                .append("/")
                                .append(_name);
                        for (Parameter parameter : Arrays.copyOfRange(pa, 1, pa.length))
                                tempBuilder
                                        .append(" <")
                                        .append(parameter.getAnnotation(ParameterDefine.class).name())
                                        .append(">");
                        return tempBuilder.toString();
                    })
                    .collect(Collectors.joining("\n"));
        }

        String[] aliases;

        try {
            aliases = (String[]) commandClass.getField("aliases").get(null);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            aliases = new String[0];
        }

        String invalidUsage;

        try {
            invalidUsage = (String) commandClass.getField("invalidUsage").get(null);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            invalidUsage = "Invalid usage.\n%USAGE%";
        }

        final String _invalidUsage = invalidUsage;

        String permission;

        try {
            permission = (String) commandClass.getField("permission").get(null);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            permission = commandClass.getName() + "." + name;
        }

        String permissionDescription;

        try {
            permissionDescription = (String) commandClass.getField("permissionDescription").get(null);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            permissionDescription = null;
        }

        String permissionDefault;

        try {
            permissionDefault = (String) commandClass.getField("permissionDefault").get(null);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            permissionDefault = null;
        }

        String permissionMessage;

        try {
            permissionMessage = (String) commandClass.getField("permissionMessage").get(null);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            permissionMessage = null;
        }

        Command made = new Command(name, description, usage, aliases) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!testPermission(sender))
                    return true;

                methodLoop: for (Method method : methods) {
                    List<Object> arguments = new ArrayList<>();
                    Parameter[] parameters = method.getParameters();
                    parameters = Arrays.copyOfRange(parameters, 1, parameters.length);
                    if (!(parameters[parameters.length - 1].getAnnotation(Whitespaceable.class) != null
                            ? args.length >= parameters.length : args.length == parameters.length))
                        continue;
                    if (!parameters[0].getType().isInstance(sender))
                        continue;
                    arguments.add(sender);

                    for (int i = 0; i < parameters.length; i++)
                        if (parameters[i].getType() == Byte.TYPE) {
                            try {
                                arguments.add(Byte.parseByte(args[i]));
                            } catch (NumberFormatException e) {
                                continue methodLoop;
                            }
                        } else if (parameters[i].getType() == Short.TYPE) {
                            try {
                                arguments.add(Short.parseShort(args[i]));
                            } catch (NumberFormatException e) {
                                continue methodLoop;
                            }
                        } else if (parameters[i].getType() == Integer.TYPE) {
                            try {
                                arguments.add(Integer.parseInt(args[i]));
                            } catch (NumberFormatException e) {
                                continue methodLoop;
                            }
                        } else if (parameters[i].getType() == Long.TYPE) {
                            try {
                                arguments.add(Long.parseLong(args[i]));
                            } catch (NumberFormatException e) {
                                continue methodLoop;
                            }
                        } else if (parameters[i].getType() == Float.TYPE) {
                            try {
                                arguments.add(Float.parseFloat(args[i]));
                            } catch (NumberFormatException e) {
                                continue methodLoop;
                            }
                        } else if (parameters[i].getType() == Double.TYPE) {
                            try {
                                arguments.add(Double.parseDouble(args[i]));
                            } catch (NumberFormatException e) {
                                continue methodLoop;
                            }
                        } else if (parameters[i].getType() == Boolean.TYPE) {
                            switch (args[i]) {
                                case "true":
                                    arguments.add(true);
                                    break;
                                case "false":
                                    arguments.add(false);
                                    break;
                                default:
                                    continue methodLoop;
                            }
                        } else if (parameters[i].getType() == Character.TYPE) {
                            if (args[i].length() != 1)
                                continue methodLoop;
                            arguments.add(args[i].charAt(0));
                        } else if (parameters[i].getType() == String.class) {
                            if (parameters[i].getAnnotation(Whitespaceable.class) != null)
                                arguments.add(String.join(" ", Arrays.asList(Arrays.copyOfRange(args, i, args.length))));
                            else
                                arguments.add(args[i]);
                        } else if (parameters[i].getType() == Player.class) {
                            Player searched = parameters[i].getAnnotation(ExactPlayerSearch.class) != null
                                    ? Server.getInstance().getPlayerExact(args[i]) : Server.getInstance().getPlayer(args[i]);
                            if (searched == null)
                                continue methodLoop;
                            arguments.add(searched);
                        }

                    try {
                        method.invoke(null, arguments.toArray());
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        Server.getInstance().getLogger().logException(e);
                    }

                    return true;
                }

                sender.sendMessage(_invalidUsage.replace("%USAGE%", getUsage()));
                return true;
            }
        };
        new Permission(permission, permissionDescription, permissionDefault);
        made.setPermission(permission);
        made.setPermissionMessage(permissionMessage);
        Server.getInstance().getCommandMap().register(name, made);
    }
}
