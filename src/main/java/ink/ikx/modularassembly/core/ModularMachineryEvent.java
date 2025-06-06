package ink.ikx.modularassembly.core;

import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import ink.ikx.modularassembly.Main;
import ink.ikx.modularassembly.utils.MiscUtils;
import ink.ikx.modularassembly.utils.assembly.MachineAssembly;
import ink.ikx.modularassembly.utils.assembly.MachineAssemblyManager;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import youyihj.modularcontroller.block.BlockMMController;

import java.util.List;
import java.util.Set;

public class ModularMachineryEvent {

    public static ModularMachineryEvent INSTANCE = new ModularMachineryEvent();

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos blockPos = event.getPos();
        ItemStack stack = event.getItemStack();
        EntityPlayer player = event.getEntityPlayer();
        TileEntity tileEntity = world.getTileEntity(blockPos);
        Block block = world.getBlockState(blockPos).getBlock();

        if (world.isRemote) return;

        Item item = Item.getByNameOrId(Configuration.itemName);
        if (item == null) item = Items.STICK;

        if (tileEntity instanceof TileMachineController && !player.isSneaking()) {
            TileMachineController controller = (TileMachineController) tileEntity;
            if (stack.getItem().equals(ItemsMM.blueprint)) {
                if (getBlueprint(controller).isEmpty()) {
                    ItemStack copy = stack.copy();
                    copy.setCount(1);
                    if (isPlayerNotCreative(player)) stack.setCount(stack.getCount() - 1);
                    controller.getInventory().setStackInSlot(TileMachineController.BLUEPRINT_SLOT, copy);
                }
                event.setCanceled(true);
            } else if (stack.isItemEqual(new ItemStack(item, 1, Configuration.itemMeta))) {
                if (Main.instance.isMoCLoaded() && block instanceof BlockMMController) {
                    BlockMMController blockMMController = (BlockMMController) block;
                    List<DynamicMachine> machineList = blockMMController.getAssociatedMachines();
                    for (int i = 0; i < machineList.size(); i++) {
                        DynamicMachine machine = machineList.get(i);
                        if ((machineList.size() - 1) == i) {
                            assemblyBefore(machine, player, blockPos, false);
                        } else {
                            if (assemblyBefore(machine, player, blockPos, true)) break;
                        }
                    }
                } else {
                    DynamicMachine blueprintMachine = controller.getBlueprintMachine();
                    assemblyBefore(blueprintMachine, player, blockPos, false);
                }
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        World world = player.world;
        if (event.phase == TickEvent.Phase.START || event.side.isClient() || world.getWorldTime() % Configuration.tickBlock != 0)
            return;

        Set<MachineAssembly> machineAssemblyListFromPlayer = MachineAssemblyManager.getMachineAssemblyListFromPlayer(player);

        if (MiscUtils.isNotEmpty(machineAssemblyListFromPlayer)) {
            machineAssemblyListFromPlayer.stream().filter(MachineAssembly::isFilter).forEach(MachineAssembly::assembly);
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Main.MOD_ID)) {
            ConfigManager.sync(Main.MOD_ID, Config.Type.INSTANCE);
        }
    }

    private boolean assemblyBefore(DynamicMachine machine, EntityPlayer player, BlockPos pos, boolean isMoc) {
        if (machine == null) {
            if (!isMoc) player.sendMessage(MiscUtils.translate(1));
            return false;
        }

        EnumFacing controllerFacing = player.world.getBlockState(pos).getValue(BlockController.FACING);
        BlockArray blockArray = rotateYCCWNorthUntil(machine.getPattern(), controllerFacing);
        MachineAssembly Machine = new MachineAssembly(pos, player, blockArray.getPattern());

        if (MachineAssemblyManager.checkMachineExist(Machine)) {
            player.sendMessage(MiscUtils.translate(2));
            return false;
        }
        if (isPlayerNotCreative(player)) {
            if (!Machine.isAllItemsContains() && Configuration.needAllBlocks) {
                if (!isMoc) player.sendMessage(MiscUtils.translate(3));
                return false;
            } else
                MachineAssemblyManager.addMachineAssembly(Machine);
        } else {
            Machine.buildWithCreative();
        }
        return true;
    }

    private ItemStack getBlueprint(TileMachineController controller) {
        return controller.getInventory().getStackInSlot(TileMachineController.BLUEPRINT_SLOT);
    }

    private boolean isPlayerNotCreative(EntityPlayer player) {
        return !player.isCreative();
    }

    public static BlockPos rotateYCCWNorthUntil(BlockPos at, EnumFacing dir) {
        EnumFacing currentFacing = EnumFacing.NORTH;
        BlockPos pos = at;
        while (currentFacing != dir) {
            currentFacing = currentFacing.rotateYCCW();
            pos = new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
        }
        return pos;
    }

    public static BlockArray rotateYCCWNorthUntil(BlockArray array, EnumFacing dir) {
        EnumFacing currentFacing = EnumFacing.NORTH;
        BlockArray rot = array;
        while (currentFacing != dir) {
            currentFacing = currentFacing.rotateYCCW();
            rot = rot.rotateYCCW();
        }
        return rot;
    }
}
