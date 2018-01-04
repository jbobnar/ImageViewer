/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.transition;

import static com.bric.image.transition.Transition.CLOCKWISE;
import static com.bric.image.transition.Transition.DOWN;
import static com.bric.image.transition.Transition.HORIZONTAL;
import static com.bric.image.transition.Transition.IN;
import static com.bric.image.transition.Transition.LEFT;
import static com.bric.image.transition.Transition.RIGHT;
import static com.bric.image.transition.Transition.TOP_LEFT;
import static com.bric.image.transition.Transition.UP;
import static com.bric.image.transition.Transition.VERTICAL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.bric.image.transition.BarsTransition2D;
import com.bric.image.transition.BlindsTransition2D;
import com.bric.image.transition.BoxTransition2D;
import com.bric.image.transition.CheckerboardTransition2D;
import com.bric.image.transition.CircleTransition2D;
import com.bric.image.transition.CollapseTransition2D;
import com.bric.image.transition.CubeTransition3D;
import com.bric.image.transition.CurtainTransition2D;
import com.bric.image.transition.DiamondsTransition2D;
import com.bric.image.transition.DocumentaryTransition2D;
import com.bric.image.transition.DotsTransition2D;
import com.bric.image.transition.DropTransition2D;
import com.bric.image.transition.FlipTransition3D;
import com.bric.image.transition.FlurryTransition2D;
import com.bric.image.transition.FunkyWipeTransition2D;
import com.bric.image.transition.GooTransition2D;
import com.bric.image.transition.HalftoneTransition2D;
import com.bric.image.transition.KaleidoscopeTransition2D;
import com.bric.image.transition.LevitateTransition2D;
import com.bric.image.transition.MeshShuffleTransition2D;
import com.bric.image.transition.MicroscopeTransition2D;
import com.bric.image.transition.MirageTransition2D;
import com.bric.image.transition.MotionBlendTransition2D;
import com.bric.image.transition.PivotTransition2D;
import com.bric.image.transition.PushTransition2D;
import com.bric.image.transition.RadialWipeTransition2D;
import com.bric.image.transition.RefractiveTransition2D;
import com.bric.image.transition.RevealTransition2D;
import com.bric.image.transition.RotateTransition2D;
import com.bric.image.transition.ScaleTransition2D;
import com.bric.image.transition.ScribbleTransition2D;
import com.bric.image.transition.SlideTransition2D;
import com.bric.image.transition.SpiralTransition2D;
import com.bric.image.transition.SplitTransition2D;
import com.bric.image.transition.SquareRainTransition2D;
import com.bric.image.transition.SquaresTransition2D;
import com.bric.image.transition.StarTransition2D;
import com.bric.image.transition.StarsTransition2D;
import com.bric.image.transition.SwivelTransition2D;
import com.bric.image.transition.TossTransition2D;
import com.bric.image.transition.WaveTransition2D;
import com.bric.image.transition.WeaveTransition2D;
import com.bric.image.transition.WipeTransition2D;
import com.jakabobnar.imageviewer.Transition;

/**
 * TransitionMap is a list of all available transitions.
 *
 * @author Jaka Bobnar
 *
 */
public final class TransitionsMap extends HashMap<String, Transition> {

    private static final long serialVersionUID = 8976995771644035116L;
    private static final TransitionsMap INSTANCE = new TransitionsMap();

    /**
     * Return the singleton instance of this class.
     * @return the instance
     */
    public static TransitionsMap getInstance() {
        return INSTANCE;
    }

