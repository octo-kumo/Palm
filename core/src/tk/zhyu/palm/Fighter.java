package tk.zhyu.palm;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.HashMap;

public class Fighter extends Actor implements Steerable<Vector2> {
    public static TextureAtlas atlas;
    private static Animation<TextureRegion> IDLE1_ANIMATION;
    private static Animation<TextureRegion> IDLE2_ANIMATION;
    private static Animation<TextureRegion> DRAW_SWORD_ANIMATION;
    private static Animation<TextureRegion> UNDRAW_SWORD_ANIMATION;
    private static Animation<TextureRegion> RUN_ANIMATION;
    private static Animation<TextureRegion> RUN_PUNCH_ANIMATION;
    private static Animation<TextureRegion> PUNCH_ANIMATION;
    private static Animation<TextureRegion> JUMP_ANIMATION;
    private static Animation<TextureRegion> FALL_ANIMATION;
    private static Animation<TextureRegion> ATTACK1_ANIMATION;
    private static Animation<TextureRegion> ATTACK2_ANIMATION;
    private static Animation<TextureRegion> ATTACK3_ANIMATION;
    private static Animation<TextureRegion> ATTACK1_AIR_ANIMATION;
    private static Animation<TextureRegion> ATTACK2_AIR_ANIMATION;
    private static Animation<TextureRegion> ATTACK_KICK_AIR_ANIMATION;
    private static Animation<TextureRegion> ATTACK3_AIR_ANIMATION_START;
    private static Animation<TextureRegion> ATTACK3_AIR_ANIMATION_LOOP;
    private static Animation<TextureRegion> ATTACK3_AIR_ANIMATION_END;
    private static Animation<TextureRegion> CAST_ANIMATION;
    private static Animation<TextureRegion> CAST_START_ANIMATION;
    private static Animation<TextureRegion> CROUCH_ANIMATION;
    private static Animation<TextureRegion> CROUCH_WALK_ANIMATION;
    private static Animation<TextureRegion> ATTACK_KICK_ANIMATION;
    private static Animation<TextureRegion> DEAD_ANIMATION;
    private static Animation<TextureRegion> HURT_ANIMATION;
    private static Animation<TextureRegion> STAND_ANIMATION;
    private static Animation<TextureRegion> KNOCK_ANIMATION;
    private static Animation<TextureRegion> WALL_ANIMATION;
    private static HashMap<Animation<TextureRegion>, Vector2> render_offset;
    static TiledMap map;
    private static Texture HEALTH;
    private static Texture HEALTH_BACKGROUND;
    private Animation<TextureRegion> current;
    float eTime;
    private float aTime;
    private Array<Integer> keys_down;
    private boolean armed = false;
    private Body body;
    private boolean face;
    private final World world;
    private FightScreen fightScreen;

    static float scale = 0.07f;
    protected boolean tempOnGround;
    protected boolean tempHasWall;
    protected boolean wannaCrouch;
    protected boolean wannaGoLeft;
    protected boolean wannaGoRight;
    protected boolean wannaJump;
    protected boolean wannaSword;
    protected boolean wannaAttack1;
    protected boolean wannaAttack2;
    protected boolean wannaAttack3;

    private boolean tagged;
    protected float zeroLinearSpeedThreshold;
    protected float maxLinearSpeed;
    protected float maxLinearAcceleration;
    protected float maxAngularSpeed;
    protected float maxAngularAcceleration;
    public float health, maxHealth;
    public float damage;

