package com.onemillionworlds.tamarintestbed

import com.jme3.math.Vector3f
import com.simsilica.lemur.*;
import com.simsilica.lemur.Button.ButtonAction;
import com.simsilica.lemur.component.*;

def borderedContainer = TbtQuadBackgroundComponent.create(
        texture( name:"/com/onemillionworlds/tamarintestbed/uitextures/bordered-container.png",
                generateMips:false ),
        1, 1, 1, 126, 126,
        1f, false );

/**
 * Primary containers are the very top of a UI element, that has empty space around it
 */
selector( "solidbackground", "glass" ) {
    background = borderedContainer.clone()
}
