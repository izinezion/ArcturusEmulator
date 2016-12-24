package com.eu.habbo.messages.outgoing.catalog;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class NotEnoughPointsTypeComposer extends MessageComposer
{
    private final boolean isCredits;
    private final boolean isPixels;
    private final int pointsType;

    public NotEnoughPointsTypeComposer(boolean isCredits, boolean isPixels, int pointsType)
    {
        this.isCredits = isCredits;
        this.isPixels = isPixels;
        this.pointsType = pointsType;
    }

    @Override
    public ServerMessage compose()
    {
        this.response.init(Outgoing.NotEnoughPointsTypeComposer);
        this.response.appendBoolean(this.isCredits);
        this.response.appendBoolean(this.isPixels);
        this.response.appendInt32(this.pointsType);
        return this.response;
    }
}