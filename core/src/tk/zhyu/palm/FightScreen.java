package tk.zhyu.palm;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import box2dLight.ChainLight;
import box2dLight.ConeLight;
import box2dLight.Light;
import box2dLight.PointLight;
import box2dLight.RayHandler;

public class FightScreen implements Screen {
    private final Integer mapWidth;
    private final Integer mapHeight;
    public final RayHandler rayHandler;
    private final PointLight playerLight;
    Stage stage;
    public Array<Integer> keys_down = new Array<Integer>();
    public ExtendViewport viewport;
    public OrthographicCamera camera;
    public OrthogonalTiledMapRenderer renderer;
    public World world;
    public Box2DDebugRenderer box2DDebugRenderer;
    public Fighter fighter;
    public Bullets bullets;

    public FightScreen() {
        camera = new OrthographicCamera(20, 10);
        viewport = new ExtendViewport(20, 10, camera);
        stage = new Stage(viewport);
        stage.addListener(new InputListener() {
            public boolean keyDown(InputEvent event, int keycode) {
                keys_down.add(keycode);
                return false;
            }

            public boolean keyUp(InputEvent event, int keycode) {
                keys_down.removeValue(keycode, false);
                return false;
            }
        });
        renderer = new OrthogonalTiledMapRenderer(Fighter.map, 1 / 16f);
        //Box2d Stuff
        world = new World(new Vector2(0, -9.81f), true);
        fighter = new Fighter(keys_down, world, this);
        stage.addActor(fighter);
        AIFighter fighter1 = new AIFighter(keys_down, world, fighter, this);
        stage.addActor(fighter1);
        bullets = new Bullets(world, this);
        stage.addActor(bullets);
        MapBodyBuilder.buildShapes(Fighter.map, world);

        box2DDebugRenderer = new Box2DDebugRenderer();
        System.out.println(world.getBodyCount());
        MapProperties prop = Fighter.map.getProperties();
        mapWidth = prop.get("width", Integer.class);
        mapHeight = prop.get("height", Integer.class);
        rayHandler = new RayHandler(world);
        RayHandler.useDiffuseLight(true);
        Light.setGlobalContactFilter((short) 65535, (short) 65535, (short) 65533);
        playerLight = new PointLight(rayHandler, 500, new Color(0.9f, 0.9f, 0.89f, 1f), 10, 0, 0);// Player Light
        new ConeLight(rayHandler, 256, new Color(0.972549f, 0.8039216f, 0.27450982f, 1f), 10, 5.5f, 9.3f, 90, 120); //Yellow Lamp 1
        new ConeLight(rayHandler, 256, new Color(0.1764706f, 0.7764706f, 0.36862746f, 1), 10, 13.4975f, 4f, 90, 100); //Underground Green Lamp
        new ConeLight(rayHandler, 256, new Color(0.1764706f, 0.7764706f, 0.36862746f, 1f), 10, 18.5f, 14.3f, 90, 120); //Green Lamp 1
        new PointLight(rayHandler, 256, new Color(0.1764706f, 0.7764706f, 0.36862746f, 1f), 10, 22f, 5f);//Underground Acid
        new ConeLight(rayHandler, 256, new Color(0.972549f, 0.8039216f, 0.27450982f, 1f), 10, 18.5f, 24.3f, 90, 120);// Top Yellow Lamp
        world.setContactListener(new ContactListener() {
            public void beginContact(Contact contact) {
                Object dataA = contact.getFixtureA().getBody().getUserData();
                Object dataB = contact.getFixtureB().getBody().getUserData();
                if (dataA != null && dataA instanceof BulletInfo) {
                    ((BulletInfo) dataA).contacts++;
                    ((BulletInfo) dataA).kill();
                    if (dataB != null && dataB instanceof Fighter) {
                        ((Fighter) dataB).health -= ((BulletInfo) dataA).getDamage();
                        ((Fighter) dataB).hurt();
                    }
                }
                if (dataB != null && dataB instanceof BulletInfo) {
                    ((BulletInfo) dataB).contacts++;
                    ((BulletInfo) dataB).kill();
                    if (dataA != null && dataA instanceof Fighter) {
                        ((Fighter) dataA).health -= ((BulletInfo) dataB).getDamage();
                        ((Fighter) dataA).hurt();
                    }
                }
            }

            public void endContact(Contact contact) {
                Object dataA = contact.getFixtureA().getBody().getUserData();
                Object dataB = contact.getFixtureB().getBody().getUserData();
                if (dataA != null && dataA instanceof BulletInfo) ((BulletInfo) dataA).contacts--;
                if (dataB != null && dataB instanceof BulletInfo) ((BulletInfo) dataB).contacts--;
            }

            public void preSolve(Contact contact, Manifold oldManifold) {

            }

            public void postSolve(Contact contact, ContactImpulse impulse) {

            }
        });
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        safelyMoveCam();
        world.step(delta, 5, 5);
        stage.act(delta);
        renderer.setView(camera);
        renderer.render();
        stage.draw();
        playerLight.setPosition(fighter.getX(), fighter.getY());
        rayHandler.setCombinedMatrix(camera);
        rayHandler.updateAndRender();
        if (keys_down.contains(Input.Keys.F1, false))
            box2DDebugRenderer.render(world, camera.combined);
    }

    public void safelyMoveCam() {
        camera.position.set(fighter.getX(), fighter.getY(), 0);
        camera.position.x = MathUtils.clamp(camera.position.x, camera.viewportWidth * .5f, mapWidth - camera.viewportWidth * .5f);
        camera.position.y = MathUtils.clamp(camera.position.y, camera.viewportHeight * .5f, mapHeight - camera.viewportHeight * .5f);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.update();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
        world.dispose();
        rayHandler.dispose();
        renderer.dispose();
        box2DDebugRenderer.dispose();
    }
}
