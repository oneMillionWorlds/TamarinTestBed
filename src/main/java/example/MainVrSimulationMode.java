package example;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.LostFocusBehavior;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ConstantVerifierState;
import com.jme3.system.AppSettings;
import com.onemillionworlds.tamarin.actions.DesktopSimulatingXrActionAppState;
import com.onemillionworlds.tamarin.actions.XrActionAppState;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.openxr.DesktopSimulatingXrAppState;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.openxr.XrSettings;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import example.actions.ActionHandles;
import example.actions.ActionSets;

public class MainVrSimulationMode{

    /**
     * This excercises the application in a VR-like way but without the need for a VR headset.
     * It is intended to be used for development and testing purposes, it isn't intended to be a user facing feature,
     * which would probably require more specific non VR behaviours.
     * @param args
     */
    public static void main(String[] args){
        AppSettings settings = new AppSettings(true);
        settings.put("Renderer", AppSettings.LWJGL_OPENGL45); // OpenXR only supports relatively modern OpenGL
        settings.setTitle("Tamarin OpenXR Example");
        settings.setVSync(false); // don't want to VSync to the monitor refresh rate, we want to VSync to the headset refresh rate (which tamarin implictly handles)
        settings.setSamples(4);
        settings.setWindowSize(1280, 720);
        Main app = new Main(
                new DesktopSimulatingXrAppState(),
                new DesktopSimulatingXrActionAppState(Main.manifest(), ActionHandles.HAND_POSE, ActionSets.MAIN),
                new VRHandsAppState(Main.handSpec()),
                //these are just the default JME states (that we have to explicitly select because of using the constructor that takes states)
                new FlyCamAppState(),
                new StatsAppState(),
                new ConstantVerifierState(),
                new DebugKeysAppState());
        app.setLostFocusBehavior(LostFocusBehavior.Disabled);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
}
