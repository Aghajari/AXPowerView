# AXPowerView
 An Android PowerView with awesome animations and circular loading, Inspired by HotspotShieldVPN.
 
<img src="./preview.gif" width=300 title="AXPowerView">

## Installation

AXPowerView is available in the JCenter, so you just need to add it as a dependency (Module gradle)

Gradle
```gradle
implementation 'com.aghajari.powerview:AXPowerView:1.0.0'
```

Maven
```xml
<dependency>
  <groupId>com.aghajari.powerview</groupId>
  <artifactId>AXPowerView</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```

# Usage
add the AXPowerView to your layout:

```xml
<com.aghajari.powerview.AXPowerView
    android:id="@+id/powerView"
    android:layout_width="184dp"
    android:layout_height="wrap_content"
    android:layout_gravity="center"
    app:autoStart="true"
    app:color="#55A4F1"
    app:innerColor="@android:color/white"
    app:state="POWER" />
```

That's all you need! If you don't want the AXPowerView to automatically start animating, omit the app:autoStart option and start it manually yourself:

```java
AXPowerView powerView = findViewById(R.id.powerView);
powerView.setState(AXPowerView.State.POWER);
```

*States : HIDDEN | POWER | LOADING | SUCCED*

## XML attributes

| Name | Type | Default | Description |
|:----:|:----:|:-------:|:-----------:|
| color | color | #55A4F1 | color of the main circle |
| innerColor | color | black | color of the innerView |
| thickness | dimension | 4dp | thickness of the main circle |
| innerThickness | color | 3dp | thickness of the innerView |
| indeterminateDuration | integer | 600 | loading animation duration |
| delay | integer | 80 | loading animation delay |
| showDuration | integer | 400 | showing animation duration |
| succeedDuration | integer | 400 | succeed animation duration |
| autoStart | boolean | true | Whether the view should automatically start animating once it is initialized. |
| firstAnimation | boolean | true | Whether the view should load current state without animation for first time |
| innerViewEnabled | boolean | true | Whether the view should draw innerView |
| state | enum | HIDDEN | current view's state (HIDDEN\|POWER\|LOADING\|SUCCED) |

## Public Methods

| Name | Description |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| setState(AXPowerView.State) | Sets current AXPowerView's state |
| setState(AXPowerView.State,boolean) | Sets current AXPowerView's state |
| getCurrentState() | Returns the current AXPowerView's state |
| getNextState() | Returns the next AXPowerView's state |
| isAnimationRunning() | Check whether animation is running |
| setAutoStart(boolean) | Sets whether the view should automatically start animating once it is initialized. |
| isAnimationRunning() | Check whether the view should automatically start animating once it is initialized. |
| isInnerViewEnabled() | Check whether the innerView is enabled |
| setInnerViewEnabled(boolean) | Sets the innerView enabled |
| setColor(int) | Sets color of the main circle |
| getColor() | Gets color of the main circle |
| setInnerColor(int) | Sets color of the innerView |
| getInnerColor() | Gets color of the innerView |
| setThickness(float) | Sets thickness of the main circle |
| getThickness() | Gets thickness of the main circle |
| setInnerThickness(float) | Sets thickness of the innerView |
| getInnerThickness() | Gets thickness of the innerView |
| setIndeterminateDuration(long) | Sets loading animation duration |
| getIndeterminateDuration() | Gets loading animation duration |
| setShowDuration(long) | Sets showing animation duration |
| getShowDuration() | Gets showing animation duration |
| setSucceedDuration(long) | Sets succeed animation duration |
| getSucceedDuration() | Gets succeed animation duration |
| setDelay(long) | Sets loading animation delay |
| getDelay() | Gets loading animation delay |
| setAnimatorListener(AXPowerView.AnimatorListener) | Registers an AnimatorListener with this view. |

## Listener Events

| Event | Description |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| onAnimationEnded(State currentState, State nextState) | Called when the last animation finished  |
| onStateChanged(State from, State to,boolean animationLoaded) | Called when the state changed |

## Author 
- **Amir Hossein Aghajari**

License
=======

    Copyright 2020 Amir Hossein Aghajari
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


<br><br>
<div align="center">
  <img width="64" alt="LCoders | AmirHosseinAghajari" src="https://user-images.githubusercontent.com/30867537/90538314-a0a79200-e193-11ea-8d90-0a3576e28a18.png">
  <br><a>Amir Hossein Aghajari</a> • <a href="mailto:amirhossein.aghajari.82@gmail.com">Email</a> • <a href="https://github.com/Aghajari">GitHub</a>
</div>
