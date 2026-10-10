package example;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.AppState;
import com.jme3.app.state.ConstantVerifierState;
import com.jme3.audio.AudioListenerState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.onemillionworlds.tamarin.actions.ActionType;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.XrActionAppState;
import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
import com.onemillionworlds.tamarin.actions.controllerprofile.GoogleDaydreamController;
import com.onemillionworlds.tamarin.actions.controllerprofile.HtcViveController;
import com.onemillionworlds.tamarin.actions.controllerprofile.MetaHandMicrogestures;
import com.onemillionworlds.tamarin.actions.controllerprofile.MixedRealityMotionController;
import com.onemillionworlds.tamarin.actions.controllerprofile.OculusGoController;
import com.onemillionworlds.tamarin.actions.controllerprofile.OculusTouchController;
import com.onemillionworlds.tamarin.actions.controllerprofile.ValveIndexController;
import com.onemillionworlds.tamarin.actions.gestures.Gesture;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;
import com.onemillionworlds.tamarin.openxr.XrSettings;
import com.onemillionworlds.tamarin.vrhands.HandSpec;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.skeletonsynthesis.DefaultSkeletonSynthesiser;
import com.onemillionworlds.tamarin.vrhands.skeletonsynthesis.SkeletonSynthesiser;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;
import example.actions.ActionHandles;
import example.actions.ActionSets;

import java.util.function.Function;
import java.util.logging.Logger;

public class Main extends SimpleApplication{

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.put("Renderer", AppSettings.LWJGL_OPENGL45);
        settings.setTitle("Tamarin OpenXR Example");
        settings.setSamples(4);
        settings.setWindowSize(1280, 720);
        settings.setVSync(false); // vr applications must not have v sync. Tamarin will sync for you
        XrSettings xrSettings = new XrSettings();
        xrSettings.setPassthroughSupport(true); // allows passthrough to be turned on at runtime (see PassthroughExampleState)
        //settings.setGraphicsDebug(true);

        VRHandsAppState vrHands = new VRHandsAppState(handSpec());
        vrHands.setSkeletonSynthesiser(new DefaultSkeletonSynthesiser(ActionHandles.TRIGGER, ActionHandles.GRIP, SkeletonSynthesiser.SynthesiseMode.FALLBACK_ONLY));
        Main app = new Main(
                new XrAppState(xrSettings),
                new XrActionAppState(manifest(), ActionSets.MAIN),
                vrHands,
                //these are just the default JME states (that we have to explicitly select because of using the constructor that takes states)
                new StatsAppState(),
                new ConstantVerifierState(),
                new AudioListenerState(),
                new DebugKeysAppState());
        app.setLostFocusBehavior(LostFocusBehavior.Disabled);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    public Main(AppState... initialStates) {
        super(initialStates);
    }

    @Override
    public void simpleInitApp(){

        getViewPort().setBackgroundColor(ColorRGBA.Brown);

        XrBaseAppState vrAppState = getStateManager().getState(XrBaseAppState.ID, XrBaseAppState.class);

        vrAppState.movePlayersFeetToPosition(new Vector3f(0,0,10));
        vrAppState.playerLookAtPosition(new Vector3f(0,0,0));

        getStateManager().attach(new MenuExampleState());

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        BaseStyles.loadStyleResources("com/onemillionworlds/tamarintestbed/tamarintestbed-extra-styles.groovy");
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        getCamera().setFrustumPerspective(120f, (float)cam.getWidth() / cam.getHeight(), 0.01f, 1000f);

        XrBaseAppState xrAppState = getStateManager().getState(XrBaseAppState.ID, XrBaseAppState.class);
        xrAppState.runAfterInitialisation(() -> LOGGER.info("System is: "+xrAppState.getSystemName()));
        xrAppState.setMainViewportConfiguration(vp -> {
            vp.setBackgroundColor(ColorRGBA.Brown);
        });


        //set up some lights to make the hands look better
        rootNode.addLight(new DirectionalLight(new Vector3f(-1, -1, -1).normalizeLocal(), new ColorRGBA(0.6f, 0.6f, 0.4f, 1f)));
        rootNode.addLight(new AmbientLight(new ColorRGBA(0.1f, 0.1f, 0.1f, 1f)));
        rootNode.addLight(new DirectionalLight(new Vector3f(0, -1, 1).normalizeLocal(), new ColorRGBA(0.5f, 0.45f, 0.5f, 1f)));
    }

