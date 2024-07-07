package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
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
import com.onemillionworlds.tamarin.actions.OpenXrActionState;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.event.MouseListener;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;


/**
 * This app state gives a basic menu, implemented in lemur that can be interacted with via either hand
 */
public class MenuExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("MenuExampleState");

    XrAppState xrAppState;
    OpenXrActionState openXrActionState;
    VRHandsAppState vrHands;

    /**
     * This is a list of active hand functions that should be autoremoved when this state is exited
     */
    List<FunctionRegistration> functionRegistrations = new ArrayList<>();

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        openXrActionState = getState(OpenXrActionState.ID, OpenXrActionState.class);
        vrHands = getState(VRHandsAppState.ID, VRHandsAppState.class);
        xrAppState = getState(XrAppState.ID, XrAppState.class);
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.005f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("Example application using Tamarin & Lemur");
        label.addMouseListener(new MouseListener(){
            @Override
            public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture){
                System.out.println("Tamarin supports mouse listener click events (but only the fact they happened, no other details)");
            }
            @Override public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture){}
            @Override public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture){}
            @Override public void mouseMoved(MouseMotionEvent event, Spatial target, Spatial capture){}
        });
        lemurWindow.addChild(label);

        lemurWindow.setLocalTranslation(-0.5f,1,7);

        rootNodeDelegate.attachChild(lemurWindow);

        lemurWindow.addChild(new Button("Block moving example")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new BlockMovingExampleState());
        });

        lemurWindow.addChild(new Button("Moving and teleporting example")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new MovingPlayerExampleState());
        });

        lemurWindow.addChild(new Button("Advanced Lemur example (inc keyboards)")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new AdvancedLemurTestState());
        });

        lemurWindow.addChild(new Button("Hand velocity example (throwing)")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new HandVelocityExampleState());
        });

        lemurWindow.addChild(new Button("Climbing example")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new ClimbingExampleState());
        });

        lemurWindow.addChild(new Button("Hand menu example")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new HandMenuExampleState());
        });

        lemurWindow.addChild(new Button("Advanced grabbing example (restricted to lines, boxes and parent motion)")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new RestrictedLineGrabMovement());
        });

        lemurWindow.addChild(new Button("Item given to hand on hand clench example")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new BlockCreateOnGrabExampleState());
        });

        lemurWindow.addChild(new Button("Precise grabbing Test")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new PreciseGrabExampleState());
        });

        lemurWindow.addChild(new Button("VR exit reenter Test")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new DetachAndReattachExampleState());
        });

        lemurWindow.addChild(new Button("VR sound Test")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new VrSoundExampleState());
        });

        lemurWindow.addChild(new Button("Change hands at runtime")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new ChangeHandsExampleState());
        });
        lemurWindow.addChild(new Button("Test that glow effects work in VR")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new GlowTestState());
        });
        lemurWindow.addChild(new Button("Additional (overlay) viewports")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new AdditionalViewportExampleState());
        });
        lemurWindow.addChild(new Button("Mechanical buttons")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new MechanicalButtonExample());
        });

        lemurWindow.addChild(new Button("Exit")).addClickCommands(source ->
            getApplication().stop()
        );

        //get the left hand and add a pick line to it
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
