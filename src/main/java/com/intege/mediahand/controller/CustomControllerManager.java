package com.intege.mediahand.controller;

import static org.libsdl.SDL.Event.SDL_JOYDEVICEADDED;
import static org.libsdl.SDL.Event.SDL_JOYDEVICEREMOVED;
import static org.libsdl.SDL.SDL_IsGameController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.libsdl.SDL;

import com.studiohartman.jamepad.ControllerUnpluggedException;

/**
 * This class handles initializing the native library, connecting to controllers, and managing the
 * list of controllers.
 * <p>
 * Generally, after creating a ControllerManager object and calling initSDLGamepad() on it, you
 * would access the states of the attached gamepads by calling getState().
 * <p>
 * For some applications (but probably very few), getState may have a performance impact. In this
 * case, it may make sense to use the getControllerIndex() method to access the objects used
 * internally by  ControllerManager.
 *
 * @author William Hartman
 */
public class CustomControllerManager {

    private String mappingsPath;
    private boolean isInitialized;
    private CustomControllerIndex[] controllers;

    /**
     * Default constructor. Makes a manager for 4 controllers with the built in mappings from here:
     * https://github.com/gabomdq/SDL_GameControllerDB
     */
    public CustomControllerManager() {
        this(4, "/gamecontrollerdb.txt");
    }

    /**
     * Constructor. Uses built-in mappings from here: https://github.com/gabomdq/SDL_GameControllerDB
     *
     * @param maxNumControllers The number of controllers this ControllerManager can deal with
     */
    public CustomControllerManager(int maxNumControllers) {
        this(maxNumControllers, "/gamecontrollerdb.txt");
    }

    /**
     * Constructor.
     *
     * @param mappingsPath The path to a file containing SDL controller mappings.
     * @param maxNumControllers The number of controller this ControllerManager can deal with
     */
    public CustomControllerManager(int maxNumControllers, String mappingsPath) {
        this.mappingsPath = mappingsPath;
        this.isInitialized = false;
        this.controllers = new CustomControllerIndex[maxNumControllers];
    }

    /**
     * Initialize the ControllerIndex library. This loads the native library and initializes SDL
     * in the native code.
     *
     * @throws IllegalStateException If the native code fails to initialize or if SDL is already initialized
     */
    public void initSDLGamepad() throws IllegalStateException {
        if (this.isInitialized) {
            throw new IllegalStateException("SDL is already initialized!");
        }

        //Initialize SDL
        if (!nativeInitSDLGamepad()) {
            throw new IllegalStateException("Failed to initialize SDL in native method!");
        } else {
            this.isInitialized = true;
        }

        //Set controller mappings. The possible exception is caught, since stuff will still work ok
        //for most people if mapping aren't set.
        try {
            addMappingsFromFile(this.mappingsPath);
        } catch (IOException | IllegalStateException ignored) {
        }


        //Connect and keep track of the controllers
        for (int i = 0; i < this.controllers.length; i++) {
            this.controllers[i] = new CustomControllerIndex(i);
        }
    }

    private boolean nativeInitSDLGamepad() {
        if (SDL.SDL_Init(SDL.SDL_INIT_EVENTS | SDL.SDL_INIT_JOYSTICK | SDL.SDL_INIT_GAMECONTROLLER) != 0) {
            return false;
        }
        SDL.Event event = new SDL.Event();
        while (SDL.SDL_PollEvent(event)) {
        }
        return true;
    }
    /*
        if (SDL_Init(SDL_INIT_EVENTS | SDL_INIT_JOYSTICK | SDL_INIT_GAMECONTROLLER) != 0) {
            printf("NATIVE METHOD: SDL_Init failed: %s\n", SDL_GetError());
            return JNI_FALSE;
        }

        //We don't want any controller connections events (which are automatically generated at init)
        //since they interfere with us detecting new controllers, so we go through all events and clear them.
        while (SDL_PollEvent(&event));

        return JNI_TRUE;
    */

