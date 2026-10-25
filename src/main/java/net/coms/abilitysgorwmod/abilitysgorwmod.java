package net.coms.abilitysgorwmod;

import net.coms.abilitysgorwmod.item.AbilityObtainItem;
import net.coms.abilitysgorwmod.item.SpecialLensItem;
import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(abilitysgorwmod.MODID)
public class abilitysgorwmod {

    public static final String MODID = "abilitysgorwmod";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Item> ABILITY_OBTAIN_ITEM =
            ITEMS.register(
                    "ability_obtain_item",
                    () -> new AbilityObtainItem(
                            new Item.Properties().stacksTo(16)
                    )
            );

    public static final RegistryObject<Item> ABILITY_OBTAIN_ITEM_RARE =
            ITEMS.register("ability_obtain_item_rare",
                    () -> new AbilityObtainItem(new Item.Properties().stacksTo(16),
                            AbilityDefinition.Rarity.RARE));

    public static final RegistryObject<Item> ABILITY_OBTAIN_ITEM_EPIC =
            ITEMS.register("ability_obtain_item_epic",
                    () -> new AbilityObtainItem(new Item.Properties().stacksTo(16),
                            AbilityDefinition.Rarity.EPIC));

    public static final RegistryObject<Item> ABILITY_OBTAIN_ITEM_LEGENDARY =
            ITEMS.register("ability_obtain_item_legendary",
                    () -> new AbilityObtainItem(new Item.Properties().stacksTo(16),
                            AbilityDefinition.Rarity.LEGENDARY));

    public static final RegistryObject<Item> SPECIAL_LENS =
            ITEMS.register(
                    "special_lens",
                    () -> new SpecialLensItem(
                            new Item.Properties().stacksTo(1)
                    )
            );

    public static final RegistryObject<CreativeModeTab> ABILITY_TAB = CREATIVE_TABS.register(
            "abilitysgorwmod_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.abilitysgorwmod"))
                    .icon(() -> SPECIAL_LENS.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ABILITY_OBTAIN_ITEM.get());
                        output.accept(ABILITY_OBTAIN_ITEM_RARE.get());
                        output.accept(ABILITY_OBTAIN_ITEM_EPIC.get());
                        output.accept(ABILITY_OBTAIN_ITEM_LEGENDARY.get());
                        output.accept(SPECIAL_LENS.get());
                    })
                    .build()
    );

    public abilitysgorwmod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        AbilityNetwork.register();
    }
}