package com.intege.mediahand.controller;

import java.io.Serializable;

import com.studiohartman.jamepad.ControllerAxis;
import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerUnpluggedException;

/**
 * This class represents the state of a gamepad at a given moment. This includes
 * the state of the axes and the buttons.
 * <p>
 * This is probably how most people should deal with gamepads.
 * <p>
 * The isConnected field is pretty important. This is how you determine if the controller
 * you asked for is plugged in. If isConnected is false, all other fields will be zero or false.
 * For some applications, you might not need to even bother checking isConnected.
 * <p>
 * All fields are public, but immutable.
 *
 * @author William Hartman
 */
public final class CustomControllerState implements Serializable {
    private static final CustomControllerState DISCONNECTED_CONTROLLER = new CustomControllerState();
    private static final long serialVersionUID = -3068755590868647792L;

    /**
     * Whether or not the controller is currently connected.
     * <p>
     * If the controller is disconnected, all other fields will be 0 or false.
     */
    public final boolean isConnected;

    /**
     * A string describing the type of controller (i.e. "PS4 Controller" or "XInput Controller")
     */
    public final String controllerType;

    /**
     * The x position of the left stick between -1 and 1
     */
    public final float leftStickX;

    /**
     * The y position of the left stick between -1 and 1
     */
    public final float leftStickY;

    /**
     * The x position of the right stick between -1 and 1
     */
    public final float rightStickX;

    /**
     * The y position of the right stick between -1 and 1
     */
    public final float rightStickY;

    /**
     * The angle of the left stick (for reference, 0 is right, 90 is up, 180 is left, 270 is down)
     */
    public final float leftStickAngle;

    /**
     * The amount the left stick is pushed in the current direction. This probably between 0 and 1,
     * but this can't be guaranteed due to weird gamepads (like the square holes on a Logitech Dual Action)
     */
    public final float leftStickMagnitude;

    /**
     * The angle of the right stick (for reference, 0 is right, 90 is up, 180 is left, 270 is down)
     */
    public final float rightStickAngle;

    /**
     * The amount the right stick is pushed in the current direction. This probably between 0 and 1,
     * but this can't be guaranteed due to weird gamepads (like the square holes on a Logitech Dual Action)
     */
    public final float rightStickMagnitude;

    /**
     * Whether or not the left stick is clicked in
     */
    public final boolean leftStickClick;

    /**
     * Whether or not the right stick is clicked in
     */
    public final boolean rightStickClick;

    /**
     * The position of the left trigger between 0 and 1
     */
    public final float leftTrigger;

    /**
     * The position of the right trigger between 0 and 1
     */
    public final float rightTrigger;

    /**
     * Whether or not the left stick was just is clicked in
     */
    public final boolean leftStickJustClicked;

    /**
     * Whether or not the right stick was just is clicked in
     */
    public final boolean rightStickJustClicked;

    /**
     * Whether or not the a button is pressed
     */
    public final boolean a;

    /**
     * Whether or not the b button is pressed
     */
    public final boolean b;

    /**
     * Whether or not the x button is pressed
     */
    public final boolean x;

    /**
     * Whether or not the y button is pressed
     */
    public final boolean y;

    /**
     * Whether or not the left bumper is pressed
     */
    public final boolean lb;

    /**
     * Whether or not the right bumper is pressed
     */
    public final boolean rb;

    /**
     * Whether or not the start button is pressed
     */
    public final boolean start;

    /**
     * Whether or not the back button is pressed
     */
    public final boolean back;

    /**
     * Whether or not the guide button is pressed. For some controller/platform combinations this
     * doesn't work. You probably shouldn't use this.
     */
    public final boolean guide;

    /**
     * Whether or not the up button on the dpad is pushed
     */
    public final boolean dpadUp;

    /**
     * Whether or not the down button on the dpad is pushed
     */
    public final boolean dpadDown;

    /**
     * Whether or not the left button on the dpad is pushed
     */
    public final boolean dpadLeft;

    /**
     * Whether or not the right button on the dpad is pushed
     */
    public final boolean dpadRight;

    /**
     * Whether or not the a button was just pressed
     */
    public final boolean aJustPressed;


    /**
     * Whether or not the b button was just pressed
     */
    public final boolean bJustPressed;

    /**
     * Whether or not the x button was just pressed
     */
    public final boolean xJustPressed;

    /**
     * Whether or not the y button was just pressed
     */
    public final boolean yJustPressed;

    /**
     * Whether or not the left bumper was just pressed
     */
    public final boolean lbJustPressed;

    /**
     * Whether or not the right bumper was just pressed
     */
    public final boolean rbJustPressed;

    /**
     * Whether or not the start button was just pressed
     */
    public final boolean startJustPressed;

    /**
     * Whether or not the back button was just pressed
     */
    public final boolean backJustPressed;

    /**
     * Whether or not the guide button was just pressed
     */
    public final boolean guideJustPressed;

    /**
     * Whether or not the up button on the dpad was just pressed
     */
    public final boolean dpadUpJustPressed;

    /**
     * Whether or not the down button on the dpad was just pressed
     */
    public final boolean dpadDownJustPressed;

    /**
     * Whether or not the left button on the dpad was just pressed
     */
    public final boolean dpadLeftJustPressed;

    /**
     * Whether or not the right button on the dpad was just pressed
     */
    public final boolean dpadRightJustPressed;

    /**
     * Return a controller state based on the current state of the passed controller.
     * <p>
     * If the controller a disconnected mid-read, the disconnected controller is returned, and the
     * pre-disconnection read data is ignored.
     *
     * @param c The ControllerIndex object whose state should be read.
     */
    static CustomControllerState getInstanceFromController(CustomControllerIndex c) {
        try {
            return new CustomControllerState(c);
        } catch (ControllerUnpluggedException e) {
            return DISCONNECTED_CONTROLLER;
        }
    }

