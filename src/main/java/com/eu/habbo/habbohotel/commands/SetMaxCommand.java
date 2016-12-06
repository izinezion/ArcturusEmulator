package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserWhisperComposer;

public class SetMaxCommand extends Command
{
    public SetMaxCommand()
    {
        super("cmd_setmax", Emulator.getTexts().getValue("commands.keys.cmd_setmax").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception
    {
        if (params.length >= 2)
        {
            int max = 0;
            try
            {
                max = Integer.valueOf(params[1]);
            }
            catch (Exception e)
            {
                return false;
            }

            if (max > 0 && max < 9999)
            {
                gameClient.getHabbo().getHabboInfo().getCurrentRoom().setUsersMax(max);
                return true;
            }
            else
            {
                gameClient.sendResponse(new RoomUserWhisperComposer(new RoomChatMessage(Emulator.getTexts().getValue("commands.error.cmd_setmax.invalid_number"), gameClient.getHabbo(), gameClient.getHabbo(), RoomChatMessageBubbles.ALERT)));
                return true;
            }
        }
        else
        {
            gameClient.sendResponse(new RoomUserWhisperComposer(new RoomChatMessage(Emulator.getTexts().getValue("commands.error.cmd_setmax.forgot_number"), gameClient.getHabbo(), gameClient.getHabbo(), RoomChatMessageBubbles.ALERT)));
            return true;
        }
    }
}