    @Override
    public void simpleUpdate(float tpf){
        super.simpleUpdate(tpf);
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
                .withDesktopSimulationKeyTrigger(HandSide.LEFT, new KeyTrigger(KeyInput.KEY_F1), true)
                .withDesktopSimulationKeyTrigger(HandSide.RIGHT, new KeyTrigger(KeyInput.KEY_F2), true)
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
                // hand tracking: thumb and index finger pinch. This also suggests the runtime's own pinch binding
                // (HandInteractionExt), which is used where the runtime supports it, otherwise Tamarin detects the pinch
                .withGestureBinding(HandSide.LEFT, Gesture.PINCH_INDEX)
                .withGestureBinding(HandSide.RIGHT, Gesture.PINCH_INDEX)
                .withDesktopSimulationKeyTrigger(HandSide.LEFT, new KeyTrigger(KeyInput.KEY_F3), false)
                .withDesktopSimulationKeyTrigger(HandSide.RIGHT, new KeyTrigger(KeyInput.KEY_F4), false)
                .build();

        Action handPose = Action.builder()
                .actionHandle(ActionHandles.HAND_POSE)
                .translatedName("Hand Pose")
                .actionType(ActionType.POSE)
                .withSuggestAllKnownAimPoseBindings()
                .build();


        /*
         * This puts all the dpad controls into a single action, this sucks but is the only way to do it on older runtimes
         * which may not support the XR_EXT_dpad_binding extension
         */
        Action movementDPad = Action.builder()
                .actionHandle(ActionHandles.MOVEMENT_DPAD)
                .translatedName("Step Movement controls")
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
                .withSuggestedBinding(GoogleDaydreamController.PROFILE, GoogleDaydreamController.pathBuilder().leftHand().trackpadClick())
                .withSuggestedBinding(GoogleDaydreamController.PROFILE, GoogleDaydreamController.pathBuilder().rightHand().trackpadClick())
                .withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().leftHand().trackpadClick())
                .withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().rightHand().trackpadClick())
                .withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().leftHand().trackpadClick())
                .withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().rightHand().trackpadClick())
                .withSuggestedBinding(OculusGoController.PROFILE, OculusGoController.pathBuilder().leftHand().trackpadClick())
                .withSuggestedBinding(OculusGoController.PROFILE, OculusGoController.pathBuilder().rightHand().trackpadClick())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().thumbStickClick())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().thumbStickClick())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().aClick())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().aTouch())

                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().leftHand().thumbStickClick())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().rightHand().thumbStickClick())
                .withDesktopSimulationKeyTrigger(HandSide.LEFT, new KeyTrigger(KeyInput.KEY_F5), true)
                .withDesktopSimulationKeyTrigger(HandSide.RIGHT, new KeyTrigger(KeyInput.KEY_F6), true)
                .build();

        /*
         * META ONLY thumb microgestures (bare hands). On other runtimes these bindings are skipped and the actions
         * never fire. Used by MetaOnlyHandGesturesExampleState. The numpad keys simulate them in desktop simulation mode.
         */
        Action microgestureTap = microgestureAction(ActionHandles.MICROGESTURE_TAP, "Thumb tap", MetaHandMicrogestures.BindingPathBuilderHand::tapThumb, KeyInput.KEY_NUMPAD5);
        Action microgestureSwipeLeft = microgestureAction(ActionHandles.MICROGESTURE_SWIPE_LEFT, "Thumb swipe left", MetaHandMicrogestures.BindingPathBuilderHand::swipeLeft, KeyInput.KEY_NUMPAD4);
        Action microgestureSwipeRight = microgestureAction(ActionHandles.MICROGESTURE_SWIPE_RIGHT, "Thumb swipe right", MetaHandMicrogestures.BindingPathBuilderHand::swipeRight, KeyInput.KEY_NUMPAD6);
        Action microgestureSwipeForward = microgestureAction(ActionHandles.MICROGESTURE_SWIPE_FORWARD, "Thumb swipe forward", MetaHandMicrogestures.BindingPathBuilderHand::swipeForward, KeyInput.KEY_NUMPAD8);
        Action microgestureSwipeBackward = microgestureAction(ActionHandles.MICROGESTURE_SWIPE_BACKWARD, "Thumb swipe backward", MetaHandMicrogestures.BindingPathBuilderHand::swipeBackward, KeyInput.KEY_NUMPAD2);

        /*
         * A Tamarin side (synthetic) gesture binding. Tamarin detects the thumbs up from the hand joints, so this works on
         * any runtime with hand tracking (not just Meta). Used by MetaOnlyHandGesturesExampleState. Numpad 0 simulates it.
         */
        Action resetCube = Action.builder()
                .actionHandle(ActionHandles.RESET_CUBE)
                .translatedName("Reset cube")
                .actionType(ActionType.BOOLEAN)
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().xClick())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().leftHand().aClick())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().rightHand().aClick())
                .withGestureBinding(HandSide.LEFT, Gesture.THUMBS_UP)
                .withGestureBinding(HandSide.RIGHT, Gesture.THUMBS_UP)
                .withDesktopSimulationKeyTrigger(HandSide.RIGHT, new KeyTrigger(KeyInput.KEY_NUMPAD0), false)
                .build();

        /*
         * Spins the cube while held. Thumb and ring finger pinch (no OpenXR binding exists for that, so it is a gesture
         * binding) or the B/Y buttons. Used by MetaOnlyHandGesturesExampleState. Numpad 7 simulates it.
         */
        Action spinCube = Action.builder()
                .actionHandle(ActionHandles.SPIN_CUBE)
                .translatedName("Spin cube")
                .actionType(ActionType.BOOLEAN)
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().yClick())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().bClick())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().leftHand().bClick())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().rightHand().bClick())
                .withGestureBinding(HandSide.LEFT, Gesture.PINCH_RING)
                .withGestureBinding(HandSide.RIGHT, Gesture.PINCH_RING)
                .withDesktopSimulationKeyTrigger(HandSide.RIGHT, new KeyTrigger(KeyInput.KEY_NUMPAD7), false)
                .build();

        /*
         * A float action, the cube grows with its value. Thumb and middle finger pinch strength (a gesture binding) or the
         * grip. Used by MetaOnlyHandGesturesExampleState. Numpad 9 simulates it.
         */
        Action growCube = Action.builder()
                .actionHandle(ActionHandles.GROW_CUBE)
                .translatedName("Grow cube")
                .actionType(ActionType.FLOAT)
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().squeeze())
                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().squeeze())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().leftHand().squeezeValue())
                .withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().rightHand().squeezeValue())
                .withGestureBinding(HandSide.LEFT, Gesture.PINCH_MIDDLE)
                .withGestureBinding(HandSide.RIGHT, Gesture.PINCH_MIDDLE)
                .withDesktopSimulationKeyTrigger(HandSide.RIGHT, new KeyTrigger(KeyInput.KEY_NUMPAD9), false)
                .build();

        /*
         * Actions bound only to a hand gesture (no controller bindings at all). Nothing uses these other than
         * MetaOnlyHandGesturesExampleState showing when they are active. Only Tamarin detects these gestures.
         */
        Action fist = gestureOnlyAction(ActionHandles.FIST, "Fist", Gesture.FIST);
        Action point = gestureOnlyAction(ActionHandles.POINT, "Point", Gesture.POINT);
        Action thumbsUp = gestureOnlyAction(ActionHandles.THUMBS_UP, "Thumbs up", Gesture.THUMBS_UP);

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
                        .withAction(movementDPad)
                        .withAction(walk)
                        .withAction(openHandMenu)
                        .withAction(microgestureTap)
                        .withAction(microgestureSwipeLeft)
                        .withAction(microgestureSwipeRight)
                        .withAction(microgestureSwipeForward)
                        .withAction(microgestureSwipeBackward)
                        .withAction(resetCube)
                        .withAction(spinCube)
                        .withAction(growCube)
                        .withAction(fist)
                        .withAction(point)
                        .withAction(thumbsUp)
                        .build()
                ).build();
    }

    /**
     * META ONLY. A boolean action bound to a thumb microgesture on either hand.
     */
    private static Action microgestureAction(ActionHandle actionHandle, String translatedName, Function<MetaHandMicrogestures.BindingPathBuilderHand, String> gesture, int simulationKey){
        return Action.builder()
                .actionHandle(actionHandle)
                .translatedName(translatedName)
                .actionType(ActionType.BOOLEAN)
                .withSuggestedBinding(MetaHandMicrogestures.PROFILE, gesture.apply(MetaHandMicrogestures.pathBuilder().leftHand()))
                .withSuggestedBinding(MetaHandMicrogestures.PROFILE, gesture.apply(MetaHandMicrogestures.pathBuilder().rightHand()))
                .withDesktopSimulationKeyTrigger(HandSide.RIGHT, new KeyTrigger(simulationKey), false)
                .build();
    }

    /**
     * A boolean action bound only to a hand gesture (on either hand).
     */
    private static Action gestureOnlyAction(ActionHandle actionHandle, String translatedName, Gesture gesture){
        return Action.builder()
                .actionHandle(actionHandle)
                .translatedName(translatedName)
                .actionType(ActionType.BOOLEAN)
                .withGestureBinding(HandSide.LEFT, gesture)
                .withGestureBinding(HandSide.RIGHT, gesture)
                .build();
    }

    public static HandSpec handSpec(){
        return HandSpec.builder(
                        ActionHandles.HAND_POSE,
                        ActionHandles.HAND_POSE)
                .applyMaterialToLeftHand((hand, assetManager) -> {
                    //use the standard Tamarin texture but use a lit material instead
                    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                    mat.setTexture("DiffuseMap", assetManager.loadTexture("Tamarin/Textures/basicHands_pinStripe.png"));
                    hand.setMaterial(mat);
                })
                .applyMaterialToRightHand((hand, assetManager) -> {
                    //use the standard Tamarin texture but use a lit material instead
                    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                    mat.setTexture("DiffuseMap", assetManager.loadTexture("Tamarin/Textures/basicHands_pinStripe.png"));
                    hand.setMaterial(mat);
                })
                .build();
    }
}
