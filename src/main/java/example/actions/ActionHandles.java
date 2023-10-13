package example.actions;

import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;

public class ActionHandles{

    public static final ActionHandle GRIP = new ActionHandle(ActionSets.MAIN, "grip");
    public static final ActionHandle HAPTIC = new ActionHandle(ActionSets.MAIN, "haptic");
    public static final ActionHandle TRIGGER = new ActionHandle(ActionSets.MAIN, "trigger");
    public static final ActionHandle HAND_POSE = new ActionHandle(ActionSets.MAIN, "handpose");

    public static final ActionHandle SYNTHETIC_D_PAD = new ActionHandle(ActionSets.MAIN, "dpad");

    //can do it properly once LWJGL is upgraded to 3.3.3 and we can use true dpads
//    public static final ActionHandle TELEPORT = new ActionHandle(ActionSets.MAIN, "teleport");
//    public static final ActionHandle TURN_LEFT = new ActionHandle(ActionSets.MAIN, "turnLeft");
//    public static final ActionHandle TURN_RIGHT = new ActionHandle(ActionSets.MAIN, "turnRight");
    public static final ActionHandle WALK = new ActionHandle(ActionSets.MAIN, "walk");
    public static final ActionHandle OPEN_HAND_MENU = new ActionHandle(ActionSets.MAIN, "open_handmenu");
}
