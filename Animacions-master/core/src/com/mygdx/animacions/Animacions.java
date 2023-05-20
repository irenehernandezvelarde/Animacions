package com.mygdx.animacions;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketListener;
import com.github.czyzby.websocket.WebSockets;

public class Animacions extends ApplicationAdapter {

	Texture walkSheet, background;
	SpriteBatch spriteBatch;
	TextureRegion idleFrames[] = new TextureRegion[4];
	TextureRegion walkingFrames[] = new TextureRegion[4];

	private OrthographicCamera camera;

	Animation<TextureRegion> idleAnimation, walkingAnimation;
	float stateTime;
	SpriteBatch batch;
	float posx = 512, posy = 266;

	Rectangle rectangleUp, rectangleDown, rectangleLeft, rectangleRight;
	final int IDLE=0, UP=1, DOWN=2, LEFT=3, RIGHT=4;
	float lastSend = 0f;

	WebSocket socket;
	String address = "localhost";
	int port = 8888;

	@Override
	public void create() {

		walkSheet = new Texture(Gdx.files.internal("mario.png"));
		background = new Texture(Gdx.files.internal("backgroundx2.png"));

		// Frames cuando esta quieto
		idleFrames[0] = new TextureRegion(walkSheet,4,122,20,39);
		idleFrames[1] = new TextureRegion(walkSheet,31,122,20,39);
		idleFrames[2] = new TextureRegion(walkSheet,62,122,20,39);
		idleFrames[3] = new TextureRegion(walkSheet,90,122,20,39);

		// Frames andar (se giran cuando anda hacia la izquierda)
		walkingFrames[0] = new TextureRegion(walkSheet,21,0,20,39);
		walkingFrames[1] = new TextureRegion(walkSheet,42,0,20,39);
		walkingFrames[2] = new TextureRegion(walkSheet,64,0,20,39);
		walkingFrames[3] = new TextureRegion(walkSheet,85,0,20,39);

		idleAnimation = new Animation<TextureRegion>(0.3f, idleFrames);
		walkingAnimation = new Animation<TextureRegion>(0.3f, walkingFrames);

		camera = new OrthographicCamera();
		camera.setToOrtho(false, 1024, 512);
		batch = new SpriteBatch();
		stateTime = 0f;

		// Controles
		rectangleUp = new Rectangle(0, 512*2/3, 1024, 512/3);
		rectangleDown = new Rectangle(0, 0, 1024, 512/3);
		rectangleLeft = new Rectangle(0, 0, 1024/3,512);
		rectangleRight = new Rectangle(1024*2/3, 0, 1024/3, 512);

		// Abrir Websocket
		if( Gdx.app.getType()== Application.ApplicationType.Android )
			// en Android el host és accessible per 10.0.2.2
			address = "10.0.2.2";
		socket = WebSockets.newSocket(WebSockets.toWebSocketUrl(address, port));
		socket.setSendGracefully(false);
		socket.addListener((WebSocketListener) new MyWSListener());
		socket.connect();
		socket.send("Test");
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void render() {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stateTime += Gdx.graphics.getDeltaTime();

		camera.update();

		TextureRegion frame = idleAnimation.getKeyFrame(stateTime,true);
		TextureRegion walkCurrentFrame = walkingAnimation.getKeyFrame(stateTime,true);

		batch.begin();
		batch.setProjectionMatrix(camera.combined);
		batch.draw(background, 0 , 0, 1024, 512);
		batch.end();

		int direction = virtual_joystick_control();

		switch (direction){
			case 0: // Quieto
				batch.begin();
				idleAnimation = new Animation<TextureRegion>(0.3f, idleFrames);
				batch.draw(frame, posx, posy, 0, 0,
						frame.getRegionWidth(),frame.getRegionHeight(),2,2,0);
				batch.end();
				break;
			case 1: // Arriba
				idleAnimation = new Animation<TextureRegion>(0.3f, walkingFrames);
				batch.begin();
				posy += 5;
				batch.draw(walkCurrentFrame, posx, posy,0, 0,
						walkCurrentFrame.getRegionWidth(),walkCurrentFrame.getRegionHeight(),2,2,0);
				batch.end();
				break;
			case 2: // Abajo
				idleAnimation = new Animation<TextureRegion>(0.3f, walkingFrames);
				batch.begin();
				posy -= 5;
				batch.draw(walkCurrentFrame, posx, posy,0, 0,
						walkCurrentFrame.getRegionWidth(),walkCurrentFrame.getRegionHeight(),2,2,0);
				batch.end();
				break;
			case 3: // Izquierda
				idleAnimation = new Animation<TextureRegion>(0.3f, walkingFrames);
				batch.begin();
				posx -= 5;
				batch.draw(walkCurrentFrame, posx, posy,walkCurrentFrame.getRegionWidth(), 0,
						walkCurrentFrame.getRegionWidth(),walkCurrentFrame.getRegionHeight(),-2,2,0); // Se pone el scaleX en negativo para girarlo del reves
				batch.end();
				break;
			case 4: // Derecha
				idleAnimation = new Animation<TextureRegion>(0.3f, walkingFrames);
				batch.begin();
				posx += 5;
				batch.draw(walkCurrentFrame, posx, posy, 0, 0,
						walkCurrentFrame.getRegionWidth(), walkCurrentFrame.getRegionHeight(), 2, 2, 0);
				batch.end();
				break;
		}

		// Envio websockets
		if( stateTime-lastSend > 1.0f ) {
			lastSend = stateTime;
			socket.send("Posicion x:"+posx+", y:"+posy);
		}
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {
		spriteBatch.dispose();
		walkSheet.dispose();
		background.dispose();
	}

	protected int virtual_joystick_control() {
		for(int i=0;i<10;i++)
			if (Gdx.input.isTouched(i)) {
				Vector3 touchPos = new Vector3();
				touchPos.set(Gdx.input.getX(i), Gdx.input.getY(i), 0);
				camera.unproject(touchPos);
				if (rectangleUp.contains(touchPos.x, touchPos.y)) {
					return UP;
				} else if (rectangleDown.contains(touchPos.x, touchPos.y)) {
					return DOWN;
				} else if (rectangleLeft.contains(touchPos.x, touchPos.y)) {
					return LEFT;
				} else if (rectangleRight.contains(touchPos.x, touchPos.y)) {
					return RIGHT;
				}
			}
		return IDLE;
	}

	// COMUNICACIONS (rebuda de missatges)
	/////////////////////////////////////////////
	class MyWSListener implements WebSocketListener {

		@Override
		public boolean onOpen(WebSocket webSocket) {
			System.out.println("Opening...");
			return false;
		}

		@Override
		public boolean onClose(WebSocket webSocket, int closeCode, String reason) {
			System.out.println("Closing...");
			return false;
		}

		@Override
		public boolean onMessage(WebSocket webSocket, String packet) {
			System.out.println("Message:");
			return false;
		}

		@Override
		public boolean onMessage(WebSocket webSocket, byte[] packet) {
			System.out.println("Message:");
			return false;
		}

		@Override
		public boolean onError(WebSocket webSocket, Throwable error) {
			System.out.println("ERROR:"+error.toString());
			return false;
		}
	}
}