package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.actions.XrActionAppState;
import com.onemillionworlds.tamarin.actions.state.Vector2fActionState;
import com.onemillionworlds.tamarin.miniesupport.KinematicHandPhysics;
import com.onemillionworlds.tamarin.miniesupport.PhysicsDebugVrAppState;
import com.onemillionworlds.tamarin.miniesupport.PlayerVrPhysicsAppState;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;

/**
 * This example uses some of the starter physics interactions provided by Tamarin.
 * A real game will probably want to fork those examples to get tighter control on
 * physics (which can vary considerably between games)
 */
public class PhysicsExampleState extends BaseAppState{
    private static final float PLAYER_WALK_SPEED = 2;

    private final Node rootNodeDelegate = new Node("BlockMovingExampleState");
    private XrAppState vrAppState;
    private XrActionAppState openXrActionState;

    private BulletAppState bulletAppState;

    private final List<FunctionRegistration> functionRegistrations = new ArrayList<>();

    private final PhysicsDebugVrAppState physicsDebugVrAppState = new PhysicsDebugVrAppState();

    private final PlayerVrPhysicsAppState playerVrPhysicsAppState = new PlayerVrPhysicsAppState();

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        vrAppState = getState(XrAppState.ID, XrAppState.class);
        openXrActionState = getState(XrActionAppState.ID, XrActionAppState.class);
        VRHandsAppState vrHands = getState(VRHandsAppState.class);


        bulletAppState = new BulletAppState();
        getStateManager().attach(bulletAppState);
        getStateManager().attach(physicsDebugVrAppState);
        getStateManager().attach(playerVrPhysicsAppState);
        initialiseScene();

