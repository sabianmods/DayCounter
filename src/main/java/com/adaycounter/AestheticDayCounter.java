package com.adaycounter;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;


@Mod(AestheticDayCounter.MODID)
public class AestheticDayCounter {

    public static final String MODID = "adaycounter";

    public AestheticDayCounter(IEventBus modEventBus) {
        // Nothing to register on the mod bus - all the logic lives on
        // the game event bus (NeoForge.EVENT_BUS) via the
        // @EventBusSubscriber on ClientDayCounterHandler.
        // Keeping this constructor here in case you want to add
        // config registration (e.g. an offline-timestopper toggle) later.
        if (FMLEnvironment.dist.isClient()) {
            // Client-only mod - server dedicated builds don't need this class,
            // but it's harmless to leave loaded server-side since the handler
            // itself is annotated Dist.CLIENT and won't fire there.
        }
    }
}
