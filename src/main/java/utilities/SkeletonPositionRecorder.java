package utilities;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.XrActionAppState;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import example.actions.ActionHandles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Records skeleton joint positions for both hands over a grid of target Grip/Trigger values and writes them to CSV.
 *
 * The UI shows current grip/trigger per hand and the next outstanding target combination to help the user.
 */
public class SkeletonPositionRecorder extends BaseAppState {

    public static final float[] TARGET_GRIPS = new float[]{0.0f, 0.2f, 0.5f, 0.8f, 1.0f};
    public static final float[] TARGET_TRIGGERS = new float[]{0.0f, 0.2f, 0.5f, 0.8f, 1.0f};

    public static final float PERMITTED_ERROR = 0.01f;

    private static final String LEFT_PATH = "/user/hand/left";
    private static final String RIGHT_PATH = "/user/hand/right";

    private final Node rootNodeDelegate = new Node("SkeletonPositionRecorder");

    private XrActionAppState xrActions;

    private Label leftNowLabel;
    private Label rightNowLabel;
    private Label nextTargetLabel;
    private final DecimalFormat df2 = new DecimalFormat("0.00");

    private final Map<HandSide, HandRecording> recordings = new EnumMap<>(HandSide.class);

    private boolean writtenCsv = false;

    // Represents a joint sample for a specific target
    private static class JointSample {
        Vector3f position;
        Quaternion rotation;
        float actualGrip;
        float actualTrigger;
        float radius;
        // smaller is better (max of individual diffs)
        float errorMagnitude;
    }

    private static class TargetKey {
        final float targetGrip;
        final float targetTrigger;

