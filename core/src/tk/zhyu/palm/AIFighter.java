package tk.zhyu.palm;

import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.Arrive;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class AIFighter extends Fighter {
    private SteeringAcceleration<Vector2> steeringAcceleration;
    private SteeringBehavior<Vector2> behavior;

    public AIFighter(Array<Integer> keys_down, World world, Fighter fighter, FightScreen fightScreen) {
        super(keys_down, world, fightScreen);
        zeroLinearSpeedThreshold = 0.1f;
        maxLinearSpeed = 2000;
        maxLinearAcceleration = 60;
        maxAngularSpeed = 1;
        maxAngularAcceleration = 2;
        steeringAcceleration = new SteeringAcceleration<Vector2>(new Vector2());
        Arrive<Vector2> arrive = new Arrive<Vector2>(this, fighter).setTimeToTarget(0.1f).setArrivalTolerance(5).setDecelerationRadius(4);
        setBehavior(arrive);
        wannaCrouch = wannaGoLeft = wannaGoRight = wannaJump = wannaSword = wannaAttack1 = wannaAttack2 = wannaAttack3 = false;
    }

    public void decide() {
        if (behavior != null) {
            behavior.calculateSteering(steeringAcceleration);
            if (steeringAcceleration.linear != null) {
                if (Math.abs(steeringAcceleration.linear.x) > 2f)
                    wannaGoLeft = !(wannaGoRight = steeringAcceleration.linear.x > 0);
                else
                    wannaGoLeft = wannaGoRight = false;
                wannaJump = steeringAcceleration.linear.y > 15;
            }
        }
    }

    public void jump(Vector2 velocity, float i, float delta) {
        if (steeringAcceleration.linear != null)
            velocity.y = 10;
    }

    public void move(Vector2 velocity, float i, float delta) {
        if (steeringAcceleration.linear != null)
            velocity.x += steeringAcceleration.linear.x * delta;
    }

    public void setBehavior(SteeringBehavior<Vector2> behavior) {
        this.behavior = behavior;
    }

    public SteeringBehavior<Vector2> getBehavior() {
        return behavior;
    }

    public SteeringAcceleration<Vector2> getSteeringAcceleration() {
        return steeringAcceleration;
    }

    public void setSteeringAcceleration
            (SteeringAcceleration<Vector2> steeringAcceleration) {
        this.steeringAcceleration = steeringAcceleration;
    }
}
