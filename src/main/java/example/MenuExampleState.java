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
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.event.MouseListener;


/**
 * This app state gives a basic menu, implemented in lemur that can be interacted with via either hand
 */
public class MenuExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("MenuExampleState");
    ActionBasedOpenVrState openVr;
    VRHandsAppState vrHands;

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        openVr = getState(ActionBasedOpenVrState.class);
        vrHands = getState(VRHandsAppState.class);

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

        lemurWindow.addChild(new Button("Start block moving example")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new BlockMovingExampleState());
        });

        lemurWindow.addChild(new Button("Start moving and teleporting example")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new MovingPlayerExampleState());
        });

        lemurWindow.addChild(new Button("Start advanced Lemur example (inc keyboards)")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new AdvancedLemurTestState());
        });

        lemurWindow.addChild(new Button("Start hand velocity example (throwing)")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new HandVelocityExampleState());
        });

        lemurWindow.addChild(new Button("Start climbing example")).addClickCommands(source -> {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new ClimbingExampleState());
        });

        lemurWindow.addChild(new Button("Exit")).addClickCommands(source -> {
            getApplication().stop();
        });

        //get the left hand and add a pick line to it
        vrHands.getHandControls().forEach(h -> {
            h.attachPickLine(pickLine());
            h.setPickMarkerContinuous(rootNodeDelegate);
            h.setClickAction_lemurSupport("/actions/main/in/trigger", rootNodeDelegate);
        });
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        vrHands.getHandControls().forEach(boundHand -> {
            boundHand.clearPickMarkerContinuous();
            boundHand.removePickLine();
            boundHand.clearClickAction_lemurSupport();
        });
    }

    @Override protected void onEnable(){}

    @Override protected void onDisable(){}

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
