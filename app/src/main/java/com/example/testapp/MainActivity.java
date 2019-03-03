package com.example.testapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SkeletonNode;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Light;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;



public class MainActivity extends AppCompatActivity {
    private ArFragment fragment;
    private PointerDrawable pointer = new PointerDrawable();
    private boolean isTracking;
    private boolean isHitting;
    private TransformableNode fieldGlobal;
    private Deque<Node> globalUndoArray = new ArrayDeque<Node>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragment = (ArFragment)
                getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        fragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            fragment.onUpdate(frameTime);
            onUpdate();
        });
        initializeGallery();


        }



    private void onUpdate() {
        boolean trackingChanged = updateTracking();
        View contentView = findViewById(android.R.id.content);
        if (trackingChanged) {
            if (isTracking) {
                contentView.getOverlay().add(pointer);
            } else {
                contentView.getOverlay().remove(pointer);
            }
            contentView.invalidate();
        }

        if (isTracking) {
            boolean hitTestChanged = updateHitTest();
            if (hitTestChanged) {
                pointer.setEnabled(isHitting);
                contentView.invalidate();
            }
        }
    }

    private boolean updateTracking() {
        Frame frame = fragment.getArSceneView().getArFrame();
        boolean wasTracking = isTracking;
        isTracking = frame != null &&
                frame.getCamera().getTrackingState() == TrackingState.TRACKING;
        return isTracking != wasTracking;
    }

    private boolean updateHitTest() {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        boolean wasHitting = isHitting;
        isHitting = false;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    isHitting = true;
                    break;
                }
            }
        }
        return wasHitting != isHitting;
    }

    private android.graphics.Point getScreenCenter() {
        View vw = findViewById(android.R.id.content);
        return new android.graphics.Point(vw.getWidth()/2, vw.getHeight()/2);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initializeGallery() {
        LinearLayout gallery = findViewById(R.id.gallery_layout);

        ImageView stadium1 = new ImageView(this);
        stadium1.setImageResource(R.drawable.smallfield);
        stadium1.setContentDescription("stadium1");
        stadium1.setOnClickListener(view ->{addObject(Uri.parse("stadium1.sfb"),1);});
        gallery.addView(stadium1);

        ImageView tinker = new ImageView(this);
        tinker.setImageResource(R.drawable.red_char);
        tinker.setContentDescription("tinker");
        tinker.setOnClickListener(view ->{addObject(Uri.parse("tinker.sfb"),0);});
        gallery.addView(tinker);

        ImageView Ball2 = new ImageView(this);
        Ball2.setImageResource(R.drawable.green_char);
        Ball2.setContentDescription("Ball2");
        Ball2.setOnClickListener(view ->{addObject(Uri.parse("Ball2.sfb"), 0);});
        gallery.addView(Ball2);

        ImageView soccer = new ImageView(this);
        soccer.setImageResource(R.drawable.largefield);
        soccer.setContentDescription("soccer");
        soccer.setOnClickListener(view ->{addObject(Uri.parse("soccer2.sfb"), 1);});
        gallery.addView(soccer);

        ImageView arrow = new ImageView(this);
        arrow.setImageResource(R.drawable.arrow);
        arrow.setContentDescription("arrow");
        arrow.setOnClickListener(view ->{addObject(Uri.parse("arrow.sfb"), 2);});
        gallery.addView(arrow);
    }

    private void addObject(Uri model, int type) {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    placeObject(fragment, hit.createAnchor(), model, type);
                    break;

                }
            }
        }
    }

    private void placeObject(ArFragment fragment, Anchor anchor, Uri model, int type) {
        CompletableFuture<Void> renderableFuture =
                ModelRenderable.builder()
                        .setSource(fragment.getContext(), model)
                        .build()
                        .thenAccept(renderable -> addNodeToScene(fragment, anchor, renderable, type))
                        .exceptionally((throwable -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage(throwable.getMessage())
                                    .setTitle("Codelab error!");
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            return null;
                        }));
    }

    private void addNodeToScene(ArFragment fragment, Anchor anchor, Renderable renderable, int type) {
            AnchorNode anchorNode = new AnchorNode(anchor);
            TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
            node.getScaleController().setMaxScale(0.10f);
            node.getScaleController().setMinScale(0.05f);
            node.setRenderable(renderable);
            node.setParent(anchorNode);
            fragment.getArSceneView().getScene().addChild(anchorNode);
            node.select();
            if(type == 1) {
                //THINGS FOR STADIUM
                node.getScaleController().setMaxScale(2.80f);
                node.getScaleController().setMinScale(0.5f);
                fieldGlobal = node;
            }
            else if(type == 2){
                //THINGS FOR LIGHT
                //node.setLight(Light.builder(Light.Type.POINT).build());
                node.getScaleController().setMaxScale(0.02f);
                node.getScaleController().setMinScale(0.005f);
            }
            else
            {
                //THINGS FOR PLAYERS
                node.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), -90f));
                node.getScaleController().setMaxScale(0.055f);
                node.getScaleController().setMinScale(0.01f);
            }
            globalUndoArray.push(node);
        }

        public void undoButton(View v){
            if(globalUndoArray.size() != 0) {
                globalUndoArray.peek().getParent().removeChild(globalUndoArray.peek());
                if(globalUndoArray.pop() == fieldGlobal)
                {
                    fieldGlobal = null;
                }

            }
    }

    public void fieldSet(View v) {
        if (fieldGlobal != null) {
            globalUndoArray.remove(fieldGlobal);
            SkeletonNode node = new SkeletonNode();
            node.setParent(fieldGlobal.getParent());
            node.setRenderable(fieldGlobal.getRenderable());
            node.setWorldRotation(fieldGlobal.getWorldRotation());
            node.setWorldScale(fieldGlobal.getWorldScale());
            globalUndoArray.push(node);
            fieldGlobal.getParent().removeChild(fieldGlobal);
            fieldGlobal = null;
        }
    }

    public void clearAll(View v){
            if(globalUndoArray.size() != 0) {
                int size = globalUndoArray.size();
                for(int z = 0; z < size; z++){
                    globalUndoArray.peek().getParent().removeChild(globalUndoArray.pop());
                }
                fieldGlobal = null;
            }
        }


    public void goToHome(View v) {
        Intent intent = new Intent(MainActivity.this, homescreen.class);
        startActivity(intent);
        finish();
    }

}