    private TransitionsMap() {
        put(DoorsTransition.NAME,new DoorsTransition());
        put(FadeTransition.NAME,new FadeTransition());
        put(SlideFadeTransition.NAME,new SlideFadeTransition());
        put(SlideTransition.NAME,new SlideTransition());
        put(ZoomRotateTransition.NAME,new ZoomRotateTransition());
        put(ZoomTransition.NAME,new ZoomTransition());

        List<Transition> list = new ArrayList<>();
        list.add(new TransitionWrapper("Cube Horizontal (3D)",new CubeTransition3D(LEFT,true)));
        list.add(new TransitionWrapper("Cube Vertical (3D)",new CubeTransition3D(DOWN,true)));
        list.add(new TransitionWrapper("Flip Horizontal (3D)",new FlipTransition3D(LEFT,true)));
        list.add(new TransitionWrapper("Flip Vertical (3D)",new FlipTransition3D(DOWN,true)));
        list.add(new TransitionWrapper("Bars Horizontal",new BarsTransition2D(HORIZONTAL,true)));
        list.add(new TransitionWrapper("Bars Vertical",new BarsTransition2D(VERTICAL,true)));
        list.add(new TransitionWrapper("Blinds Horizontal",new BlindsTransition2D(LEFT)));
        list.add(new TransitionWrapper("Blinds Vertical",new BlindsTransition2D(DOWN)));
        list.add(new TransitionWrapper("Box",new BoxTransition2D(IN)));
        list.add(new TransitionWrapper("Checkerboard Horizontal",new CheckerboardTransition2D(LEFT)));
        list.add(new TransitionWrapper("Checkerboard Vertical",new CheckerboardTransition2D(DOWN)));
        list.add(new TransitionWrapper("Circle",new CircleTransition2D(IN)));
        list.add(new TransitionWrapper("Collapse",new CollapseTransition2D()));
        list.add(new TransitionWrapper("Curtain",new CurtainTransition2D()));
        list.add(new TransitionWrapper("Diamonds",new DiamondsTransition2D()));
        list.add(new TransitionWrapper("Documentary",new DocumentaryTransition2D(LEFT),
                new DocumentaryTransition2D(RIGHT),new DocumentaryTransition2D(UP),
                new DocumentaryTransition2D(DOWN)));
        list.add(new TransitionWrapper("Dots",new DotsTransition2D()));
        list.add(new TransitionWrapper("Drop",new DropTransition2D()));
        list.add(new TransitionWrapper("Flurry",new FlurryTransition2D(IN)));
        list.add(new TransitionWrapper("Funky Wipe",new FunkyWipeTransition2D(false),new FunkyWipeTransition2D(true)));
        list.add(new TransitionWrapper("Goo",new GooTransition2D()));
        list.add(new TransitionWrapper("Halftone",new HalftoneTransition2D(IN)));
        list.add(new TransitionWrapper("Kaleidoscope",new KaleidoscopeTransition2D()));
        list.add(new TransitionWrapper("Levitate",new LevitateTransition2D()));
        list.add(new TransitionWrapper("Mean Shuffle",new MeshShuffleTransition2D()));
        list.add(new TransitionWrapper("Microscope",new MicroscopeTransition2D()));
        list.add(new TransitionWrapper("Mirage",new MirageTransition2D()));
        list.add(new TransitionWrapper("Motion Blend",true,new MotionBlendTransition2D()));
        list.add(new TransitionWrapper("Pivot",new PivotTransition2D(TOP_LEFT,false)));
        list.add(new TransitionWrapper("Push Horizontal",new PushTransition2D(LEFT)));
        list.add(new TransitionWrapper("Push Vertical",new PushTransition2D(DOWN)));
        list.add(new TransitionWrapper("Radial Wipe",new RadialWipeTransition2D(CLOCKWISE)));
        list.add(new TransitionWrapper("Refractive",new RefractiveTransition2D()));
        list.add(new TransitionWrapper("Reveal Horizontal",new RevealTransition2D(LEFT)));
        list.add(new TransitionWrapper("Reveal Vertical",new RevealTransition2D(UP)));
        list.add(new TransitionWrapper("Rotate",new RotateTransition2D(IN)));
        list.add(new TransitionWrapper("Scale",new ScaleTransition2D(IN)));
        list.add(new TransitionWrapper("Scribble",new ScribbleTransition2D(true)));
        list.add(new TransitionWrapper("Slide Horizontal",new SlideTransition2D(LEFT)));
        list.add(new TransitionWrapper("Slide Vertical",new SlideTransition2D(UP)));
        list.add(new TransitionWrapper("Spiral",new SpiralTransition2D(true)));
        list.add(new TransitionWrapper("Split Horizontal",new SplitTransition2D(HORIZONTAL,true)));
        list.add(new TransitionWrapper("Split Vertical",new SplitTransition2D(VERTICAL,true)));
        list.add(new TransitionWrapper("Squares",new SquaresTransition2D()));
        list.add(new TransitionWrapper("Square Rain",new SquareRainTransition2D(12,true)));
        list.add(new TransitionWrapper("Stars",new StarsTransition2D(LEFT)));
        list.add(new TransitionWrapper("Star",new StarTransition2D(IN)));
        list.add(new TransitionWrapper("Swivel",new SwivelTransition2D(CLOCKWISE)));
        list.add(new TransitionWrapper("Toss",new TossTransition2D(LEFT),new TossTransition2D(RIGHT)));
        list.add(new TransitionWrapper("Wave Horizontal",new WaveTransition2D(LEFT)));
        list.add(new TransitionWrapper("Wave Vertical",new WaveTransition2D(DOWN)));
        list.add(new TransitionWrapper("Weave",new WeaveTransition2D()));
        list.add(new TransitionWrapper("Wipe Horizontal",new WipeTransition2D(LEFT)));
        list.add(new TransitionWrapper("Wipe Vertical",new WipeTransition2D(DOWN)));

        list.forEach(t -> put(t.getName(),t));
    }

    /**
     * Return the list of all available transitions.
     *
     * @return the list of all transitions
     */
    public List<Transition> getRegisteredTransitions() {
        return values().stream().sorted((a, b) -> a.getName().compareTo(b.getName())).collect(Collectors.toList());
    }

}