    public Fighter(Array<Integer> keys_down, World world, FightScreen fightScreen) {
        this.keys_down = keys_down;
        this.world = world;
        this.fightScreen = fightScreen;
        tempOnGround = false;
        health = maxHealth = 5;
        damage = 0.5f;
        eTime = 0;
        aTime = 0;
        current = IDLE1_ANIMATION;
        face = true;
        hitting = new ArrayList<Fighter>();
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;
        bodyDef.position.set(5, 15);
        body = world.createBody(bodyDef);
        body.setUserData(this);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.22f, 0.72f);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.filter.categoryBits = 4;
        fixtureDef.filter.maskBits = 1;
        body.createFixture(fixtureDef);
        //Round corners
        CircleShape circle = new CircleShape();
        circle.setRadius(0.1f);
        for (int x = -1; x < 2; x += 2) {
            for (int y = -1; y < 2; y += 2) {
                circle.setPosition(new Vector2(0.2f * x, 0.7f * y));
                fixtureDef = new FixtureDef();
                fixtureDef.filter.categoryBits = 2;
                fixtureDef.filter.maskBits = 1;
                fixtureDef.shape = circle;
                fixtureDef.density = 1f;
                body.createFixture(fixtureDef);
            }
        }
        shape.dispose();
        circle.dispose();
    }

    private Vector2 offset;
    private boolean bigImpact = false;

    public void draw(Batch batch, float parentAlpha) {
        offset = render_offset.get(current);
        if (offset == null) offset = Vector2.Zero;
        TextureRegion frame = current.getKeyFrame(aTime);
        batch.draw(frame, getX() + (face ? 0 : 1f) * frame.getRegionWidth() * scale - frame.getRegionWidth() * scale / 2 + offset.x * scale * (face ? 1 : -1), getY() - frame.getRegionHeight() * scale / 2 + scale * 6 + offset.y * scale, (face ? scale : -scale) * frame.getRegionWidth(), frame.getRegionHeight() * scale);
        batch.draw(HEALTH_BACKGROUND, getX() - 1, getY() + 2, 2, 0.2f);
        batch.draw(HEALTH, getX() - 1, getY() + 2, 2 * Math.max(0, health / maxHealth), 0.2f);
    }

    public void decide() {
        wannaCrouch = keys_down.contains(Input.Keys.SHIFT_LEFT, false);
        wannaGoLeft = keys_down.contains(Input.Keys.A, false);
        wannaGoRight = keys_down.contains(Input.Keys.D, false);
        wannaJump = keys_down.contains(Input.Keys.SPACE, false) || keys_down.contains(Input.Keys.W, false);
        wannaSword = keys_down.contains(Input.Keys.ENTER, false);
        wannaAttack1 = keys_down.contains(Input.Keys.J, false);
        wannaAttack2 = keys_down.contains(Input.Keys.K, false);
        wannaAttack3 = keys_down.contains(Input.Keys.L, false);
    }

    public RayCastCallback groundDetecter = new RayCastCallback() {
        public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            if (fixture.getBody() == body)
                return 1;
            return (tempOnGround = tempOnGround || (fraction < 1 && fixture.getFilterData().categoryBits == 1)) ? 0 : 1;
        }
    };

    public RayCastCallback wallDetecter = new RayCastCallback() {
        public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            if (fixture.getBody() == body)
                return 1;
            return (tempHasWall = tempHasWall || (fraction < 1 && fixture.getFilterData().categoryBits == 1)) ? 0 : 1;
        }
    };

    public RayCastCallback attackDetecter = new RayCastCallback() {
        public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            if (fixture.getFilterData().categoryBits == 4 && fixture.getBody() != body && fraction <= 1) {
                hitting.add((Fighter) fixture.getBody().getUserData());
            }
            return 1;
        }
    };
    ArrayList<Fighter> hitting;

    public void tryAttack() {
        System.out.println("ATTACK!");
        hitting.clear();
        world.rayCast(attackDetecter, body.getPosition().cpy().add(face ? -1 : 1, 0), body.getPosition().cpy().add(face ? 5 : -5, 0));
        for (Fighter fighter : hitting) {
            fighter.health -= damage;
            fighter.hurt();
            System.out.println("hit");
        }
    }

    public void react(float delta) {
        eTime += delta;
        aTime += delta;
        setPosition(body.getPosition().x, body.getPosition().y);
        if (health <= 0) {
            if (current != DEAD_ANIMATION) {
                current = DEAD_ANIMATION;
                aTime = 0;
            }
            return;
        }
        Vector2 velocity = body.getLinearVelocity();
        tempOnGround = false;
        world.rayCast(groundDetecter, body.getPosition().cpy(), body.getPosition().cpy().add(0.3f, -1.1f));
        if (!tempOnGround)
            world.rayCast(groundDetecter, body.getPosition().cpy(), body.getPosition().cpy().add(-0.3f, -1.1f));
        if (isOnGround()) {
            /*
             * The third in air attack/ or drop kick, on impact with ground
             * */
            if (getAttackingAir3State() == 2 || getAttackingAir3State() == 1) {
                aTime = 0;
                current = ATTACK3_AIR_ANIMATION_END;
                System.out.println("In Air attack 3 lands.");
                tryAttack();
            }
            if (current == ATTACK_KICK_AIR_ANIMATION) {
                current = IDLE1_ANIMATION;
            }
            /*
             * Land
             * */
            if ((isFalling() || isJumping()) && !isAttacking() && !isHurt() && !isStanding()) {
                current = armed ? IDLE2_ANIMATION : IDLE1_ANIMATION;
            }
            /*
             * On impact
             * Aftershock/stood up
             * */
            if (bigImpact) {
                aTime = 0;
                current = KNOCK_ANIMATION;
                bigImpact = false;
            }
            if (isHurt() && current.isAnimationFinished(aTime)) {
                current = armed ? IDLE2_ANIMATION : IDLE1_ANIMATION;
            }
            if (isStanding() && current.isAnimationFinished(aTime)) {
                current = armed ? IDLE2_ANIMATION : IDLE1_ANIMATION;
            }
            if (isKnocked() && current.isAnimationFinished(aTime)) {
                aTime = 0;
                current = STAND_ANIMATION;
            }
            /*
             * Do not grant such actions when is attacking or (un)drawing sword.
             * */
            if (!(isAttacking() || isUnDrawingSword() || isDrawingSword() || isCasting() || isHurt() || isStanding() || isKnocked())) {
                /*
                 * Key A is LEFT
                 * Key D is RIGHT
                 * Set to Running
                 * */
                if (!isCrouching() && wannaCrouch)
                    current = CROUCH_ANIMATION;
                else if (isCrouching() && !wannaCrouch)
                    current = armed ? IDLE2_ANIMATION : IDLE1_ANIMATION;
                boolean crouching = isCrouching();
                if (wannaGoLeft) {
                    move(velocity, (crouching ? -30 : -60) * delta, delta);
                    current = crouching ? CROUCH_WALK_ANIMATION : RUN_ANIMATION;
                    face = false;
                } else if (wannaGoRight) {
                    move(velocity, (crouching ? 30 : 60) * delta, delta);
                    current = crouching ? CROUCH_WALK_ANIMATION : RUN_ANIMATION;
                    face = true;
                } else if (isRunning() || crouching || isIdle()) {
                    current = crouching ? CROUCH_ANIMATION : (armed ? IDLE2_ANIMATION : IDLE1_ANIMATION);
                }
                /*
                 * Space or W to JUMP
                 * Set to Jumping
                 * */
                if (wannaJump) {
                    jump(velocity, 150 * delta, delta);
                    current = JUMP_ANIMATION;
                    aTime = 0;
                }
                /*
                 * Drawing/Un-drawing sword
                 * (PS) Stop player in x axis
                 * */
                if (!armed && wannaSword) {
                    current = DRAW_SWORD_ANIMATION;
                    aTime = 0;
                } else if (armed && wannaSword) {
                    current = UNDRAW_SWORD_ANIMATION;
                    aTime = 0;
                }
                /*
                 * Check when (un)drawing of sword is done.
                 * */
            } else if (isDrawingSword() && current.isAnimationFinished(aTime)) {
                armed = true;
                current = IDLE2_ANIMATION;
            } else if (isUnDrawingSword() && current.isAnimationFinished(aTime)) {
                armed = false;
                current = IDLE1_ANIMATION;
            }
            /*
             * Attacks, divided into in 3 types
             * */
            if (isReadyToAttackGround() && armed) {
                if (wannaAttack1) {
                    current = ATTACK1_ANIMATION;
                    aTime = 0;
                    tryAttack();
                } else if (wannaAttack2) {
                    current = ATTACK2_ANIMATION;
                    aTime = 0;
                    tryAttack();
                } else if (wannaAttack3) {
                    current = ATTACK3_ANIMATION;
                    aTime = 0;
                    tryAttack();
                }
            } else if (isReadyToAttackGround()) {
                if (wannaAttack2) {
                    current = ATTACK_KICK_ANIMATION;
                    aTime = 0;
                    tryAttack();
                } else if (wannaAttack3 && isRunning()) {
                    current = RUN_PUNCH_ANIMATION;
                    aTime = 0;
                    tryAttack();
                } else if (wannaAttack3 && !isRunning()) {
                    current = PUNCH_ANIMATION;
                    aTime = 0;
                    tryAttack();
                }
            } else if (isAttacking() && current.isAnimationFinished(aTime) && !isCasting() && !isHurt() && !isStanding()) { // Attack ended
                current = IDLE2_ANIMATION;
            }
            /*
             * Spell casting
             * */
            if (!armed && wannaAttack1 && isReadyToAttackGround()) {
                current = CAST_START_ANIMATION;
                aTime = 0;
            } else if (isCasting() && current.isAnimationFinished(aTime) && current == CAST_START_ANIMATION && !isHurt() && !isStanding()) { // Cast finished.
                current = CAST_ANIMATION;
            } else if (isCasting() && !wannaAttack1 && !isHurt() && !isStanding()) {
                current = IDLE1_ANIMATION;
            } else if (current == CAST_ANIMATION && current.isAnimationFinished(aTime) && !isHurt() && !isStanding()) {
                aTime = 0;
                fightScreen.bullets.addBullet(body.getPosition().cpy().add(face ? 1 : -1, 0.25f), face ? 10 : -10, 0, this);
            }
            velocity.x *= (float) Math.pow(0.001f, delta);
        } else {
            /*
             * Touching Wall
             * */
            tempHasWall = false;
            world.rayCast(wallDetecter, body.getPosition().cpy(), body.getPosition().cpy().add(-0.4f, 0));
            if (tempHasWall) {
                current = WALL_ANIMATION;
                face = false;
                velocity.y *= Math.pow(0.5f, delta);
            } else {
                world.rayCast(wallDetecter, body.getPosition().cpy(), body.getPosition().cpy().add(0.4f, 0));
                if (tempHasWall) {
                    current = WALL_ANIMATION;
                    face = true;
                    velocity.y *= Math.pow(0.5f, delta);
                } else if (!isAttacking() && !isJumping())
                    current = FALL_ANIMATION;
            }
            /*
             * Make be knocked out
             * If velocity fast enough
             * */
            if (velocity.y < -10) {
                bigImpact = true;
            }
            if (!isJumping() && !isAttacking() && !isFalling() && !isHurt() && !isStanding()) {
                current = JUMP_ANIMATION;
            }
            if (velocity.y < 0 && isJumping() && !isAttacking() && current != WALL_ANIMATION) {
                current = FALL_ANIMATION;
            }/*
             * Attacks in mid-air, divided into in 3 types
             * */
            if (armed && isReadyToAttackGround()) {
                if (wannaAttack1) {
                    current = ATTACK1_AIR_ANIMATION;
                    aTime = 0;
                    tryAttack();
                } else if (wannaAttack2) {
                    current = ATTACK2_AIR_ANIMATION;
                    aTime = 0;
                    tryAttack();
                } else if (wannaAttack3) {
                    current = ATTACK3_AIR_ANIMATION_START;
                    aTime = 0;
                }
            } else if (!armed && !isReadyToAttackGround()) {
                if (wannaAttack2) {
                    current = ATTACK_KICK_AIR_ANIMATION;
                    aTime = 0;
                    tryAttack();
                }
            } else if (current.isAnimationFinished(aTime) && !isHurt() && !isStanding()) { // Attack ended
                /*
                 * Covers the third in air attack
                 * */
                int state = getAttackingAir3State();
                if (current != ATTACK_KICK_AIR_ANIMATION) {
                    if (state == 0 || state == 4) current = IDLE2_ANIMATION;
                    else if (state == 1) current = ATTACK3_AIR_ANIMATION_LOOP;
                }
            }
        }
        body.setLinearVelocity(velocity);
        if (wannaGoLeft) {
            body.applyForceToCenter(-1, 0, true);
        }
        if (wannaGoRight) {
            body.applyForceToCenter(1, 0, true);
        }
    }

    public void jump(Vector2 velocity, float i, float delta) {
        velocity.y += i;
    }

    public void move(Vector2 velocity, float i, float delta) {
        velocity.x += i;
    }

    public void act(float delta) {
        decide();
        react(delta);
    }

    private boolean isOnGround() {
        return tempOnGround;
    }

    private boolean isFalling() {
        return current == FALL_ANIMATION || current == WALL_ANIMATION;
    }

    private boolean isJumping() {
        return current == JUMP_ANIMATION;
    }

    private boolean isIdle() {
        return current == IDLE1_ANIMATION || current == IDLE2_ANIMATION;
    }

    private boolean isCasting() {
        return current == CAST_ANIMATION || current == CAST_START_ANIMATION;
    }

    private boolean isRunning() {
        return current == RUN_ANIMATION;
    }

    private boolean isCrouching() {
        return current == CROUCH_ANIMATION || current == CROUCH_WALK_ANIMATION;
    }

    private boolean isAttacking() {
        return isCasting() || current == ATTACK1_ANIMATION || current == ATTACK1_AIR_ANIMATION || current == ATTACK2_ANIMATION || current == ATTACK3_ANIMATION || current == ATTACK2_AIR_ANIMATION || current == ATTACK3_AIR_ANIMATION_END || current == ATTACK3_AIR_ANIMATION_LOOP || current == ATTACK3_AIR_ANIMATION_START || current == ATTACK_KICK_AIR_ANIMATION || current == ATTACK_KICK_ANIMATION || current == RUN_PUNCH_ANIMATION || current == PUNCH_ANIMATION;
    }

    private boolean isHurt() {
        return current == HURT_ANIMATION;
    }

    private boolean isStanding() {
        return current == STAND_ANIMATION;
    }

    private int getAttackingAir3State() {
        return armed ? ((current == ATTACK3_AIR_ANIMATION_START) ? 1 : ((current == ATTACK3_AIR_ANIMATION_LOOP) ? 2 : ((current == ATTACK3_AIR_ANIMATION_END) ? 3 : 4))) : 0;
    }

    private boolean isDrawingSword() {
        return current == DRAW_SWORD_ANIMATION;
    }

    private boolean isUnDrawingSword() {
        return current == UNDRAW_SWORD_ANIMATION;
    }

    private boolean isKnocked() {
        return current == KNOCK_ANIMATION;
    }

    private boolean isReadyToAttackGround() {
        return !isAttacking() && !isCrouching() && !isCasting() && !isKnocked() && !isHurt() && !isStanding() && isOnGround();
    }

    static void init() {
        AssetManager manager = ((Palm) Gdx.app.getApplicationListener()).manager;
        manager.load("player.atlas", TextureAtlas.class);
        manager.load("health.png", Texture.class);
        manager.load("health_background.png", Texture.class);
        manager.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
        manager.load("map.tmx", TiledMap.class);
        manager.finishLoading();
        atlas = manager.get("player.atlas", TextureAtlas.class);
        Array<TextureRegion> idle1_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> idle2_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> run_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> crouch_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> crouch_walk_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> jump_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> fall_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> attack1_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> attack2_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> attack3_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> attack_kick_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> attack1_air_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> attack2_air_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> attack_kick_air_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> attack3_air_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> attack3_air_end_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> drawing_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> undrawing_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> casting_frames_start = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> casting_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> run_punch_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> punch_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> dead_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> hurt_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> stand_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> knock_frames = new Array<TextureRegion>(TextureRegion.class);
        Array<TextureRegion> wall_slide_frames = new Array<TextureRegion>(TextureRegion.class);
        for (AtlasRegion region : atlas.getRegions()) {
            if (region.name.startsWith("adventurer-idle-1")) idle1_frames.add(region);
            else if (region.name.startsWith("adventurer-idle-2"))
                idle2_frames.add(region);//adventurer-run-punch-0
            else if (region.name.startsWith("adventurer-run-punch")) run_punch_frames.add(region);
            else if (region.name.startsWith("adventurer-punch")) punch_frames.add(region);
            else if (region.name.startsWith("adventurer-run")) run_frames.add(region);
            else if (region.name.startsWith("adventurer-crouch-walk"))
                crouch_walk_frames.add(region);
            else if (region.name.startsWith("adventurer-crouch")) crouch_frames.add(region);
            else if (region.name.startsWith("adventurer-jump")) jump_frames.add(region);
            else if (region.name.startsWith("adventurer-fall")) fall_frames.add(region);
            else if (region.name.startsWith("adventurer-attack1")) attack1_frames.add(region);
            else if (region.name.startsWith("adventurer-attack2")) attack2_frames.add(region);
            else if (region.name.startsWith("adventurer-kick")) attack_kick_frames.add(region);
            else if (region.name.startsWith("adventurer-attack3")) attack3_frames.add(region);
            else if (region.name.startsWith("adventurer-air-attack1"))
                attack1_air_frames.add(region);
            else if (region.name.startsWith("adventurer-air-attack2"))
                attack2_air_frames.add(region);
            else if (region.name.startsWith("adventurer-air-attack3-loop"))
                attack3_air_frames.add(region);
            else if (region.name.startsWith("adventurer-air-attack3-end"))
                attack3_air_end_frames.add(region);
            else if (region.name.startsWith("adventurer-swrd-drw")) drawing_frames.add(region);
            else if (region.name.startsWith("adventurer-swrd-shte")) undrawing_frames.add(region);
            else if (region.name.startsWith("adventurer-cast-loop")) casting_frames.add(region);
            else if (region.name.startsWith("adventurer-cast")) casting_frames_start.add(region);
            else if (region.name.startsWith("adventurer-drop-kick"))
                attack_kick_air_frames.add(region);
            else if (region.name.startsWith("adventurer-die")) dead_frames.add(region);
            else if (region.name.startsWith("adventurer-hurt")) hurt_frames.add(region);
            else if (region.name.startsWith("adventurer-stand")) stand_frames.add(region);
            else if (region.name.startsWith("adventurer-knock-dwn")) knock_frames.add(region);
            else if (region.name.startsWith("adventurer-wall-slide")) wall_slide_frames.add(region);
        }
        IDLE1_ANIMATION = new Animation<TextureRegion>(0.16f, idle1_frames);
        IDLE1_ANIMATION.setPlayMode(Animation.PlayMode.LOOP);
        IDLE2_ANIMATION = new Animation<TextureRegion>(0.16f, idle2_frames);
        IDLE2_ANIMATION.setPlayMode(Animation.PlayMode.LOOP);
        RUN_ANIMATION = new Animation<TextureRegion>(0.1f, run_frames);
        RUN_ANIMATION.setPlayMode(Animation.PlayMode.LOOP);
        RUN_PUNCH_ANIMATION = new Animation<TextureRegion>(0.1f, run_punch_frames);
        PUNCH_ANIMATION = new Animation<TextureRegion>(0.1f, punch_frames);
        CROUCH_ANIMATION = new Animation<TextureRegion>(0.1f, crouch_frames);
        CROUCH_ANIMATION.setPlayMode(Animation.PlayMode.LOOP);
        CROUCH_WALK_ANIMATION = new Animation<TextureRegion>(0.1f, crouch_walk_frames);
        CROUCH_WALK_ANIMATION.setPlayMode(Animation.PlayMode.LOOP);
        JUMP_ANIMATION = new Animation<TextureRegion>(0.01f, jump_frames);
        FALL_ANIMATION = new Animation<TextureRegion>(0.1f, fall_frames);
        FALL_ANIMATION.setPlayMode(Animation.PlayMode.LOOP);
        ATTACK1_ANIMATION = new Animation<TextureRegion>(0.1f, attack1_frames);
        ATTACK2_ANIMATION = new Animation<TextureRegion>(0.1f, attack2_frames);
        ATTACK_KICK_ANIMATION = new Animation<TextureRegion>(0.1f, attack_kick_frames);
        ATTACK_KICK_AIR_ANIMATION = new Animation<TextureRegion>(0.1f, attack_kick_air_frames);
        ATTACK_KICK_AIR_ANIMATION.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
        ATTACK3_ANIMATION = new Animation<TextureRegion>(0.1f, attack3_frames);
        ATTACK1_AIR_ANIMATION = new Animation<TextureRegion>(0.16f, attack1_air_frames);
        ATTACK2_AIR_ANIMATION = new Animation<TextureRegion>(0.16f, attack2_air_frames);
        ATTACK3_AIR_ANIMATION_START = new Animation<TextureRegion>(0.16f, atlas.findRegion("adventurer-air-attack3-rdy-0"));
        ATTACK3_AIR_ANIMATION_LOOP = new Animation<TextureRegion>(0.16f, attack3_air_frames);
        ATTACK3_AIR_ANIMATION_LOOP.setPlayMode(Animation.PlayMode.LOOP);
        ATTACK3_AIR_ANIMATION_END = new Animation<TextureRegion>(0.1f, attack3_air_end_frames);
        DRAW_SWORD_ANIMATION = new Animation<TextureRegion>(0.1f, drawing_frames);
        UNDRAW_SWORD_ANIMATION = new Animation<TextureRegion>(0.1f, undrawing_frames);
        CAST_START_ANIMATION = new Animation<TextureRegion>(0.09f, casting_frames_start);
        CAST_ANIMATION = new Animation<TextureRegion>(0.16f, casting_frames);
        CAST_ANIMATION.setPlayMode(Animation.PlayMode.LOOP);
        DEAD_ANIMATION = new Animation<TextureRegion>(0.16f, dead_frames);
        HURT_ANIMATION = new Animation<TextureRegion>(0.16f, hurt_frames);
        STAND_ANIMATION = new Animation<TextureRegion>(0.10f, stand_frames);
        KNOCK_ANIMATION = new Animation<TextureRegion>(0.05f, knock_frames);
        WALL_ANIMATION = new Animation<TextureRegion>(0.16f, wall_slide_frames);
        WALL_ANIMATION.setPlayMode(Animation.PlayMode.LOOP);
        map = manager.get("map.tmx", TiledMap.class);
        render_offset = new HashMap<Animation<TextureRegion>, Vector2>();
        render_offset.put(RUN_ANIMATION, new Vector2(-4, 0));
        render_offset.put(RUN_PUNCH_ANIMATION, new Vector2(-4, 0));
        render_offset.put(FALL_ANIMATION, new Vector2(-2, -3));
        render_offset.put(JUMP_ANIMATION, new Vector2(-2, -5));
        render_offset.put(ATTACK1_ANIMATION, new Vector2(-2, 0));
        render_offset.put(ATTACK2_ANIMATION, new Vector2(-1, 0));
        render_offset.put(ATTACK3_ANIMATION, new Vector2(-4, 0));
        render_offset.put(ATTACK1_AIR_ANIMATION, new Vector2(0, -2));
        render_offset.put(ATTACK3_AIR_ANIMATION_START, new Vector2(4, 0));
        render_offset.put(ATTACK3_AIR_ANIMATION_LOOP, new Vector2(4, 0));
        render_offset.put(CAST_START_ANIMATION, new Vector2(-4, 0));
        render_offset.put(CAST_ANIMATION, new Vector2(-4, 0));
        render_offset.put(WALL_ANIMATION, new Vector2(-3, 0));
        HEALTH = manager.get("health.png", Texture.class);
        HEALTH_BACKGROUND = manager.get("health_background.png", Texture.class);
        Bullets.init();
    }

    public Vector2 getLinearVelocity() {
        return body.getLinearVelocity();
    }

    public float getAngularVelocity() {
        return body.getAngularVelocity();
    }

    public float getBoundingRadius() {
        return 1;
    }

    public boolean isTagged() {
        return tagged;
    }

    public void setTagged(boolean tagged) {
        this.tagged = tagged;
    }

    public float getZeroLinearSpeedThreshold() {
        return zeroLinearSpeedThreshold;
    }

    public void setZeroLinearSpeedThreshold(float value) {
        zeroLinearSpeedThreshold = value;
    }

    public float getMaxLinearSpeed() {
        return maxLinearSpeed;
    }

    public void setMaxLinearSpeed(float maxLinearSpeed) {
        this.maxLinearSpeed = maxLinearSpeed;
    }

    public float getMaxLinearAcceleration() {
        return maxLinearAcceleration;
    }

    public void setMaxLinearAcceleration(float maxLinearAcceleration) {
        this.maxLinearAcceleration = maxLinearAcceleration;
    }

    public float getMaxAngularSpeed() {
        return maxAngularSpeed;
    }

    public void setMaxAngularSpeed(float maxAngularSpeed) {
        this.maxAngularSpeed = maxAngularSpeed;
    }

    public float getMaxAngularAcceleration() {
        return maxAngularAcceleration;
    }

    public void setMaxAngularAcceleration(float maxAngularAcceleration) {
        this.maxAngularAcceleration = maxAngularAcceleration;
    }

    public Vector2 getPosition() {
        return body.getPosition();
    }

    public float getOrientation() {
        return body.getAngle();
    }

    public void setOrientation(float orientation) {
        body.setTransform(getPosition(), orientation);
    }

    public float vectorToAngle(Vector2 vector) {
        return vector.angle();
    }

    public Vector2 angleToVector(Vector2 outVector, float angle) {
        return outVector.setAngle(angle);
    }

    public Location<Vector2> newLocation() {
        return new Location<Vector2>() {
            public Vector2 getPosition() {
                return Fighter.this.getPosition();
            }

            public float getOrientation() {
                return Fighter.this.getOrientation();
            }

            public void setOrientation(float orientation) {
                Fighter.this.setOrientation(orientation);
            }

            public float vectorToAngle(Vector2 vector) {
                return Fighter.this.vectorToAngle(vector);
            }

            public Vector2 angleToVector(Vector2 outVector, float angle) {
                return Fighter.this.angleToVector(outVector, angle);
            }

            public Location<Vector2> newLocation() {
                return this;
            }
        };
    }

    public void hurt() {
        if (!(health <= 0)) {
            current = HURT_ANIMATION;
            aTime = 0;
        }
    }
}
