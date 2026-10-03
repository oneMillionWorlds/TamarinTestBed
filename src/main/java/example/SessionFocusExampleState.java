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
import com.onemillionworlds.tamarin.observable.ObservableValueSubscription;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Demonstrates pausing the game when the user stops interacting with it, i.e. they open the system menu (the session
 * loses focus) or they take the headset off (the user is no longer present).
 * <p>
 *     A counter counts up while the game should be running and stops counting while it should be paused. This is the
 *     pattern a game would use to pause its game logic. Note that the JME application as a whole is NOT paused (the
 *     OpenXR frame loop must keep running), only the game logic (in this case the counter) is.
 * </p>
 */
public class SessionFocusExampleState extends BaseAppState{

    private static final String USER_PRESENCE_EXTENSION = "XR_EXT_user_presence";

    Node rootNodeDelegate = new Node("SessionFocusExampleState");

    XrBaseAppState xrAppState;

    ObservableValueSubscription<Boolean> shouldPauseSubscription;
    ObservableValueSubscription<Boolean> focusSubscription;
    ObservableValueSubscription<Boolean> userPresentSubscription;

    float secondsCounted = 0;

    int timesFocusLost = 0;
    int timesHeadsetRemoved = 0;

    Label counterLabel;
    Label statusLabel;

    List<FunctionRegistration> functionRegistrations = new ArrayList<>();

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);

        xrAppState = getState(XrBaseAppState.ID, XrBaseAppState.class);
        shouldPauseSubscription = xrAppState.subscribeToShouldPause();
        // the below 2 are just for displaying the details, a real game would just need shouldPauseSubscription
        focusSubscription = xrAppState.subscribeToSessionFocused();
        userPresentSubscription = xrAppState.subscribeToUserPresent();

        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.005f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        lemurWindow.addChild(new Label("The counter counts up while you are using the application.\n\n" +
                "Open the system menu or take off the headset and the counter\n" +
                "should stop. Close the menu or put the headset back on and it should resume."));
        counterLabel = lemurWindow.addChild(new Label(""));
        counterLabel.setFontSize(40);
        statusLabel = lemurWindow.addChild(new Label(""));

        lemurWindow.addChild(new Button("Back to menu")).addClickCommands(source -> {
            getStateManager().detach(this);
            getStateManager().attach(new MenuExampleState());
        });

        lemurWindow.setLocalTranslation(-0.5f,1.5f,7);
        rootNodeDelegate.attachChild(lemurWindow);

        VRHandsAppState vrHands = getState(VRHandsAppState.ID, VRHandsAppState.class);
        vrHands.getHandControls().forEach(h -> {
            functionRegistrations.add(h.attachPickLine(pickLine()));
            functionRegistrations.add(h.setPickMarkerContinuous(rootNodeDelegate));
            functionRegistrations.add(h.setClickAction_lemurSupport(ActionHandles.TRIGGER, rootNodeDelegate));
        });

        xrAppState.movePlayersFeetToPosition(new Vector3f(0,0,10));
        xrAppState.playerLookAtPosition(new Vector3f(0,0,0));

        updateLabels();
    }

    @Override
    public void update(float tpf){
        super.update(tpf);

        // In a real game this is where game logic would be paused/resumed (e.g. by disabling its game app states).
        // Here the counter just checks shouldPauseSubscription.get() each frame, checkHasChanged() is used for logging
        if (shouldPauseSubscription.checkHasChanged()){
            System.out.println("Should pause changed to " + shouldPauseSubscription.get());
        }
        if (focusSubscription.checkHasChanged() && !focusSubscription.get()){
            timesFocusLost++;
        }
        if (userPresentSubscription.checkHasChanged() && !userPresentSubscription.get()){
            timesHeadsetRemoved++;
        }

        if (!shouldPauseSubscription.get()){
            secondsCounted += tpf;
        }

        updateLabels();
    }

    private void updateLabels(){
        counterLabel.setText(String.format(Locale.ROOT, "%.1f", secondsCounted) + (shouldPauseSubscription.get() ? " (paused)" : ""));
        String presenceSupport = xrAppState.checkExtensionLoaded(USER_PRESENCE_EXTENSION) ? "" : " (" + USER_PRESENCE_EXTENSION + " not available, always reports present)";
        statusLabel.setText(
                "Session state: " + xrAppState.getSessionState() + ", focus lost " + timesFocusLost + " time(s)\n" +
                "User present: " + userPresentSubscription.get() + presenceSupport + ", headset removed " + timesHeadsetRemoved + " time(s)");
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        functionRegistrations.forEach(FunctionRegistration::endFunction);
    }

    @Override protected void onEnable(){}

    @Override protected void onDisable(){}

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