        vrHands.getHandControls().forEach(hand
                -> functionRegistrations.add(hand.addFunction(new KinematicHandPhysics(bulletAppState.getPhysicsSpace()))));

    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        functionRegistrations.forEach(FunctionRegistration::endFunction);
        getStateManager().detach(physicsDebugVrAppState);
        getStateManager().detach(playerVrPhysicsAppState);
        getStateManager().detach(bulletAppState);
    }

    @Override
    protected void onEnable(){}

    @Override
    protected void onDisable(){}

    private void initialiseScene(){

        PhysicsSpace physicsSpace = bulletAppState.getPhysicsSpace();

        physicsSpace.setMaxTimeStep(1f/90);

        rootNodeDelegate.attachChild(checkerboardFloor(getApplication().getAssetManager(), physicsSpace));

        table(new Vector3f(2, 0.5f, 10), new Vector3f(0.25f, 0.1f, 0.25f), physicsSpace);

        grabbableBox(new Vector3f(2, 0.6f, 10), physicsSpace);
        grabbableBox(new Vector3f(2, 0.7f, 10), physicsSpace);
        grabbableBox(new Vector3f(2, 0.6f, 10.1f), physicsSpace);
        grabbableBox(new Vector3f(2.1f, 0.6f, 10), physicsSpace);

        wall(new Vector3f(-1, 5, 10), new Vector3f(0.1f, 5, 0.5f), physicsSpace);

        //add some stairs to walk up
        step(new Vector3f(-1,0, 5), new Vector3f(1,0.2f, 8), ColorRGBA.Red, physicsSpace);
        step(new Vector3f(-1,0.2f, 5), new Vector3f(1,0.4f, 7.5f), ColorRGBA.Blue, physicsSpace);
        step(new Vector3f(-1,0.4f, 5), new Vector3f(1,0.6f, 7), ColorRGBA.Green, physicsSpace);
        step(new Vector3f(-1,0.6f, 5), new Vector3f(1,0.8f, 6.5f), ColorRGBA.Red, physicsSpace);
        step(new Vector3f(-1,0.8f, 5), new Vector3f(1,1f, 6.0f), ColorRGBA.Blue, physicsSpace);
        step(new Vector3f(-1,1f, 5), new Vector3f(1,1.2f, 5.5f), ColorRGBA.Green, physicsSpace);

    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        moveViaControls(tpf);
    }

    public void moveViaControls(float timeslice){

        Vector2fActionState analogActionState = openXrActionState.getVector2fActionState(ActionHandles.WALK);
        //we'll want the joystick to move the player relative to the head face direction, not the hand pointing direction
        Vector3f walkingDirectionRaw = new Vector3f(-analogActionState.getX(), 0, analogActionState.getY());

        Quaternion lookDirection = new Quaternion().lookAt(vrAppState.getVrCameraLookDirection(), Vector3f.UNIT_Y);

        Vector3f playerRelativeWalkDirection = lookDirection.mult(walkingDirectionRaw);
        playerRelativeWalkDirection.y = 0;
        if (playerRelativeWalkDirection.length()>0.01){
            playerRelativeWalkDirection.normalizeLocal();

            playerVrPhysicsAppState
                    .moveByWalking(
                            new Vector2f(playerRelativeWalkDirection.x, playerRelativeWalkDirection.z)
                            .mult(timeslice*PLAYER_WALK_SPEED)
                    );
        }
    }

    public static Geometry checkerboardFloor(AssetManager assetManager, PhysicsSpace physicsSpace){
        Quad floorQuad = new Quad(10,10);
        Geometry floor = new Geometry("floor", floorQuad);
        Texture floorTexture = assetManager.loadTexture("Textures/checkerBoard.png");
        floorTexture.setMagFilter(Texture.MagFilter.Nearest);
        Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", floorTexture);

        floor.setMaterial(mat);
        Quaternion floorRotate = new Quaternion();
        floorRotate.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
        floor.setLocalRotation(floorRotate);
        floor.setLocalTranslation(-5,0,15);

        RigidBodyControl rigidBodyControl = new RigidBodyControl(0);
        floor.addControl(rigidBodyControl);

        physicsSpace.addCollisionObject(rigidBodyControl);
        return floor;
    }


    /**
     * TODO; need hand physics and ability to pick up box
     */
    private void grabbableBox(Vector3f location, PhysicsSpace physicsSpace){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", ColorRGBA.randomColor());
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(location);
        //AutoMovingGrabControl grabControl = new AutoMovingGrabControl(new Vector3f(0.025f,0,0), 0.05f);
        //boxGeometry.addControl(grabControl);
        rootNodeDelegate.attachChild(boxGeometry);

        RigidBodyControl rigidBodyControl = new RigidBodyControl(1);
        boxGeometry.addControl(rigidBodyControl);
        physicsSpace.addCollisionObject(rigidBodyControl);
    }

    private void wall(Vector3f locationCentre, Vector3f size, PhysicsSpace physicsSpace){
        Box box = new Box(size.x, size.y, size.z);
        Geometry boxGeometry = new Geometry("wall", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setTexture("ColorMap", getApplication().getAssetManager().loadTexture("Textures/backboard.png"));
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(locationCentre);
        RigidBodyControl rigidBodyControl = new RigidBodyControl(0);
        boxGeometry.addControl(rigidBodyControl);
        physicsSpace.addCollisionObject(rigidBodyControl);
        rootNodeDelegate.attachChild(boxGeometry);
    }

    private void table(Vector3f locationCentre, Vector3f size, PhysicsSpace physicsSpace){
        Box box = new Box(size.x, size.y, size.z);
        Geometry boxGeometry = new Geometry("wall", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setTexture("ColorMap", getApplication().getAssetManager().loadTexture("Textures/backboard.png"));
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(locationCentre);
        RigidBodyControl rigidBodyControl = new RigidBodyControl(0);
        boxGeometry.addControl(rigidBodyControl);
        physicsSpace.addCollisionObject(rigidBodyControl);
        rootNodeDelegate.attachChild(boxGeometry);
    }

    private void step(Vector3f min, Vector3f max, ColorRGBA colour, PhysicsSpace physicsSpace){
        Vector3f size = max.subtract(min);
        Box box = new Box(size.x/2, size.y/2, size.z/2);
        Geometry boxGeometry = new Geometry("physicsBox", box);
        boxGeometry.setLocalTranslation(min.add(max).multLocal(0.5f));
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        boxMat.setTexture("ColorMap", getApplication().getAssetManager().loadTexture("Textures/backboard.png"));

        boxGeometry.setMaterial(boxMat);

        RigidBodyControl rigidBodyControl = new RigidBodyControl(0);
        boxGeometry.addControl(rigidBodyControl);
        physicsSpace.addCollisionObject(rigidBodyControl);

        rootNodeDelegate.attachChild(boxGeometry);
    }


}