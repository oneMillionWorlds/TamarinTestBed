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
import com.jme3.scene.shape.Cylinder;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.XrActionBaseAppState;
import com.onemillionworlds.tamarin.observable.ObservableDataEventSubscription;
import com.onemillionworlds.tamarin.observable.ObservableEventSubscription;
import com.onemillionworlds.tamarin.openxr.GameplayInterrupt;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Demonstrates pausing the game with a "Continue" message whenever gameplay is interrupted: the system menu is opened,
 * the headset is taken off, or a controller is lost (e.g. its battery runs out). All of these come through a single
 * subscription, {@link XrBaseAppState#subscribeToGameplayInterrupts()}.
 * <p>
 *     Unlike {@link SessionFocusExampleState} (which resumes automatically) the game stays paused until the user presses
 *     continue. That matters for lost controllers, which may never come back (the user may want to carry on without one).
 * </p>
 * <p>
 *     This example also shows a log of the raw events so it can be seen how different actions are reported by the
 *     runtime.
 * </p>
 */
public class GameplayInterruptExampleState extends BaseAppState{

    private static final int MAX_LOG_LINES = 14;

    Node rootNodeDelegate = new Node("GameplayInterruptExampleState");

    XrBaseAppState xrAppState;
    XrActionBaseAppState xrActionState;

    ObservableDataEventSubscription<GameplayInterrupt> interruptSubscription;

    // only used for the diagnostic log
    ObservableEventSubscription interactionProfileSubscription;

    float secondsCounted = 0;
    float timeSinceStart = 0;

    /**
     * The interrupts the user hasn't yet dismissed (keyed by type and hand), and whether each has ended (i.e. the
     * headset is back on, the controller is back etc.)
     */
    Map<String, InterruptStatus> undismissedInterrupts = new LinkedHashMap<>();

    Label counterLabel;
    Label currentStateLabel;
    Label eventLogLabel;
    LinkedList<String> eventLog = new LinkedList<>();

    Container pauseDialog;
    Label pauseDialogLabel;

    List<FunctionRegistration> functionRegistrations = new ArrayList<>();

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);

        xrAppState = getState(XrBaseAppState.ID, XrBaseAppState.class);
        xrActionState = getState(XrActionBaseAppState.ID, XrActionBaseAppState.class);

        interruptSubscription = xrAppState.subscribeToGameplayInterrupts();
        interactionProfileSubscription = xrActionState.subscribeToInteractionProfileChanges();

        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.005f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        lemurWindow.addChild(new Label("The counter counts up while you are using the application.\n\n" +
                "Try opening the system menu, taking the headset off, and removing a controller's battery (or\n" +
                "putting a controller down). Each should pause the counter and show a message you dismiss to continue.\n" +
                "The log below shows the interrupts and the raw interaction profile changes."));
        counterLabel = lemurWindow.addChild(new Label(""));
        counterLabel.setFontSize(40);
        currentStateLabel = lemurWindow.addChild(new Label(""));
        lemurWindow.addChild(new Label("Event log (seconds since this example started):"));
        eventLogLabel = lemurWindow.addChild(new Label(""));

        lemurWindow.addChild(new Button("Back to menu")).addClickCommands(source -> {
            getStateManager().detach(this);
            getStateManager().attach(new MenuExampleState());
        });

        lemurWindow.setLocalTranslation(-1.2f,2.2f,7);
        rootNodeDelegate.attachChild(lemurWindow);

        pauseDialog = new Container();
        pauseDialog.setLocalScale(0.007f);
        pauseDialog.addChild(new Label("Paused"));
        pauseDialogLabel = pauseDialog.addChild(new Label(""));
        pauseDialog.addChild(new Button("Continue")).addClickCommands(source -> {
            log("Pause dismissed");
            undismissedInterrupts.clear();
        });
        pauseDialog.setLocalTranslation(-0.6f,1.3f,8.5f);

        VRHandsAppState vrHands = getState(VRHandsAppState.ID, VRHandsAppState.class);
        vrHands.getHandControls().forEach(h -> {
            functionRegistrations.add(h.attachPickLine(pickLine()));
            functionRegistrations.add(h.setPickMarkerContinuous(rootNodeDelegate));
            functionRegistrations.add(h.setClickAction_lemurSupport(ActionHandles.TRIGGER, rootNodeDelegate));
        });

        xrAppState.movePlayersFeetToPosition(new Vector3f(0,0,10));
        xrAppState.playerLookAtPosition(new Vector3f(0,0,0));

        log("Started with profiles " + describeProfiles());
        updateLabels();
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        timeSinceStart += tpf;

        // this is the part a real game would need
        for(GameplayInterrupt interrupt : interruptSubscription.pollEvents()){
            log("INTERRUPT " + interrupt);
            String key = interrupt.getType() + interrupt.getHandSide().map(HandSide::toString).orElse("");
            if (interrupt.isStarted()){
                undismissedInterrupts.put(key, new InterruptStatus(interrupt, false));
            } else if (undismissedInterrupts.containsKey(key)){
                undismissedInterrupts.put(key, new InterruptStatus(interrupt, true));
            }
        }

        // just for the diagnostic log
        if (interactionProfileSubscription.checkHasChanged()){
            log("Interaction profiles changed: " + describeProfiles());
        }

        boolean paused = !undismissedInterrupts.isEmpty();
        if (paused && pauseDialog.getParent() == null){
            rootNodeDelegate.attachChild(pauseDialog);
        } else if (!paused && pauseDialog.getParent() != null){
            pauseDialog.removeFromParent();
        }

        if (!paused){
            secondsCounted += tpf;
        }

        updateLabels();
    }

    private void updateLabels(){
        boolean paused = !undismissedInterrupts.isEmpty();
        counterLabel.setText(String.format(Locale.ROOT, "%.1f", secondsCounted) + (paused ? " (paused)" : ""));

        currentStateLabel.setText("Current profiles: " + describeProfiles() + " (controllers must be missing for " + xrActionState.getControllerLostGracePeriod() + "s to count as lost)\n" +
                "Session state: " + xrAppState.getSessionState() + ", user present: " + xrAppState.isUserPresent());

        eventLogLabel.setText(String.join("\n", eventLog));

        if (paused){
            pauseDialogLabel.setText(undismissedInterrupts.values().stream()
                    .map(InterruptStatus::describe)
                    .collect(Collectors.joining("\n")));
        }
    }

    private String describeProfiles(){
        return Arrays.stream(HandSide.values())
                .map(hand -> hand + "=" + xrActionState.getCurrentInteractionProfile(hand).orElse("none"))
                .collect(Collectors.joining(", "));
    }

    private void log(String message){
        String line = String.format(Locale.ROOT, "%6.1f  %s", timeSinceStart, message);
        System.out.println("GameplayInterruptExample: " + line);
        eventLog.addLast(line);
        while (eventLog.size() > MAX_LOG_LINES){
            eventLog.removeFirst();
        }
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        functionRegistrations.forEach(FunctionRegistration::endFunction);
    }

    @Override protected void onEnable(){}

    @Override protected void onDisable(){}

    private static final class InterruptStatus{
        private final GameplayInterrupt interrupt;
        private final boolean ended;

        private InterruptStatus(GameplayInterrupt interrupt, boolean ended){
            this.interrupt = interrupt;
            this.ended = ended;
        }

        String describe(){
            String description;
            switch(interrupt.getType()){
                case SESSION_NOT_FOCUSED:
                    description = "System menu was opened";
                    break;
                case HEADSET_REMOVED:
                    description = "Headset was taken off";
                    break;
                case CONTROLLER_LOST:
                    description = interrupt.getHandSide().map(HandSide::toString).orElse("") + " controller lost (battery flat, turned off or put down?)";
                    break;
                default:
                    description = interrupt.getType().toString();
            }
            return description + (ended ? " - now resolved" : "");
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