    /**
     * Return a ControllerState that represents a disconnected controller. This object is shared.
     *
     * @return The ControllerState representing the disconnected controller.
     */
    static CustomControllerState getDisconnectedControllerInstance() {
        return DISCONNECTED_CONTROLLER;
    }

    private CustomControllerState(CustomControllerIndex c) throws ControllerUnpluggedException {
        this.isConnected = true;
        this.controllerType = c.getName();
        this.leftStickX = c.getAxisState(ControllerAxis.LEFTX);
        this.leftStickY = c.getAxisState(ControllerAxis.LEFTY);
        this.rightStickX = c.getAxisState(ControllerAxis.RIGHTX);
        this.rightStickY = c.getAxisState(ControllerAxis.RIGHTY);
        this.leftStickAngle = (float) Math.toDegrees(Math.atan2(this.leftStickY, this.leftStickX));
        this.leftStickMagnitude = (float) Math.sqrt((this.leftStickX * this.leftStickX) + (this.leftStickY * this.leftStickY));
        this.rightStickAngle = (float) Math.toDegrees(Math.atan2(this.rightStickY, this.rightStickX));
        this.rightStickMagnitude = (float) Math.sqrt((this.rightStickX * this.rightStickX) + (this.rightStickY * this.rightStickY));
        this.leftTrigger = c.getAxisState(ControllerAxis.TRIGGERLEFT);
        this.rightTrigger = c.getAxisState(ControllerAxis.TRIGGERRIGHT);

        this.leftStickJustClicked = c.isButtonJustPressed(ControllerButton.LEFTSTICK);
        this.rightStickJustClicked = c.isButtonJustPressed(ControllerButton.RIGHTSTICK);
        this.leftStickClick = c.isButtonPressed(ControllerButton.LEFTSTICK);
        this.rightStickClick = c.isButtonPressed(ControllerButton.RIGHTSTICK);

        this.aJustPressed = c.isButtonJustPressed(ControllerButton.A);
        this.bJustPressed = c.isButtonJustPressed(ControllerButton.B);
        this.xJustPressed = c.isButtonJustPressed(ControllerButton.X);
        this.yJustPressed = c.isButtonJustPressed(ControllerButton.Y);
        this.lbJustPressed = c.isButtonJustPressed(ControllerButton.LEFTBUMPER);
        this.rbJustPressed = c.isButtonJustPressed(ControllerButton.RIGHTBUMPER);
        this.startJustPressed = c.isButtonJustPressed(ControllerButton.START);
        this.backJustPressed = c.isButtonJustPressed(ControllerButton.BACK);
        this.guideJustPressed = c.isButtonJustPressed(ControllerButton.GUIDE);
        this.dpadUpJustPressed = c.isButtonJustPressed(ControllerButton.DPAD_UP);
        this.dpadDownJustPressed = c.isButtonJustPressed(ControllerButton.DPAD_DOWN);
        this.dpadLeftJustPressed = c.isButtonJustPressed(ControllerButton.DPAD_LEFT);
        this.dpadRightJustPressed = c.isButtonJustPressed(ControllerButton.DPAD_RIGHT);

        this.a = c.isButtonPressed(ControllerButton.A);
        this.b = c.isButtonPressed(ControllerButton.B);
        this.x = c.isButtonPressed(ControllerButton.X);
        this.y = c.isButtonPressed(ControllerButton.Y);
        this.lb = c.isButtonPressed(ControllerButton.LEFTBUMPER);
        this.rb = c.isButtonPressed(ControllerButton.RIGHTBUMPER);
        this.start = c.isButtonPressed(ControllerButton.START);
        this.back = c.isButtonPressed(ControllerButton.BACK);
        this.guide = c.isButtonPressed(ControllerButton.GUIDE);
        this.dpadUp = c.isButtonPressed(ControllerButton.DPAD_UP);
        this.dpadDown = c.isButtonPressed(ControllerButton.DPAD_DOWN);
        this.dpadLeft = c.isButtonPressed(ControllerButton.DPAD_LEFT);
        this.dpadRight = c.isButtonPressed(ControllerButton.DPAD_RIGHT);
    }

    private CustomControllerState() {
        this.isConnected = false;
        this.controllerType = "Not Connected";
        this.leftStickX = 0;
        this.leftStickY = 0;
        this.rightStickX = 0;
        this.rightStickY = 0;
        this.leftStickAngle = 0;
        this.leftStickMagnitude = 0;
        this.rightStickAngle = 0;
        this.rightStickMagnitude = 0;
        this.leftTrigger = 0;
        this.rightTrigger = 0;

        this.leftStickJustClicked = false;
        this.rightStickJustClicked = false;
        this.leftStickClick = false;
        this.rightStickClick = false;

        this.aJustPressed = false;
        this.bJustPressed = false;
        this.xJustPressed = false;
        this.yJustPressed = false;
        this.lbJustPressed = false;
        this.rbJustPressed = false;
        this.startJustPressed = false;
        this.backJustPressed = false;
        this.guideJustPressed = false;
        this.dpadUpJustPressed = false;
        this.dpadDownJustPressed = false;
        this.dpadLeftJustPressed = false;
        this.dpadRightJustPressed = false;

        this.a = false;
        this.b = false;
        this.x = false;
        this.y = false;
        this.lb = false;
        this.rb = false;
        this.start = false;
        this.back = false;
        this.guide = false;
        this.dpadUp = false;
        this.dpadDown = false;
        this.dpadLeft = false;
        this.dpadRight = false;
    }
}
