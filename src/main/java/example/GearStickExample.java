package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.math.Line3f;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.onemillionworlds.tamarin.vrhands.grabbing.GrabEventControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.RelativeMovingGrabControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.GrabMoveRestriction;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;
import com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints.SnapToPoint;
import com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints.SnapToPoints;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;

public class GearStickExample extends BaseAppState{

    Node rootNodeDelegate = new Node("BlockMovingExampleState");

    List<FunctionRegistration> closeHandBindings = new ArrayList<>();

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);

        getState(VRHandsAppState.ID, VRHandsAppState.class).getHandControls().forEach(boundHand ->
                closeHandBindings.add(boundHand.setGrabAction(ActionHandles.GRIP, rootNodeDelegate)));
        initialiseScene();
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        closeHandBindings.forEach(FunctionRegistration::endFunction);
        closeHandBindings.clear();
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    private void initialiseScene(){
        rootNodeDelegate.attachChild(checkerboardFloor(getApplication().getAssetManager()));

        gearStick(new Vector3f(0,1f, 9.5f));

        exitBox(new Vector3f(-0.5f,1f, 10f));

        //a lemur UI with text explaining what to do
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.02f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("This example shows both advanced grab move restriction and snap-to points using a gear stick as an example");
        lemurWindow.addChild(label);
        lemurWindow.setLocalTranslation(-5,4,0);
        rootNodeDelegate.attachChild(lemurWindow);
    }

    private void gearStick(Vector3f centrePosition){
        Node mainNode = new Node("GearStick");
        mainNode.setLocalTranslation(centrePosition);

        Vector3f first = new Vector3f(-0.2f, 0, 0.2f);
        Vector3f second = new Vector3f(-0.2f, 0, -0.2f);
        Vector3f third = new Vector3f(0f, 0, 0.2f);
        Vector3f fourth = new Vector3f(0f, 0, -0.2f);
        Vector3f fifth = new Vector3f(0.2f, 0, 0.2f);
        Vector3f sixth = new Vector3f(0.2f, 0, -0.2f);

        Vector3f centreStart = first.add(second).multLocal(0.5f);
        Vector3f centreEnd = fifth.add(sixth).multLocal(0.5f);

        mainNode.attachChild(thickLine(first, second, 0.01f, ColorRGBA.Blue));
        mainNode.attachChild(thickLine(third, fourth, 0.01f, ColorRGBA.Pink));
        mainNode.attachChild(thickLine(fifth, sixth, 0.01f, ColorRGBA.Cyan));
        mainNode.attachChild(thickLine(centreStart, centreEnd, 0.01f, ColorRGBA.Gray));

        box(ColorRGBA.Black, 0.02f, first);
        box(ColorRGBA.Black, 0.02f, second);
        box(ColorRGBA.Black, 0.02f, third);
        box(ColorRGBA.Black, 0.02f, fourth);
        box(ColorRGBA.Black, 0.02f, fifth);
        box(ColorRGBA.Black, 0.02f, sixth);

        Geometry gearStick = sphere(ColorRGBA.Red, 0.03f);
        gearStick.setLocalTranslation(first);

        float snapToDistance = 0.03f;

        RelativeMovingGrabControl grabControl = new RelativeMovingGrabControl();
        grabControl.setSnapToPoints(new SnapToPoints(false, List.of(
                new SnapToPoint(first, snapToDistance),
                new SnapToPoint(second, snapToDistance),
                new SnapToPoint(third, snapToDistance),
                new SnapToPoint(fourth, snapToDistance),
                new SnapToPoint(fifth, snapToDistance),
                new SnapToPoint(sixth, snapToDistance))));

        grabControl.setGrabMoveRestriction(new GrabMoveRestriction(){
            final List<Line3f> linesToSnapTo = List.of(
                    new Line3f(first, second),
                    new Line3f(third, fourth),
                    new Line3f(fifth, sixth),
                    new Line3f(centreStart, centreEnd));

            @Override
            public Vector3f restrictPosition(Vector3f naturalPositionLocal, RestrictionUtilities restrictionUtilities){
                //restrict to a complex shape made of lines (the gear stick pattern). Restricts to the closest line
                return linesToSnapTo.stream().min((line1, line2) -> {
                    float distance1 = line1.findDistanceLineToPoint(naturalPositionLocal);
                    float distance2 = line2.findDistanceLineToPoint(naturalPositionLocal);
                    return Float.compare(distance1, distance2);
                }).map(line -> line.findPointOfClosedApproach(naturalPositionLocal)).orElseThrow();
            }
        });
        gearStick.addControl(grabControl);

        mainNode.attachChild(gearStick);

        rootNodeDelegate.attachChild(mainNode);
    }

    @SuppressWarnings("DuplicatedCode") //each example is supposed to be mostly stand along so allow some duplication
    public static Geometry checkerboardFloor(AssetManager assetManager){
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

        return floor;
    }

    @SuppressWarnings("DuplicatedCode") //each example is supposed to be mostly stand along so allow some duplication
    private void exitBox(Vector3f location){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Texture exitTexture = getApplication().getAssetManager().loadTexture("Textures/grabToExit.png");
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setTexture("ColorMap", exitTexture);
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(location);
        GrabEventControl grabControl = new GrabEventControl(() -> {
            getStateManager().detach(this);
            getStateManager().attach(new MenuExampleState());
        });
        boxGeometry.addControl(grabControl);
        rootNodeDelegate.attachChild(boxGeometry);
    }


    private Geometry box(ColorRGBA colour, float halfSize, Vector3f localTranslation){
        Box mainBox = new Box(halfSize, halfSize, halfSize);
        Geometry boxGeometry = new Geometry("box", mainBox);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(localTranslation);
        return boxGeometry;
    }

    private Geometry sphere(ColorRGBA colour, float halfSize){
        Sphere sphere = new Sphere(36, 36, halfSize);
        Geometry boxGeometry = new Geometry("parent", sphere);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        boxGeometry.setMaterial(boxMat);
        return boxGeometry;
    }

    private Geometry thickLine(Vector3f localMin, Vector3f localMax, float thickness, ColorRGBA colour){
        // Calculate the direction vector from localMin to localMax
        Vector3f direction = localMax.subtract(localMin);
        float length = direction.length();

        // Create a cylinder with the specified thickness and length
        Cylinder cylinder = new Cylinder(10, 10, thickness / 2, length, true);
        Geometry cylinderGeo = new Geometry("ThickLine", cylinder);

        // Create a material and set the color
        Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", colour);
        cylinderGeo.setMaterial(mat);

        // Calculate the midpoint of the line
        Vector3f midpoint = localMin.add(localMax).mult(0.5f);

        // Set the position of the cylinder to the midpoint
        cylinderGeo.setLocalTranslation(midpoint);

        // Create a quaternion to rotate the cylinder to align with the direction vector
        Quaternion rotation = new Quaternion();
        rotation.lookAt(direction.normalizeLocal(), Vector3f.UNIT_Y);
        cylinderGeo.setLocalRotation(rotation);

        return cylinderGeo;
    }

}
