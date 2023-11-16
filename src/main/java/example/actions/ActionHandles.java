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
    public static final ActionHandle MOVEMENT_DPAD = new ActionHandle(ActionSets.MAIN, "teleport");

    public static final ActionHandle WALK = new ActionHandle(ActionSets.MAIN, "walk");
    public static final ActionHandle OPEN_HAND_MENU = new ActionHandle(ActionSets.MAIN, "open_hand_menu");
}