        TargetKey(float g, float t) {
            this.targetGrip = g;
            this.targetTrigger = t;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TargetKey)) return false;
            TargetKey targetKey = (TargetKey) o;
            return Float.compare(targetKey.targetGrip, targetGrip) == 0 &&
                   Float.compare(targetKey.targetTrigger, targetTrigger) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetGrip, targetTrigger);
        }
    }

    // Holds all joints for a given target pair
    private static class TargetCapture {
        final TargetKey key;
        final Map<HandJoint, JointSample> joints = new EnumMap<>(HandJoint.class);

        TargetCapture(float g, float t) {
            this.key = new TargetKey(g, t);
        }

        boolean isComplete() {
            return joints.size() == HandJoint.values().length;
        }
    }

    private static class HandRecording {
        final HandSide handSide;
        final List<TargetCapture> targets = new ArrayList<>();

        HandRecording(HandSide side) {
            this.handSide = side;
            for (float g : TARGET_GRIPS) {
                for (float t : TARGET_TRIGGERS) {
                    targets.add(new TargetCapture(g, t));
                }
            }
        }

        Optional<TargetCapture> firstIncomplete() {
            return targets.stream().filter(tc -> !tc.isComplete()).findFirst();
        }

        boolean isComplete() {
            return targets.stream().allMatch(TargetCapture::isComplete);
        }
    }

    @Override
    protected void initialize(Application app) {
        ((SimpleApplication) app).getRootNode().attachChild(rootNodeDelegate);
        xrActions = getState(XrActionAppState.ID, XrActionAppState.class);

        // init structures
        recordings.put(HandSide.LEFT, new HandRecording(HandSide.LEFT));
        recordings.put(HandSide.RIGHT, new HandRecording(HandSide.RIGHT));

        initUi();
    }

    private void initUi() {
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.01f);
        lemurWindow.setLocalTranslation(-0.6f, 1.2f, 6.5f);

        lemurWindow.addChild(new Label("Skeleton Position Recorder"));
        leftNowLabel = lemurWindow.addChild(new Label("Left now: G=0.00 T=0.00"));
        rightNowLabel = lemurWindow.addChild(new Label("Right now: G=0.00 T=0.00"));
        nextTargetLabel = lemurWindow.addChild(new Label("Next target: computing..."));

        rootNodeDelegate.attachChild(lemurWindow);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (xrActions == null || !xrActions.isReady()) {
            return;
        }

        // 1) Read current inputs for each hand
        float leftGrip = readFloat(ActionHandles.GRIP, LEFT_PATH);
        float leftTrigger = readFloat(ActionHandles.TRIGGER, LEFT_PATH);
        float rightGrip = readFloat(ActionHandles.GRIP, RIGHT_PATH);
        float rightTrigger = readFloat(ActionHandles.TRIGGER, RIGHT_PATH);

        if (leftNowLabel != null) {
            leftNowLabel.setText("Left now: G=" + df2.format(leftGrip) + " T=" + df2.format(leftTrigger));
        }
        if (rightNowLabel != null) {
            rightNowLabel.setText("Right now: G=" + df2.format(rightGrip) + " T=" + df2.format(rightTrigger));
        }

        // 2) For each hand, if skeleton available, attempt to update targets when within error and closer than previous
        processHand(HandSide.LEFT, LEFT_PATH, leftGrip, leftTrigger);
        processHand(HandSide.RIGHT, RIGHT_PATH, rightGrip, rightTrigger);

        // 3) Update UI for next outstanding target
        updateNextTargetLabel();

        // 4) If both hands complete and not yet written, write CSV
        if (!writtenCsv && recordings.values().stream().allMatch(HandRecording::isComplete)) {
            writeCsv();
            writtenCsv = true;
        }
    }

    private float readFloat(ActionHandle handle, String restrictPath) {
        FloatActionState state = xrActions.getFloatActionState(handle, restrictPath);
        return state.getState();
    }

    private void processHand(HandSide hand, String path, float grip, float trigger) {
        Optional<Map<HandJoint, BonePose>> skeletonOpt = xrActions.getSkeleton(ActionHandles.HAND_POSE, hand);
        if (skeletonOpt.isEmpty()) {
            return;
        }
        Map<HandJoint, BonePose> skeleton = skeletonOpt.get();

        HandRecording handRec = recordings.get(hand);
        if (handRec == null) return;

        for (TargetCapture tc : handRec.targets) {
            float dg = Math.abs(grip - tc.key.targetGrip);
            float dt = Math.abs(trigger - tc.key.targetTrigger);
            if (dg <= PERMITTED_ERROR && dt <= PERMITTED_ERROR) {
                float thisError = Math.max(dg, dt);
                // update each joint if this measurement is closer than previous
                for (Map.Entry<HandJoint, BonePose> e : skeleton.entrySet()) {
                    HandJoint joint = e.getKey();
                    BonePose pose = e.getValue();
                    JointSample existing = tc.joints.get(joint);
                    if (existing == null || thisError < existing.errorMagnitude) {
                        JointSample js = new JointSample();
                        js.position = pose.position();
                        js.rotation = pose.orientation();
                        js.actualGrip = grip;
                        js.actualTrigger = trigger;
                        js.radius = pose.radius();
                        js.errorMagnitude = thisError;
                        tc.joints.put(joint, js);
                    }
                }
            }
        }
    }

    private void updateNextTargetLabel() {
        // Stable selection: first incomplete in LEFT then RIGHT order, then first in insertion order
        HandRecording left = recordings.get(HandSide.LEFT);
        HandRecording right = recordings.get(HandSide.RIGHT);
        String text;
        Optional<TargetCapture> next = Optional.empty();
        HandSide forHand = null;
        if (left != null) {
            next = left.firstIncomplete();
            forHand = HandSide.LEFT;
        }
        if (next.isEmpty() && right != null) {
            next = right.firstIncomplete();
            forHand = HandSide.RIGHT;
        }
        if (next.isPresent()) {
            TargetCapture tc = next.get();
            text = "Next target: " + forHand + " G=" + df2.format(tc.key.targetGrip) + " T=" + df2.format(tc.key.targetTrigger);
        } else {
            text = "All targets captured. CSV will be written.";
        }
        if (nextTargetLabel != null) {
            nextTargetLabel.setText(text);
        }
    }

    private void writeCsv() {
        File out = new File("simulatedJointValues.csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("handSide,targetGrip,targetTrigger,joint,positionX,positionY,positionZ,rotationX,rotationY,rotationZ,rotationW,radius");
            // Deterministic ordering: hand LEFT then RIGHT, then targets in insertion order, then joints by enum order
            for (HandSide side : List.of(HandSide.LEFT, HandSide.RIGHT)) {
                HandRecording hr = recordings.get(side);
                if (hr == null) continue;
                for (TargetCapture tc : hr.targets) {
                    for (HandJoint joint : HandJoint.values()) {
                        JointSample js = tc.joints.get(joint);
                        if (js == null) continue; // should not happen if complete
                        Vector3f p = js.position;
                        Quaternion r = js.rotation;
                        pw.printf("%s,%.2f,%.2f,%s,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f%n",
                                side.name(), tc.key.targetGrip, tc.key.targetTrigger, joint.name(),
                                p.x, p.y, p.z, r.getX(), r.getY(), r.getZ(), r.getW(), js.radius);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void cleanup(Application app) {
        rootNodeDelegate.removeFromParent();
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }
}
