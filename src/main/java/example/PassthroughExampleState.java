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
 * Demonstrates turning passthrough (seeing the real world behind the virtual scene, aka mixed reality) on and off at
 * runtime. This requires {@link com.onemillionworlds.tamarin.openxr.XrSettings#setPassthroughSupport(boolean)} to have
 * been set at start up.
 * <p>
 *     It also reports how passthrough is being provided (the standard ALPHA_BLEND environment blend mode or Meta's
 *     XR_FB_passthrough extension) and which environment blend modes the runtime supports.
 * </p>
 */
public class PassthroughExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("PassthroughExampleState");

    XrBaseAppState xrAppState;

    Label statusLabel;

    List<FunctionRegistration> functionRegistrations = new ArrayList<>();

    float time = 0;
    Geometry spinningBox;

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        xrAppState = getState(XrBaseAppState.ID, XrBaseAppState.class);

        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.005f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        lemurWindow.addChild(new Label("Passthrough (mixed reality). When it is on you should see the real world\n" +
                "behind this window and the boxes. There is deliberately no floor or sky in this example."));
        statusLabel = lemurWindow.addChild(new Label(""));

        Button toggleButton = lemurWindow.addChild(new Button("Toggle passthrough"));
        toggleButton.addClickCommands(source -> {
            if (xrAppState.isPassthroughAvailable()){
                xrAppState.setPassthroughEnabled(!xrAppState.isPassthroughEnabled());
            }
        });

        lemurWindow.addChild(new Button("Back to menu")).addClickCommands(source -> {
            getStateManager().detach(this);
            getStateManager().attach(new MenuExampleState());
        });

        lemurWindow.setLocalTranslation(-0.6f,1.6f,8.5f);
        rootNodeDelegate.attachChild(lemurWindow);

        rootNodeDelegate.attachChild(box(new Vector3f(-0.4f, 1f, 8.6f), ColorRGBA.Red));
        rootNodeDelegate.attachChild(box(new Vector3f(0.4f, 1f, 8.6f), ColorRGBA.Blue));
        spinningBox = box(new Vector3f(0f, 0.8f, 8.8f), ColorRGBA.Green);
        rootNodeDelegate.attachChild(spinningBox);

        VRHandsAppState vrHands = getState(VRHandsAppState.ID, VRHandsAppState.class);
        vrHands.getHandControls().forEach(h -> {
            functionRegistrations.add(h.attachPickLine(pickLine()));
            functionRegistrations.add(h.setPickMarkerContinuous(rootNodeDelegate));
            functionRegistrations.add(h.setClickAction_lemurSupport(ActionHandles.TRIGGER, rootNodeDelegate));
        });

        xrAppState.movePlayersFeetToPosition(new Vector3f(0,0,10));
        xrAppState.playerLookAtPosition(new Vector3f(0,0,0));
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        time += tpf;
        spinningBox.setLocalRotation(new Quaternion().fromAngleAxis(time, Vector3f.UNIT_Y));

        String availability = xrAppState.isPassthroughAvailable()
                ? "Passthrough available via " + xrAppState.getPassthroughMethod() + ", currently " + (xrAppState.isPassthroughEnabled() ? "ON" : "OFF")
                : "Passthrough NOT available: " + xrAppState.getPassthroughUnavailableReason();
        statusLabel.setText(availability + "\n" +
                "Runtime supports environment blend modes: " + xrAppState.getSupportedXrVrModes() + "\n" +
                "XR_FB_passthrough extension loaded: " + xrAppState.checkExtensionLoaded("XR_FB_passthrough"));
    }

    @Override
    protected void cleanup(Application app){
        // leave the rest of the testbed in VR
        if (xrAppState.isPassthroughEnabled()){
            xrAppState.setPassthroughEnabled(false);
        }
        rootNodeDelegate.removeFromParent();
        functionRegistrations.forEach(FunctionRegistration::endFunction);
    }

    @Override protected void onEnable(){}

    @Override protected void onDisable(){}

    private Geometry box(Vector3f location, ColorRGBA colour){
        Geometry geometry = new Geometry("box", new Box(0.1f, 0.1f, 0.1f));
        Material material = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", colour);
        geometry.setMaterial(material);
        geometry.setLocalTranslation(location);
        return geometry;
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