    /**
     * This method quits all the native stuff. Call it when you're done with Jamepad.
     */
    public void quitSDLGamepad() {
        for (CustomControllerIndex c : this.controllers) {
            c.close();
        }
        nativeCloseSDLGamepad();
        this.controllers = new CustomControllerIndex[0];
        this.isInitialized = false;
    }

    private void nativeCloseSDLGamepad() {
        SDL.SDL_Quit();
    }


    /**
     * Return the state of a controller at the passed index. This is probably the way most people
     * should use this library. It's simpler and less verbose, and controller connections and
     * disconnections are automatically handled.
     * <p>
     * Also, no exceptions are thrown here (unless Jamepad isn't initialized), so you don't need
     * to have a million try/catches or anything.
     * <p>
     * The returned state is immutable. This means an object is allocated every time you call this
     * (unless the controller is disconnected). This shouldn't be a big deal (even for games) if your
     * GC is tuned well, but if this is a problem for you, you can go directly through the internal
     * ControllerIndex objects using getControllerIndex().
     * <p>
     * update() is called each time this method is called. Buttons are also queried, so values
     * returned from isButtonJustPressed() in ControllerIndex may not be what you expect. Calling
     * this method will have side effects if you are using the ControllerIndex objects yourself.
     * This should be fine unless you are mixing and matching this method with ControllerIndex
     * objects, which you probably shouldn't do anyway.
     *
     * @param index The index of the controller to be checked
     * @return The state of the controller at the passed index.
     * @throws IllegalStateException if Jamepad was not initialized
     */
    public CustomControllerState getState(int index) throws IllegalStateException {
        verifyInitialized();

        if (index < this.controllers.length && index >= 0) {
            update();
            return CustomControllerState.getInstanceFromController(this.controllers[index]);
        } else {
            return CustomControllerState.getDisconnectedControllerInstance();
        }
    }

    /**
     * Starts vibrating the controller at this given index. If this fails for one reason or another (e.g.
     * the controller at that index doesn't support haptics, or if there is no controller at that index),
     * this method will return false.
     *
     * @param index The index of the controller that will be vibrated
     * @param leftMagnitude The magnitude the left vibration motor will be set to
     * @param rightMagnitude The magnitude the right vibration motor will be set to
     * @return Whether or not vibration was successfully started
     * @throws IllegalStateException if Jamepad was not initialized
     */
    public boolean doVibration(int index, float leftMagnitude, float rightMagnitude, int duration_ms) throws IllegalStateException {
        verifyInitialized();

        if (index < this.controllers.length && index >= 0) {
            try {
                return this.controllers[index].doVibration(leftMagnitude, rightMagnitude, duration_ms);
            } catch (ControllerUnpluggedException e) {
                return false;
            }
        }

        return false;
    }

    /**
     * Use doVibration instead
     *
     * @param index The index of the controller that will be vibrated
     * @param leftMagnitude The magnitude the left vibration motor will be set to
     * @param rightMagnitude The magnitude the right vibration motor will be set to
     * @return Whether or not vibration was successfully started
     * @throws IllegalStateException if Jamepad was not initialized
     * @deprecated Hatpics replaced by new rumble API, use doVibrarion instead
     */
    @Deprecated
    public boolean startVibration(int index, float leftMagnitude, float rightMagnitude) throws IllegalStateException {
        return doVibration(index, leftMagnitude, rightMagnitude, 1000);
    }

    /**
     * Does nothing
     *
     * @param index The index of the controller whose vibration effects will be stopped
     * @deprecated new rumble API does not need this
     */
    @Deprecated
    public void stopVibration(int index) {
        verifyInitialized();

        if (index < this.controllers.length && index >= 0) {
            this.controllers[index].stopVibration();
        }
    }

