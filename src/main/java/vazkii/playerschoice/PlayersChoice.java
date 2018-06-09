package vazkii.playerschoice;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState.ModState;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

@Mod(modid = PlayersChoice.MOD_ID, name = PlayersChoice.MOD_NAME, version = PlayersChoice.VERSION, dependencies = PlayersChoice.DEPENDENCIES, clientSideOnly = true)
public class PlayersChoice {

	public static final String MOD_ID = "playerschoice";
	public static final String MOD_NAME = "Player's Choice";
	public static final String BUILD = "GRADLE:BUILD";
	public static final String VERSION = "GRADLE:VERSION-" + BUILD;	
	public static final String DEPENDENCIES = "before:*";

	@Instance
	public static PlayersChoice instance;

	private File markerFile;

	public ModSettings settings;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);

		File configFolder = event.getSuggestedConfigurationFile().getParentFile();
		File fmlPropFile = new File(configFolder, "fmlModState.properties");
		File jsonFile = new File(configFolder, "playerschoice.json");
		markerFile = new File(configFolder.getParentFile(), "playerschoice.marker");

		settings = new ModSettings(jsonFile, fmlPropFile);

		if(!markerFile.exists()) try {
			nukeMods();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void tick(ClientTickEvent event) {
		if(event.phase == Phase.START && Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu && !markerFile.exists())
			Minecraft.getMinecraft().displayGuiScreen(new GuiChooseMods());
	}

	public void addMarker() {
		if(!markerFile.exists())
			try {
				markerFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	// I am so sorry cpw
	private void nukeMods() throws ReflectiveOperationException {
		Loader loader = Loader.instance();
		List<ModContainer> mods = Loader.instance().getModList();
		LoadController controller = ReflectionHelper.getPrivateValue(Loader.class, loader, "modController");
		Multimap<ModContainer, ModState> states = ReflectionHelper.getPrivateValue(LoadController.class, controller, "modStates");
		List<ModContainer> activeMods = ReflectionHelper.getPrivateValue(LoadController.class, controller, "activeModList");
		int preInit = ModState.PREINITIALIZED.ordinal();

		for(ModContainer container : mods)
			if(container instanceof FMLModContainer && container.getMod() != this && loader.getModState(container).ordinal() < preInit) {
				states.put(container, ModState.DISABLED);

				List<ModContainer> localmods = Lists.newArrayList(mods);
				localmods.remove(container);
				mods = ImmutableList.copyOf(localmods);
				ReflectionHelper.setPrivateValue(Loader.class, loader, mods, "mods");

				activeMods.remove(container);
			}
	}

}
