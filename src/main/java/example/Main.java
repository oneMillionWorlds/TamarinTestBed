package example;

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.onemillionworlds.tamarin.actions.ActionType;
import com.onemillionworlds.tamarin.actions.OpenXrActionState;
import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
import com.onemillionworlds.tamarin.actions.controllerprofile.GoogleDaydreamController;
import com.onemillionworlds.tamarin.actions.controllerprofile.HtcViveController;
import com.onemillionworlds.tamarin.actions.controllerprofile.MixedRealityMotionController;
import com.onemillionworlds.tamarin.actions.controllerprofile.OculusGoController;
import com.onemillionworlds.tamarin.actions.controllerprofile.OculusTouchController;
import com.onemillionworlds.tamarin.actions.controllerprofile.ValveIndexController;
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
        settings.setTitle("Tamarin OpenXR Example");
        settings.setVSync(false); // don't want to VSync to the monitor refresh rate, we want to VSync to the headset refresh rate (which tamarin implictly handles)

        Main app = new Main();
        app.setLostFocusBehavior(LostFocusBehavior.Disabled);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    public Main() {
        super();
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

        getCamera().setFrustumPerspective(45f, (float)cam.getWidth() / cam.getHeight(), 0.01f, 1000f);

        getStateManager().getState(XrAppState.ID, XrAppState.class).configureBothViewports(viewPort -> viewPort.setBackgroundColor(ColorRGBA.Brown));
    }

    public static ActionManifest manifest(){
        Action grip = Action.builder()
                .actionHandle(ActionHandles.GRIP)
                .translatedName("Grip an item")
                .actionType(ActionType.FLOAT)
                .withSuggestedBinding(GoogleDaydreamController.PROFILE, GoogleDaydreamController.pathBuilder().leftHand().trackpadClick())
                .withSuggestedBinding(GoogleDaydreamController.PROFILE, GoogleDaydreamController.pathBuilder().rightHand().trackpadClick())
                .withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().leftHand().squeezeClick())
                .withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().rightHand().squeezeClick())
                .withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().leftHand().squeezeClick())
                .withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().rightHand().squeezeClick())
                .withSuggestedBinding(OculusGoController.PROFILE, OculusGoController.pathBuilder().leftHand().trackpadX())
                .withSuggestedBinding(OculusGoController.PROFILE, OculusGoController.pathBuilder().rightHand().trackpadX())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().squeeze())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().squeeze())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().leftHand().squeezeValue())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().rightHand().squeezeValue())
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
                .withSuggestedBinding(GoogleDaydreamController.PROFILE, GoogleDaydreamController.pathBuilder().leftHand().selectClick())
                .withSuggestedBinding(GoogleDaydreamController.PROFILE, GoogleDaydreamController.pathBuilder().rightHand().selectClick())
                .withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().leftHand().triggerValue())
                .withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().rightHand().triggerValue())
                .withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().leftHand().triggerValue())
                .withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().rightHand().triggerValue())
                .withSuggestedBinding(OculusGoController.PROFILE, OculusGoController.pathBuilder().leftHand().triggerClick())
                .withSuggestedBinding(OculusGoController.PROFILE, OculusGoController.pathBuilder().rightHand().triggerClick())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().triggerValue())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().triggerValue())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().leftHand().triggerValue())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().rightHand().triggerValue())
                .build();

        Action handPose = Action.builder()
                .actionHandle(ActionHandles.HAND_POSE)
                .translatedName("Hand Pose")
                .actionType(ActionType.POSE)
                .withSuggestAllKnownAimPoseBindings()
                .build();

        Action syntheticDpad = Action.builder()
                .actionHandle(ActionHandles.SYNTHETIC_D_PAD)
                .translatedName("D Pad")
                .actionType(ActionType.VECTOR2F)
                .withSuggestedBinding(GoogleDaydreamController.PROFILE, GoogleDaydreamController.pathBuilder().leftHand().trackpad())
                .withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().leftHand().trackpad())
                .withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().leftHand().trackpad())
                .withSuggestedBinding(OculusGoController.PROFILE, OculusGoController.pathBuilder().leftHand().trackpad())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().thumbStick())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().leftHand().thumbStick())
                .build();

        Action walk = Action.builder()
                .actionHandle(ActionHandles.WALK)
                .translatedName("Walk")
                .actionType(ActionType.VECTOR2F)
                .withSuggestedBinding(GoogleDaydreamController.PROFILE, GoogleDaydreamController.pathBuilder().rightHand().trackpad())
                .withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().rightHand().trackpad())
                .withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().rightHand().trackpad())
                .withSuggestedBinding(OculusGoController.PROFILE, OculusGoController.pathBuilder().rightHand().trackpad())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().thumbStick())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().rightHand().thumbStick())
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
}
