package io.github.katakishi0.oberonextended.mobile;

import org.junit.Test;
import static org.junit.Assert.*;

public class RootGamepadCaptureTest {
    @Test public void linuxGamepadButtonsHaveCorrectXboxMappings() {
        assertEquals(InputState.A,RootGamepadCapture.testButton("BTN_SOUTH"));
        assertEquals(InputState.B,RootGamepadCapture.testButton("BTN_EAST"));
        assertEquals(InputState.X,RootGamepadCapture.testButton("BTN_WEST"));
        assertEquals(InputState.Y,RootGamepadCapture.testButton("BTN_NORTH"));
        assertEquals(InputState.LB,RootGamepadCapture.testButton("BTN_TL"));
        assertEquals(InputState.RB,RootGamepadCapture.testButton("BTN_TR"));
        assertEquals(InputState.START,RootGamepadCapture.testButton("BTN_START"));
        assertEquals(0,RootGamepadCapture.testButton("KEY_POWER"));
        assertEquals(0,RootGamepadCapture.testButton("ABS_X"));
    }
    @Test public void clearPhysicalDoesNotEraseTouchGamepad() {
        InputState s=new InputState();
        s.setTouch(0,0,0,0,0,0,InputState.X);
        s.setPhysicalButton(InputState.Y,true);
        s.clearPhysical();
        assertEquals(InputState.X,s.snapshot().buttons);
        s.setUsePhysical(true);
        assertEquals(0,s.snapshot().buttons);
    }
}
