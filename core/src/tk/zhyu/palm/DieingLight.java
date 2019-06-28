package tk.zhyu.palm;

import com.badlogic.gdx.math.MathUtils;

import box2dLight.PointLight;

public class DieingLight {
    private PointLight light;
    private float lengthMax;
    private float lengthMin;
    private float time;
    private float eTime;

    public DieingLight(PointLight light, float lengthMin, float lengthMax, float time) {
        this.light = light;
        this.lengthMax = lengthMax;
        this.lengthMin = lengthMin;
        this.time = time;
        eTime = 0;
    }

    public void update(float delta) {
        eTime += delta;
        light.setDistance(MathUtils.lerp(lengthMin, lengthMax, time / eTime));
    }

    public boolean isComplete() {
        return eTime >= time;
    }

    public PointLight getLight() {
        return light;
    }
}
