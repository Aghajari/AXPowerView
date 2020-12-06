package com.aghajari.axpowerview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import android.os.Bundle;
import android.view.View;

import com.aghajari.powerview.AXPowerView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final AppCompatTextView status = findViewById(R.id.status);
        final AXPowerView powerView = findViewById(R.id.powerView);

        powerView.setAnimatorListener(new AXPowerView.AnimatorListener() {
            @Override
            public void onAnimationEnded(AXPowerView.State currentState, AXPowerView.State nextState) {
                onStateChanged(nextState,nextState,true);
            }

            @Override
            public void onStateChanged(AXPowerView.State from, AXPowerView.State to,boolean animationLoaded) {
                switch (to) {
                    case SUCCEED:
                        if (powerView.isAnimationRunning()) {
                            status.setText("WAIT A MOMENT");
                        } else {
                            status.setText("CONNECTED !");
                        }
                        break;
                    case POWER:
                    case RELOADING:
                        status.setText("CONNECT");
                        break;
                    case LOADING:
                        status.setText("CONNECTING...");
                        break;
                    case HIDDEN:
                        status.setText("");
                }
            }
        });

        powerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (powerView.getCurrentState() == AXPowerView.State.HIDDEN) {
                    powerView.setState(AXPowerView.State.POWER);
                } else if (powerView.getCurrentState() == AXPowerView.State.POWER) {
                    powerView.setState(AXPowerView.State.LOADING);
                } else if (powerView.getCurrentState() == AXPowerView.State.LOADING) {
                    powerView.setState(AXPowerView.State.SUCCEED);
                } else {
                    powerView.setState(AXPowerView.State.POWER);
                }
            }
        });

        powerView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (powerView.getCurrentState() == AXPowerView.State.LOADING){
                    powerView.setState(AXPowerView.State.POWER);
                    return true;
                }
                return false;
            }
        });

    }
}