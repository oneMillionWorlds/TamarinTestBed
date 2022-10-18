package example;

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.app.VRAppState;
import com.jme3.app.VRConstants;
import com.jme3.app.VREnvironment;
import com.jme3.app.state.AppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.vrhands.HandSpec;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;

import java.io.File;

public class Main extends SimpleApplication{

    Node observerNode = new Node();

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.put(VRConstants.SETTING_VRAPI, VRConstants.SETTING_VRAPI_OPENVR_LWJGL_VALUE);

        VREnvironment env = new VREnvironment(settings);
        env.initialize();
        if (env.isInitialized()){
            VRAppState vrAppState = new VRAppState(settings, env);

            Main app = new Main(vrAppState, new ActionBasedOpenVrState());
            app.setLostFocusBehavior(LostFocusBehavior.Disabled);
            app.setSettings(settings);
            app.setShowSettings(false);
            app.start();
        }
    }

    public Main(AppState... appStates) {
        super(appStates);
    }

    @Override
    public void simpleInitApp(){
        ActionBasedOpenVrState actionBasedOpenVrState = getStateManager().getState(ActionBasedOpenVrState.class);
        VRAppState vrAppState = getStateManager().getState(VRAppState.class);
        rootNode.attachChild(observerNode);

        observerNode.setLocalTranslation(0,0,10);
        observerNode.lookAt(new Vector3f(0,0,0), Vector3f.UNIT_Y);
        vrAppState.setObserver(observerNode);

        actionBasedOpenVrState.registerActionManifest(new File("openVr/actionManifest.json").getAbsolutePath(), "/actions/main" );

        getStateManager().attach(new VRHandsAppState(handSpec()));
        getStateManager().attach(new MenuExampleState());

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

    }

    @Override
    public void simpleUpdate(float tpf){
        super.simpleUpdate(tpf);

        VRAppState vrAppState = getStateManager().getState(VRAppState.class);
        vrAppState.getLeftViewPort().setBackgroundColor(ColorRGBA.Brown);
        vrAppState.getRightViewPort().setBackgroundColor(ColorRGBA.Brown);

    }

    private HandSpec handSpec(){
        return HandSpec.builder(
                "/actions/main/in/HandPoseLeft",
                "/actions/main/in/HandSkeletonLeft",
                "/actions/main/in/HandPoseRight",
                "/actions/main/in/HandSkeletonRight")
                .postBindLeft(leftHand ->
                    leftHand.setGrabAction("/actions/main/in/grip", rootNode)
                )
                .postBindRight(rightHand ->
                    rightHand.setGrabAction("/actions/main/in/grip", rootNode)
                )
                .build();

    }

}
