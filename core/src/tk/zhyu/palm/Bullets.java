package tk.zhyu.palm;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;

import box2dLight.PointLight;

public class Bullets extends Actor {
    public static ParticleEffect EXPLOSION;
    public static ParticleEffect TRAIL;
    private ParticleEffectPool explosionEffectPool;
    private ParticleEffectPool trailEffectPool;

    float eTime = 0;

    public Array<BulletInfo> bullets;
    private Array<ParticleEffectPool.PooledEffect> effects;
    private Array<DieingLight> lights;
    private World world;
    private FightScreen screen;

    public Bullets(World world, FightScreen screen) {
        this.world = world;
        this.screen = screen;
        bullets = new Array<BulletInfo>();
        lights = new Array<DieingLight>();
        effects = new Array<ParticleEffectPool.PooledEffect>();
        explosionEffectPool = new ParticleEffectPool(EXPLOSION, 1, 1024);
        trailEffectPool = new ParticleEffectPool(TRAIL, 1, 1024);
    }

    public void act(float delta) {
        eTime += delta;
        for (BulletInfo body : bullets) {
            ParticleEffectPool.PooledEffect effect = body.getTrail();
            effect.update(delta);
            Vector2 position = body.getBullet().getPosition();
            if (body.isDead() && body.contacts <= 0) {
                effects.add(effect);
                addExplosion(position.x, position.y);
                effect.allowCompletion();
                lights.add(new DieingLight(body.getPointLight(), 0, 10, 0.4f));
                world.destroyBody(body.getBullet());
                bullets.removeValue(body, true);
            } else {
                body.getPointLight().setPosition(position);
                effect.setPosition(position.x, position.y);
            }
        }
        for (ParticleEffectPool.PooledEffect effect : effects) {
            effect.update(delta);
            if (effect.isComplete()) {
                effects.removeValue(effect, true);
                effect.free();
            }
        }
        for (DieingLight light : lights) {
            light.update(delta);
            if (light.isComplete()) {
                light.getLight().remove();
                lights.removeValue(light, true);
            }
        }
    }

    public void addExplosion(float x, float y) {
        ParticleEffectPool.PooledEffect pooledEffect = explosionEffectPool.obtain();
        pooledEffect.setPosition(x, y);
        pooledEffect.start();
        effects.add(pooledEffect);
    }


    public void draw(Batch batch, float parentAlpha) {
        for (BulletInfo body : bullets) {
            ParticleEffectPool.PooledEffect effect = body.getTrail();
            effect.draw(batch);
        }
        for (ParticleEffectPool.PooledEffect effect : effects) {
            effect.draw(batch);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void init() {
        System.out.print("Loading Effects...");
        EXPLOSION = new ParticleEffect();
        EXPLOSION.load(Gdx.files.internal("effects/explosion.p"), Fighter.atlas);
        TRAIL = new ParticleEffect();
        TRAIL.load(Gdx.files.internal("effects/fireball.p"), Fighter.atlas);
        EXPLOSION.scaleEffect(1 / 64f);
        TRAIL.scaleEffect(1 / 64f);
        TRAIL.setEmittersCleanUpBlendFunction(false);
        EXPLOSION.setEmittersCleanUpBlendFunction(false);
        System.out.println("  Done.");
    }

    public void reset() {
        for (BulletInfo bulletInfo : bullets) {
            bulletInfo.getTrail().free();
            bulletInfo.getPointLight().dispose();
            bulletInfo.kill();
        }
        bullets.clear();
    }

    public void addBullet(Vector2 position, int x, int y, Fighter fighter) {
        PointLight pointLight = new PointLight(screen.rayHandler, 256, new Color(0.99215686f, 0.8117647f, 0.34509805f, 1), 5f, 0f, 0f);
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(position);
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.gravityScale = 0;
        Body bullet = world.createBody(bodyDef);
        bullet.setLinearVelocity(x, y);
        BulletInfo bulletInfo = new BulletInfo(bullet, fighter.damage, pointLight, trailEffectPool.obtain());
        bulletInfo.getTrail().start();
        bullet.setUserData(bulletInfo);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = 10f;
        fixtureDef.friction = 1f;
        fixtureDef.restitution = 1f;
        fixtureDef.shape = new CircleShape();
        fixtureDef.shape.setRadius(0.1f);
        bullet.createFixture(fixtureDef);
        fixtureDef.shape.dispose();
        bullets.add(bulletInfo);
    }
}
