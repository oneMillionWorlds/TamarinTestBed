package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.OpenXrActionState;
import com.onemillionworlds.tamarin.actions.compatibility.SyntheticDPad;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.debug.TamarinDebugOverlayState;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.viewports.AdditionalViewportRequest;
import com.onemillionworlds.tamarin.viewports.ViewportConfigurator;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.style.ElementId;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;

public class AdditionalViewportExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("AdditionalViewportExampleState");

    Node additionalRootNode = new Node("overlayRootNode");

    XrAppState xrAppState;
    OpenXrActionState openXrActionState;
    VRHandsAppState vrHands;

    SyntheticDPad movementDpad = new SyntheticDPad();

    ViewportConfigurator viewportConfigurator;

    List<FunctionRegistration> functionsToCloseOnExit = new ArrayList<>();

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        xrAppState = getState(XrAppState.ID, XrAppState.class);
        openXrActionState = getState(OpenXrActionState.ID, OpenXrActionState.class);
        vrHands = getState(VRHandsAppState.ID, VRHandsAppState.class);
        initialiseScene();

        viewportConfigurator = xrAppState.addAdditionalViewport(AdditionalViewportRequest.builder(additionalRootNode).build());

        vrHands.getHandControls().forEach(boundHand -> {
            //tamarin doesn't care that the picking is being done against overlay nodes, hand interaction is the same :)
            functionsToCloseOnExit.add(boundHand.setClickAction_lemurSupport(ActionHandles.TRIGGER, additionalRootNode));

            //to get the picking lines in the overlay viewport is a bit involved, because the hand is in the normal viewport
            //we set it to track the hands attachment node rather than attaching directly to it
            Spatial pickLine = pickLine();
            pickLine.addControl(new TrackOtherNodeControl(boundHand.getHandNode_xPointing()));
            additionalRootNode.attachChild(pickLine);
        });

        getStateManager().attach(new TamarinDebugOverlayState());
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        viewportConfigurator.removeViewports();
        getStateManager().detach(getState(TamarinDebugOverlayState.ID, TamarinDebugOverlayState.class));
        functionsToCloseOnExit.forEach(FunctionRegistration::endFunction);
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    @SuppressWarnings("DuplicatedCode") //as the examples are supposed to be self-contained accept some duplication
    @Override
    public void update(float tpf){

        super.update(tpf);

        /*
         * this is a temporary workaround until the XR_EXT_dpad_binding extension is better supported and we can use true dpads
         */
        movementDpad.updateRawAction(openXrActionState.getVector2fActionState(ActionHandles.MOVEMENT_DPAD));

        BooleanActionState teleportAction = movementDpad.north();
        // really we should be more understanding of the action being redefined to a different hand, but that is painful
        // while using the synthetic dpad. So assume left hand.
        vrHands.getHandControl(HandSide.LEFT).ifPresent(boundHand -> {
            if (teleportAction.hasChanged() && teleportAction.getState()){
                //teleport in the direction the hand that requested it is pointing
                Vector3f pointingDirection = boundHand.getBulkPointingDirection();
                pointingDirection.y=0;

                xrAppState.movePlayersFeetToPosition(xrAppState.getPlayerFeetPosition().add(pointingDirection.mult(2)));
            }
        });

        additionalRootNode.updateLogicalState(tpf);
        additionalRootNode.updateGeometricState();

    }

    private void initialiseScene(){
        rootNodeDelegate.attachChild(BlockMovingExampleState.checkerboardFloor(getApplication().getAssetManager()));

        //add some pillars just to add visual references
        pillarExtraViewport(-0.5f,10f, ColorRGBA.Red);
        pillarExtraViewport(-0.5f,11f, ColorRGBA.Black);
        pillarExtraViewport(0.5f,10f, ColorRGBA.Yellow);
        pillarExtraViewport(0.5f,11f, ColorRGBA.Blue);

        largeCube(0, 10.5f, ColorRGBA.Pink);

        xrAppState.movePlayersFaceToPosition(new Vector3f(0,0,15));
        xrAppState.playerLookAtPosition(new Vector3f(0,0,10));

        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.02f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("This example shows an overlay viewport. The pink box is in the main root node (IT IS NOT TRANSPARENT).\n\nThe other boxes are in a separate overlay viewport that is rendered over the top of the main viewport\n\nUseful for debug shapes, or maybe seeing through wall effects.");
        lemurWindow.addChild(label);
        lemurWindow.setLocalTranslation(-5,4,0);
        rootNodeDelegate.attachChild(lemurWindow);

        Container overlayUi = new Container(new ElementId("solidbackground"));
        SpringGridLayout overlayLayout = new SpringGridLayout();
        overlayUi.setLayout(overlayLayout);
        additionalRootNode.attachChild(overlayUi);

        overlayUi.setLocalScale(0.005f);
        Label labelOverlay = new Label("This UI is attached to the overlay node. \nPerhaps its a main menu that opens while the game is paused. \nIt has been drawn 'over' the main world");
        Button exitButton = new Button("Click this overlay button to exit");
        exitButton.addClickCommands(command -> {
            getStateManager().detach(this);
            getStateManager().attach(new MenuExampleState());
        });
        overlayUi.addChild(labelOverlay);
        overlayUi.addChild(exitButton);
        overlayUi.setLocalTranslation(0,1,12);
        overlayUi.lookAt(new Vector3f(0,1,15), Vector3f.UNIT_Y);

    }

    private void pillarExtraViewport(float x, float z, ColorRGBA colorRGBA){
        float pillarHeight = 0.5f;
        Box pillar = new Box(0.25f, pillarHeight/2,  0.25f);
        Geometry pillarGeometry = new Geometry("pillar", pillar);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colorRGBA);
        boxMat.getAdditionalRenderState().setWireframe(true);
        pillarGeometry.setMaterial(boxMat);
        pillarGeometry.setLocalTranslation(new Vector3f(x, pillarHeight/2, z));
        additionalRootNode.attachChild(pillarGeometry);
    }

    private void largeCube(float x, float z, ColorRGBA colorRGBA){
        float pillarHeight = 0.9f;
        Box pillar = new Box(2f, pillarHeight/2,  2f);
        Geometry pillarGeometry = new Geometry("pillar", pillar);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Light/Lighting.j3md");
        boxMat.setColor("Diffuse", colorRGBA);
        pillarGeometry.setMaterial(boxMat);
        pillarGeometry.setLocalTranslation(new Vector3f(x, pillarHeight/2, z));
        rootNodeDelegate.attachChild(pillarGeometry);
    }

    @SuppressWarnings("DuplicatedCode") //as the examples are supposed to be self-contained accept some duplication
    private Spatial pickLine(){
        float length = 4;

        Cylinder pickCylinder = new Cylinder(10,10, 0.002f, length, true);

        Geometry geometry = new Geometry("debugHandLine", pickCylinder);
        Material material = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", ColorRGBA.White);
        geometry.setMaterial(material);

        Quaternion rotate = new Quaternion();
        rotate.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        geometry.setLocalRotation(rotate);
        geometry.setLocalTranslation(length/2, 0,0);

        Node node = new Node();
        node.attachChild(geometry);
        return node;
    }

    public static class TrackOtherNodeControl extends AbstractControl{

        Spatial spatialToFollow;

        public TrackOtherNodeControl(Spatial spatialToFollow){
            this.spatialToFollow = spatialToFollow;
        }

        @Override
        protected void controlUpdate(float tpf){
            getSpatial().setLocalTransform(spatialToFollow.getWorldTransform());
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp){

        }
    }

}
