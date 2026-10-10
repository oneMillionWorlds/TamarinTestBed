package example.actions;

import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;

public class ActionHandles{

    public static final ActionHandle GRIP = new ActionHandle(ActionSets.MAIN, "grip");
    public static final ActionHandle HAPTIC = new ActionHandle(ActionSets.MAIN, "haptic");
    public static final ActionHandle TRIGGER = new ActionHandle(ActionSets.MAIN, "trigger");
    public static final ActionHandle HAND_POSE = new ActionHandle(ActionSets.MAIN, "handpose");

    /**
     * This puts all the dpad controls into a single action, this sucks but is the only way to do it on older runtimes
     * which may not support the XR_EXT_dpad_binding extension
     */
    public static final ActionHandle MOVEMENT_DPAD = new ActionHandle(ActionSets.MAIN, "movement_dpad");

    public static final ActionHandle WALK = new ActionHandle(ActionSets.MAIN, "walk");
    public static final ActionHandle OPEN_HAND_MENU = new ActionHandle(ActionSets.MAIN, "open_hand_menu");

    /*
     * META ONLY thumb microgestures (bare hands). See MetaOnlyHandGesturesExampleState
     */
    public static final ActionHandle MICROGESTURE_TAP = new ActionHandle(ActionSets.MAIN, "microgesture_tap");
    public static final ActionHandle MICROGESTURE_SWIPE_LEFT = new ActionHandle(ActionSets.MAIN, "microgesture_swipe_left");
    public static final ActionHandle MICROGESTURE_SWIPE_RIGHT = new ActionHandle(ActionSets.MAIN, "microgesture_swipe_right");
    public static final ActionHandle MICROGESTURE_SWIPE_FORWARD = new ActionHandle(ActionSets.MAIN, "microgesture_swipe_forward");
    public static final ActionHandle MICROGESTURE_SWIPE_BACKWARD = new ActionHandle(ActionSets.MAIN, "microgesture_swipe_backward");

    /**
     * Bound to a Tamarin detected gesture (thumbs up), works on any runtime with hand tracking. See MetaOnlyHandGesturesExampleState
     */
    public static final ActionHandle RESET_CUBE = new ActionHandle(ActionSets.MAIN, "reset_cube");
    /**
     * Bound to a ring finger pinch (Tamarin gesture binding) and controller buttons. See MetaOnlyHandGesturesExampleState
     */
    public static final ActionHandle SPIN_CUBE = new ActionHandle(ActionSets.MAIN, "spin_cube");
    /**
     * A float action, bound to the middle finger pinch strength (Tamarin gesture binding) and the grip. See MetaOnlyHandGesturesExampleState
     */
    public static final ActionHandle GROW_CUBE = new ActionHandle(ActionSets.MAIN, "grow_cube");
    /**
     * Only bound to hand gestures (no controller bindings). MetaOnlyHandGesturesExampleState shows when they are active
     */
    public static final ActionHandle FIST = new ActionHandle(ActionSets.MAIN, "fist");
    public static final ActionHandle POINT = new ActionHandle(ActionSets.MAIN, "point");
    public static final ActionHandle THUMBS_UP = new ActionHandle(ActionSets.MAIN, "thumbs_up");
}
