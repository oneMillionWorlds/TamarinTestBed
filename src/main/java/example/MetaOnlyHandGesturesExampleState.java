package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.XrActionBaseAppState;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.controllerprofile.MetaHandMicrogestures;
import com.onemillionworlds.tamarin.actions.gestures.HandGestureSourcePreference;
import com.onemillionworlds.tamarin.actions.gestures.HandTrackingAimState;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates hand tracking (bare hands, no controllers) gestures by moving cubes around. Each hand controls its own
 * cube (the left hand controls the left cube, the right hand the right cube).
 * <p>
 *     1) <b>META ONLY</b> thumb microgestures (XR_META_hand_tracking_microgestures). These go through the normal action
 *     system: see the MICROGESTURE_* actions in the action manifest, which are bound using
 *     {@link MetaHandMicrogestures}. Each action is bound on both hands, and the state is read per hand by passing
 *     the hand's input string (sub action path) when getting the action state. Raise your thumb above your index finger
 *     (other fingers slightly curled) then:
 * </p>
 * <ul>
 *     <li>swipe the thumb left/right along the index finger to move the cube left/right</li>
 *     <li>swipe the thumb forward/backward across the index finger to move the cube away/towards you</li>
 *     <li>tap the thumb on the side of the index finger to make the cube change colour</li>
 * </ul>
 * <p>
 *     2) Actions with gesture bindings (see withGestureBinding in the action manifest). Each action is bound to both
 *     controller buttons and a hand gesture, and the game code just reads the action as normal (it doesn't know or care
 *     which was used). Each gesture comes from the runtime's own gesture recognition where it has one (the index pinch
 *     via XR_EXT_hand_interaction, the other pinches via Meta's XR_FB_hand_tracking_aim) and from Tamarin's otherwise
 *     (worked out from the hand's joints, so it works on any runtime with hand tracking):
 * </p>
 * <ul>
 *     <li>GROW_CUBE (float): pinch the thumb and middle finger together (or squeeze the grip) to grow the cube. As a
 *     float action it gets the pinch's strength, so the cube grows as the fingers get closer</li>
 *     <li>SPIN_CUBE: pinch the thumb and ring finger together (or hold B/Y) to spin the cube</li>
 *     <li>RESET_CUBE: give a thumbs up (or press X on Touch, A on Index) to reset the cube</li>
 *     <li>TRIGGER: the thumb and index finger pinch, which clicks the Lemur buttons</li>
 * </ul>
 * <p>
 *     The button switches the (global) hand gesture source preference. AUTOMATIC uses the runtime's version of a
 *     gesture if it has one and Tamarin's otherwise. RUNTIME_ONLY never uses Tamarin's version of a gesture the runtime
 *     could provide (so the pinches only work if the runtime supports them). TAMARIN_ONLY always uses Tamarin's
 *     gestures. This lets Meta's and Tamarin's gestures be compared. The text shows which actions are active for
 *     each hand. That includes FIST, POINT and THUMBS_UP, which are bound only to gestures and exist just to be shown.
 * </p>
 * <p>
 *     Tamarin only detects gestures from a bare hand. The text shows where each hand's joints are coming from, if it
 *     says CONTROLLER (e.g. Index controllers on SteamVR, which sense the fingers) Tamarin won't detect gestures.
 * </p>
 * <p>
 *     In desktop simulation the microgestures are on the numpad (4/6/8/2 to swipe, 5 to tap) and act as the right
 *     hand. Numpad 0 resets, 7 spins and 9 grows. The hand gestures themselves have no simulation.
 * </p>
 */
public class MetaOnlyHandGesturesExampleState extends BaseAppState{

    private static final float SWIPE_STEP = 0.1f;

    /**
     * How long (seconds) after changing the gesture source further clicks on the source button are ignored
     */
    private static final float SOURCE_BUTTON_DEAD_TIME = 0.1f;

    private static final ColorRGBA[] CUBE_COLOURS = new ColorRGBA[]{
            ColorRGBA.Orange, ColorRGBA.Cyan, ColorRGBA.Magenta, ColorRGBA.Yellow, ColorRGBA.Green
    };

    Node rootNodeDelegate = new Node("MetaOnlyHandGesturesExampleState");

    XrBaseAppState xrAppState;
    XrActionBaseAppState xrActionAppState;

    Label statusLabel;
    Label lastGestureLabel;
    Button sourceButton;
    float timeSinceSourceChange = Float.MAX_VALUE;

    List<FunctionRegistration> functionRegistrations = new ArrayList<>();

    /**
     * One per hand, each hand only ever affects its own cube.
     */
    List<HandCube> handCubes = new ArrayList<>();

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        xrAppState = getState(XrBaseAppState.ID, XrBaseAppState.class);
        xrActionAppState = getState(XrActionBaseAppState.ID, XrActionBaseAppState.class);

        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.005f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        lemurWindow.addChild(new Label("Bare hands. Each hand -> its own cube.\n\n" +
                "Microgestures (Meta only, thumb raised over index finger):\n" +
                "  Swipe left/right -> move left/right\n" +
                "  Swipe forward/back -> move away/towards\n" +
                "  Tap -> change colour\n\n" +
                "Gestures (any runtime, controller button in brackets):\n" +
                "  Middle pinch (grip) -> grow\n" +
                "  Ring pinch (B/Y) -> spin\n" +
                "  Thumbs up (X, or A on Index) -> reset\n" +
                "  Index pinch -> click\n" +
                "  Fist, point, thumbs up -> just shown below\n\n" +
                "Button: AUTOMATIC -> runtime's gesture if it has one, else Tamarin's.\n" +
                "  RUNTIME_ONLY -> runtime's only. TAMARIN_ONLY -> Tamarin's only."));
        statusLabel = lemurWindow.addChild(new Label(""));
        lastGestureLabel = lemurWindow.addChild(new Label("Last microgesture: none"));

        sourceButton = lemurWindow.addChild(new Button(""));
        sourceButton.addClickCommands(source -> {
            // changing the source while pinching changes where the trigger's value comes from mid pinch, which can
            // look like a second click. So ignore clicks for a moment after a change
            if (timeSinceSourceChange < SOURCE_BUTTON_DEAD_TIME){
                return;
            }
            timeSinceSourceChange = 0;
            // cycles AUTOMATIC -> RUNTIME_ONLY -> TAMARIN_ONLY. This is global (it applies to every gesture binding)
            // so the example resets it when it closes
            HandGestureSourcePreference[] preferences = HandGestureSourcePreference.values();
            HandGestureSourcePreference current = xrActionAppState.getHandGestureSourcePreference();
            xrActionAppState.setHandGestureSourcePreference(preferences[(current.ordinal() + 1) % preferences.length]);
            updateSourceButtonText();
        });
        updateSourceButtonText();

        lemurWindow.addChild(new Button("Back to menu")).addClickCommands(source -> {
            getStateManager().detach(this);
            getStateManager().attach(new MenuExampleState());
        });

        lemurWindow.setLocalTranslation(-1.3f, 2.1f, 8.6f);
        rootNodeDelegate.attachChild(lemurWindow);

        handCubes.add(new HandCube(HandSide.LEFT, new Vector3f(-0.3f, 1.1f, 9f), 0));
        handCubes.add(new HandCube(HandSide.RIGHT, new Vector3f(0.3f, 1.1f, 9f), 1));

        VRHandsAppState vrHands = getState(VRHandsAppState.ID, VRHandsAppState.class);
        vrHands.getHandControls().forEach(h -> {
            functionRegistrations.add(h.attachPickLine(pickLine()));
            functionRegistrations.add(h.setPickMarkerContinuous(rootNodeDelegate));
            functionRegistrations.add(h.setClickAction_lemurSupport(ActionHandles.TRIGGER, rootNodeDelegate));
        });

        xrAppState.movePlayersFeetToPosition(new Vector3f(0,0,10));
        xrAppState.playerLookAtPosition(new Vector3f(0,0,0));
    }

    private void updateSourceButtonText(){
        sourceButton.setText("Source: " + xrActionAppState.getHandGestureSourcePreference() + " (click to change)");
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        timeSinceSourceChange += tpf;

        StringBuilder handStatus = new StringBuilder();
        for(HandCube handCube : handCubes){
            handCube.handleMicrogestures();
            handCube.handleCubeActions(tpf);
            handCube.updateCube(tpf);
            handStatus.append("\n").append(describeHand(handCube.handSide));
        }

        statusLabel.setText(
                "Microgestures: " + yesNo(xrAppState.checkExtensionLoaded(MetaHandMicrogestures.REQUIRED_EXTENSION)) +
                ". Meta pinches: " + yesNo(xrAppState.checkExtensionLoaded(HandTrackingAimState.REQUIRED_EXTENSION)) +
                handStatus);
    }

    /**
     * Where the hand's joints are coming from (only a bare hand gives Tamarin's gestures) and which of the example's
     * actions are active for the hand. These are read through the action system like any other action, so they show
     * the gesture from whichever source the preference picks (and controller buttons too, for actions that have them).
     */
    private String describeHand(HandSide handSide){
        List<String> active = new ArrayList<>();
        addIfAboveZero(active, handSide, ActionHandles.TRIGGER, "click");
        addIfAboveZero(active, handSide, ActionHandles.GROW_CUBE, "grow");
        addIfActive(active, handSide, ActionHandles.SPIN_CUBE, "spin");
        addIfActive(active, handSide, ActionHandles.RESET_CUBE, "reset");
        addIfActive(active, handSide, ActionHandles.FIST, "fist");
        addIfActive(active, handSide, ActionHandles.POINT, "point");
        addIfActive(active, handSide, ActionHandles.THUMBS_UP, "thumbs up");
        return handSide + " joints: " + xrActionAppState.getHandJointsDataSource(handSide) +
                ". Active: " + (active.isEmpty() ? "nothing" : String.join(", ", active));
    }

    private void addIfActive(List<String> active, HandSide handSide, ActionHandle actionHandle, String name){
        if (xrActionAppState.getBooleanActionState(actionHandle, handSide.restrictToInputString).getState()){
            active.add(name);
        }
    }

    /**
     * For float actions (e.g. a pinch's strength, or how far the trigger is pulled), shown with their value.
     */
    private void addIfAboveZero(List<String> active, HandSide handSide, ActionHandle actionHandle, String name){
        float value = xrActionAppState.getFloatActionState(actionHandle, handSide.restrictToInputString).getState();
        if (value > 0){
            active.add(String.format("%s %.2f", name, value));
        }
    }

    private static String yesNo(boolean value){
        return value ? "yes" : "no";
    }

    @Override
    protected void cleanup(Application app){
        // this is a global setting, so don't leave it changed for the rest of the testbed
        xrActionAppState.setHandGestureSourcePreference(HandGestureSourcePreference.AUTOMATIC);
        rootNodeDelegate.removeFromParent();
        functionRegistrations.forEach(FunctionRegistration::endFunction);
    }

    @Override protected void onEnable(){}

    @Override protected void onDisable(){}

    /**
     * A cube and the hand that controls it.
     */
    private class HandCube{
        final HandSide handSide;
        final Vector3f startPosition;

        final Geometry cube;
        final Material cubeMaterial;
        int cubeColourIndex;

        /**
         * Where the cube is heading. The cube moves smoothly towards this rather than jumping.
         */
        final Vector3f targetPosition;
        final Vector3f currentPosition;
        float spinAngle = 0;

        HandCube(HandSide handSide, Vector3f startPosition, int startingColourIndex){
            this.handSide = handSide;
            this.startPosition = startPosition;
            this.targetPosition = startPosition.clone();
            this.currentPosition = startPosition.clone();
            this.cubeColourIndex = startingColourIndex;

            cubeMaterial = new Material(getApplication().getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
            cubeMaterial.setBoolean("UseMaterialColors", true);
            setCubeColour(CUBE_COLOURS[cubeColourIndex]);
            cube = new Geometry("gestureCube_" + handSide, new Box(0.1f, 0.1f, 0.1f));
            cube.setMaterial(cubeMaterial);
            cube.setLocalTranslation(startPosition);
            rootNodeDelegate.attachChild(cube);
        }

        /**
         * Microgestures are ordinary boolean actions, they become true once the gesture is recognised. Using
         * hasChangedToTrue means each gesture moves the cube exactly once.
         */
        void handleMicrogestures(){
            if (actionJustHappened(ActionHandles.MICROGESTURE_SWIPE_LEFT)){
                targetPosition.x -= SWIPE_STEP;
                lastGestureLabel.setText("Last microgesture: " + handSide + " swipe left");
            }
            if (actionJustHappened(ActionHandles.MICROGESTURE_SWIPE_RIGHT)){
                targetPosition.x += SWIPE_STEP;
                lastGestureLabel.setText("Last microgesture: " + handSide + " swipe right");
            }
            if (actionJustHappened(ActionHandles.MICROGESTURE_SWIPE_FORWARD)){
                // the player is looking down the negative z axis, so forward (away from the player) is -z
                targetPosition.z -= SWIPE_STEP;
                lastGestureLabel.setText("Last microgesture: " + handSide + " swipe forward");
            }
            if (actionJustHappened(ActionHandles.MICROGESTURE_SWIPE_BACKWARD)){
                targetPosition.z += SWIPE_STEP;
                lastGestureLabel.setText("Last microgesture: " + handSide + " swipe backward");
            }
            if (actionJustHappened(ActionHandles.MICROGESTURE_TAP)){
                cubeColourIndex = (cubeColourIndex + 1) % CUBE_COLOURS.length;
                setCubeColour(CUBE_COLOURS[cubeColourIndex]);
                lastGestureLabel.setText("Last microgesture: " + handSide + " tap");
            }
        }

        /**
         * These actions are bound to both controller buttons and hand gestures (gesture bindings, in the action
         * manifest) but they are read just like any other action, this code doesn't know or care which was used.
         */
        void handleCubeActions(float tpf){
            // a thumbs up, or the X button (Touch) / A button (Index)
            if (actionJustHappened(ActionHandles.RESET_CUBE)){
                reset();
            }
            // a thumb and ring finger pinch, or the B/Y buttons
            if (xrActionAppState.getBooleanActionState(ActionHandles.SPIN_CUBE, handSide.restrictToInputString).getState()){
                spinAngle += tpf * FastMath.PI;
            }
            // a float action, so it can drive something continuous like the size of the cube. The thumb and middle
            // finger pinch strength, or the grip
            float grow = xrActionAppState.getFloatActionState(ActionHandles.GROW_CUBE, handSide.restrictToInputString).getState();
            cube.setLocalScale(1 + 1.5f * grow);
        }

        /**
         * The action is bound on both hands, passing the hand's input string restricts the state to just this hand.
         */
        private boolean actionJustHappened(ActionHandle actionHandle){
            return xrActionAppState.getBooleanActionState(actionHandle, handSide.restrictToInputString).hasChangedToTrue();
        }

        void updateCube(float tpf){
            // move the cube smoothly towards where it is meant to be, so the gestures feel physical rather than teleporting it
            currentPosition.interpolateLocal(targetPosition, Math.min(1, tpf * 10));
            cube.setLocalTranslation(currentPosition);
            cube.setLocalRotation(new Quaternion().fromAngleAxis(spinAngle, Vector3f.UNIT_Y));
        }

        void reset(){
            targetPosition.set(startPosition);
            spinAngle = 0;
        }

        void setCubeColour(ColorRGBA colour){
            cubeMaterial.setColor("Diffuse", colour);
            cubeMaterial.setColor("Ambient", colour);
        }
    }

    @SuppressWarnings("DuplicatedCode") //as the examples are supposed to be self-contained accept some duplication
    private Spatial pickLine(){
        float length = 1;

        Cylinder pickCylinder = new Cylinder(10,10, 0.002f, length, true);

        Geometry geometry = new Geometry("debugHandLine", pickCylinder);
        Material material = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", new ColorRGBA(0.4f,0.4f,0.4f,0.5f));
        geometry.setMaterial(material);

        Quaternion rotate = new Quaternion();
        rotate.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        geometry.setLocalRotation(rotate);
        geometry.setLocalTranslation(length/2, 0,0);
        geometry.getMaterial().getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geometry.setQueueBucket(RenderQueue.Bucket.Transparent);

        return geometry;
    }
}
