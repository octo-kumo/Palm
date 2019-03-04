package tk.zhyu.palm;

import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.physics.box2d.Body;

import box2dLight.PointLight;

public class BulletInfo {
    private final Body bullet;
    private final float damage;
    private final PointLight pointLight;
    private final ParticleEffectPool.PooledEffect trail;
    public int contacts = 0;
    private boolean dead = false;

    public BulletInfo(Body bullet, float damage, PointLight pointLight, ParticleEffectPool.PooledEffect trail) {
        this.bullet = bullet;
        this.damage = damage;
        this.pointLight = pointLight;
        this.trail = trail;
    }

    public float getDamage() {
        return damage;
    }

    public PointLight getPointLight() {
        return pointLight;
    }

    public Body getBullet() {
        return bullet;
    }

    public ParticleEffectPool.PooledEffect getTrail() {
        return trail;
    }

    public boolean isDead() {
        return dead;
    }

    public void kill() {
        dead = true;
    }
}