    /**
     * Returns a the ControllerIndex object with the passed index (0 for p1, 1 for p2, etc.).
     * <p>
     * You should only use this method if you're worried about the object allocations from getState().
     * If you decide to do things this way, your code will be a good bit more verbose and you'll
     * need to deal with potential exceptions.
     * <p>
     * It is generally safe to store objects returned from this method. They will only change internally
     * if you call quitSDLGamepad() followed by a call to initSDLGamepad().
     * <p>
     * Calling update() will run through all the controllers to check for newly plugged in or unplugged
     * controllers. You could do this from your code, but keep that in mind.
     *
     * @param index the index of the ControllerIndex that will be returned
     * @return The internal ControllerIndex object for the passed index.
     * @throws IllegalStateException if Jamepad was not initialized
     */
    public CustomControllerIndex getControllerIndex(int index) {
        verifyInitialized();
        return this.controllers[index];
    }

    /**
     * Return the number of controllers that are actually connected. This may disagree with
     * the ControllerIndex objects held in here if something has been plugged in or unplugged
     * since update() was last called.
     *
     * @return the number of connected controllers.
     * @throws IllegalStateException if Jamepad was not initialized
     */
    public int getNumControllers() {
        verifyInitialized();
        return nativeGetNumRollers();
    }

    private int nativeGetNumRollers() {
        int numJoysticks = SDL.SDL_NumJoysticks();

        int numGamepads = 0;

        for (int i = 0; i < numJoysticks; i++) {
            if (SDL_IsGameController(i)) {
                numGamepads++;
            }
        }

        return numGamepads;
    }

    /**
     * Refresh the connected controllers in the controller list if something has been connected or
     * unplugged.
     * <p>
     * If there hasn't been a change in whether controller are connected or not, nothing will happen.
     *
     * @throws IllegalStateException if Jamepad was not initialized
     */
    public void update() {
        verifyInitialized();
        if (nativeControllerConnectedOrDisconnected()) {
            for (int i = 0; i < this.controllers.length; i++) {
                this.controllers[i].reconnectController();
            }
        }
    }

    private boolean nativeControllerConnectedOrDisconnected() {
        SDL.SDL_GameControllerUpdate();
        SDL.Event event = new SDL.Event();
        while (SDL.SDL_PollEvent(event)) {
            if (event.type == SDL_JOYDEVICEADDED || event.type == SDL_JOYDEVICEREMOVED) {
                return true;
            }
        }
        return false;
    } /*
        SDL_JoystickUpdate();
        while (SDL_PollEvent(&event)) {
            if (event.type == SDL_JOYDEVICEADDED || event.type == SDL_JOYDEVICEREMOVED) {
                return JNI_TRUE;
            }
        }
        return JNI_FALSE;
    */

    /**
     * This method adds mappings held in the specified file. The file is copied to the temp folder so
     * that it can be read by the native code (if running from a .jar for instance)
     *
     * @param path The path to the file containing controller mappings.
     * @throws IOException if the file cannot be read, copied to a temp folder, or deleted.
     * @throws IllegalStateException if the mappings cannot be applied to SDL
     */
    public void addMappingsFromFile(String path) throws IOException, IllegalStateException {
        /*
        Copy the file to a temp folder. SDL can't read files held in .jars, and that's probably how
        most people would use this library.
         */
        Path extractedLoc = Files.createTempFile(null, null).toAbsolutePath();
        InputStream source = getClass().getResourceAsStream(path);
        if (source == null) {
            source = ClassLoader.getSystemResourceAsStream(path);
        }
        if (source == null) {
            throw new IOException("Cannot open resource from classpath " + path);
        }

        Files.copy(source, extractedLoc, StandardCopyOption.REPLACE_EXISTING);

        if (!nativeAddMappingsFromFile(extractedLoc.toString())) {
            throw new IllegalStateException("Failed to set SDL controller mappings! Falling back to build in SDL mappings.");
        }

        Files.delete(extractedLoc);
    }

    private boolean nativeAddMappingsFromFile(String path) {
        return (SDL.SDL_GameControllerAddMappingsFromFile(path) > 0);
    }

    private boolean verifyInitialized() throws IllegalStateException {
        if (!this.isInitialized) {
            throw new IllegalStateException("SDL_GameController is not initialized!");
        }
        return true;
    }


}
