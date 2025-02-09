package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.actions.XrActionAppState;
import com.onemillionworlds.tamarin.actions.state.Vector2fActionState;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import example.actions.ActionHandles;

/**
 * This example has the whole player as an active physics object. This can be a little
 * unkind on the human player but some game styles require it
 */
public class WorldPushbackPhysicsExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("BlockMovingExampleState");
    XrAppState vrAppState;
    XrActionAppState openXrActionState;
    VRHandsAppState vrHands;

    //the fact the capsule cannot have its height updated is a problem. The height should be constantly updated to be the human players current camera height relative to observer
    float playerRequestedHeight = 1.0f;
    float capsuleRadius = 0.30f;

    CapsuleCollisionShape capsule = new CapsuleCollisionShape(capsuleRadius, playerRequestedHeight);
    CharacterControl playerControl = new CharacterControl(capsule,0.3f);

    float playersTrueCapsuleHeight = playerRequestedHeight + 2 * capsuleRadius; //the shape (insanely) is not the height actually asked for, thats just the cylindrical portion height

    Vector3f lastTickPhysicsFeetPosition;

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        vrAppState = getState(XrAppState.ID, XrAppState.class);
        openXrActionState = getState(XrActionAppState.ID, XrActionAppState.class);
        vrHands = getState(VRHandsAppState.class);

        initialiseScene();

    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    private void initialiseScene(){
        BulletAppState appState = new BulletAppState();

        getStateManager().attach(appState);
        PhysicsSpace physicsSpace = appState.getPhysicsSpace();
        physicsSpace.setMaxTimeStep(1f/90);

        rootNodeDelegate.attachChild(checkerboardFloor(getApplication().getAssetManager(), physicsSpace));

        //grabbableBox(new Vector3f(0,1f, 9.5f), physicsSpace);
        //grabbableBox(new Vector3f(0.2f,1.2f, 9.5f), physicsSpace);
        //grabbableBox(new Vector3f(-0.2f,0.9f, 9.5f), physicsSpace);
        //grabbableBox(new Vector3f(0.3f,1.1f, 9.6f), physicsSpace);

        wall(new Vector3f(-1, 5, 10), new Vector3f(0.1f, 5, 0.5f), physicsSpace);

        Node characterNode = new Node("character node");
        rootNodeDelegate.attachChild(characterNode);
        characterNode.addControl(playerControl);

        physicsSpace.add(characterNode);
        playerControl.setPhysicsLocation(getPlayerFeetPosition().addLocal(0, playersTrueCapsuleHeight/2,0));
        lastTickPhysicsFeetPosition = playerControl.getPhysicsLocation().subtract(0, playersTrueCapsuleHeight/2,0);

        //add some stairs to walk up
        step(new Vector3f(-1,0, 5), new Vector3f(1,0.2f, 8), ColorRGBA.Red, physicsSpace);
        step(new Vector3f(-1,0.2f, 5), new Vector3f(1,0.4f, 7.5f), ColorRGBA.Blue, physicsSpace);
        step(new Vector3f(-1,0.4f, 5), new Vector3f(1,0.6f, 7), ColorRGBA.Green, physicsSpace);
        step(new Vector3f(-1,0.6f, 5), new Vector3f(1,0.8f, 6.5f), ColorRGBA.Red, physicsSpace);
        step(new Vector3f(-1,0.8f, 5), new Vector3f(1,1f, 6.0f), ColorRGBA.Blue, physicsSpace);
        step(new Vector3f(-1,1f, 5), new Vector3f(1,1.2f, 5.5f), ColorRGBA.Green, physicsSpace);

        getObserver().attachChild(debugBox(ColorRGBA.Red));

    }

    @Override
    public void update(float tpf){
        super.update(tpf);

        Vector3f physicsLocation = playerControl.getPhysicsLocation();
        Vector3f physicsFeetLocation = physicsLocation.subtract(0,playersTrueCapsuleHeight/2,0);
        Vector3f vrFeetPosition = getPlayerFeetPosition();

        moveViaControls(tpf);

        //the engine only moves the body if something went wrong(e.g. collisions, falling), this should lead to observer slip to correct observer to physics (rather than the normal other way round)
        Vector3f currentPhysicsChange = physicsFeetLocation.subtract(lastTickPhysicsFeetPosition);
        getObserver().setLocalTranslation(getObserver().getLocalTranslation().add(currentPhysicsChange));
        vrFeetPosition.addLocal(currentPhysicsChange); //need to let the feet know they have been updated or else the normal correction tries to "uncorrect" it

        //this is the normal behaviour; physics follows the VR headset
        //where the vrFeetPosition and physicsFeetLocation are not aligned and this isn't due to a physics correction move the physics object to align them
        playerControl.setPhysicsLocation(vrFeetPosition.add(0,playersTrueCapsuleHeight/2, 0));
        physicsFeetLocation =  playerControl.getPhysicsLocation().subtract(0,playersTrueCapsuleHeight/2,0);
        lastTickPhysicsFeetPosition = physicsFeetLocation; //record where physics should still be next tick
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
        }
        getObserver().setLocalTranslation(getObserver().getWorldTranslation().add(playerRelativeWalkDirection.mult(2f*timeslice)));
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
     * @param location
     * @param physicsSpace
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

    /**
     * The players feet are at the height of the observer, but the x,z of the cameras
     * @return
     */
    private Vector3f getPlayerFeetPosition(){
        return vrAppState.getPlayerFeetPosition();
    }

    private Node getObserver(){
        return vrAppState.getObserver();
    }

    private Geometry debugBox(ColorRGBA colorRGBA){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colorRGBA);
        boxGeometry.setMaterial(boxMat);
        return boxGeometry;
    }

}