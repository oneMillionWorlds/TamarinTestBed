package example;

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.onemillionworlds.tamarin.actions.ActionType;
import com.onemillionworlds.tamarin.actions.OpenXrActionState;
import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
import com.onemillionworlds.tamarin.actions.controllerprofile.OculusTouchController;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.openxr.XrSettings;
import com.onemillionworlds.tamarin.vrhands.HandSpec;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;
import example.actions.ActionHandles;
import example.actions.ActionSets;

public class Main extends SimpleApplication{

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.put("Renderer", AppSettings.LWJGL_OPENGL45); // OpenXR only supports relatively modern OpenGL

        Main app = new Main();
        app.setLostFocusBehavior(LostFocusBehavior.Disabled);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    public Main(AppState... appStates) {
        super(appStates);
    }

    @Override
    public void simpleInitApp(){
        XrSettings xrSettings = new XrSettings();
        getStateManager().attach(new XrAppState(xrSettings));
        getStateManager().attach(new OpenXrActionState(manifest(), ActionSets.MAIN));
        getStateManager().attach(new VRHandsAppState(handSpec()));

        XrAppState vrAppState = getStateManager().getState(XrAppState.class);

        vrAppState.movePlayersFeetToPosition(new Vector3f(0,0,10));
        vrAppState.playerLookAtPosition(new Vector3f(0,0,0));

        getStateManager().attach(new MenuExampleState());

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");


        getStateManager().getState(XrAppState.ID, XrAppState.class).configureBothViewports(viewPort -> viewPort.setBackgroundColor(ColorRGBA.Brown));
    }

    public static ActionManifest manifest(){
        Action grip = Action.builder()
                .actionHandle(ActionHandles.GRIP)
                .translatedName("Grip an item")
                .actionType(ActionType.FLOAT)
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().squeeze())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().squeeze())
                .build();

        Action haptic = Action.builder()
                .actionHandle(ActionHandles.HAPTIC)
                .translatedName("Haptic feedback")
                .actionType(ActionType.HAPTIC)
                .withSuggestAllKnownHapticBindings()
                .build();

        Action trigger = Action.builder()
                .actionHandle(ActionHandles.TRIGGER)
                .translatedName("Trigger action")
                .actionType(ActionType.FLOAT)
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().triggerValue())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().triggerValue())
                .build();

        Action handPose = Action.builder()
                .actionHandle(ActionHandles.HAND_POSE)
                .translatedName("Hand Pose")
                .actionType(ActionType.POSE)
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().gripPose())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().gripPose())
                .build();

        Action syntheticDpad = Action.builder()
                .actionHandle(ActionHandles.SYNTHETIC_D_PAD)
                .translatedName("D Pad")
                .actionType(ActionType.VECTOR2F)
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().thumbStick())
                .build();

        Action walk = Action.builder()
                .actionHandle(ActionHandles.WALK)
                .translatedName("Walk")
                .actionType(ActionType.VECTOR2F)
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().thumbStick())
                .build();

        Action openHandMenu = Action.builder()
                .actionHandle(ActionHandles.OPEN_HAND_MENU)
                .translatedName("Open Hand Menu")
                .actionType(ActionType.BOOLEAN)
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().thumbStickClick())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().thumbStickClick())
                .build();

        return ActionManifest.builder()
                .withActionSet(ActionSet
                        .builder()
                        .name("main")
                        .translatedName("Main Actions")
                        .priority(1)
                        .withAction(grip)
                        .withAction(haptic)
                        .withAction(trigger)
                        .withAction(handPose)
                        .withAction(syntheticDpad)
                        .withAction(walk)
                        .withAction(openHandMenu)
                        .build()
                ).build();
    }

    public static HandSpec handSpec(){
        return HandSpec.builder(
                        ActionHandles.HAND_POSE,
                        ActionHandles.HAND_POSE)
                .build();

    }

    private Spatial pointingLineDebug(){
        Node node = new Node();

        Geometry lineX = new Geometry("line", new com.jme3.scene.shape.Line(new Vector3f(0,0,0), new Vector3f(0.10f,0,0)));
        lineX.setMaterial(new com.jme3.material.Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        lineX.getMaterial().setColor("Color", ColorRGBA.Green);
        node.attachChild(lineX);

        Geometry lineY = new Geometry("line", new com.jme3.scene.shape.Line(new Vector3f(0,0,0), new Vector3f(0,0.10f,0)));
        lineY.setMaterial(new com.jme3.material.Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        lineY.getMaterial().setColor("Color", ColorRGBA.Yellow);
        node.attachChild(lineY);

        Geometry linez = new Geometry("line", new com.jme3.scene.shape.Line(new Vector3f(0,0,0), new Vector3f(0,0,0.10f)));
        linez.setMaterial(new com.jme3.material.Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        linez.getMaterial().setColor("Color", ColorRGBA.Red);
        node.attachChild(linez);


        return node;
    }

}
