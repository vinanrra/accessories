package io.wispforest.cclayer;

import io.wispforest.accessories.networking.AccessoriesPacket;

public class WrappedAccessoriesPacket {

    public final AccessoriesPacket packet;

    protected WrappedAccessoriesPacket(AccessoriesPacket packet){
        this.packet = packet;
    }
}
