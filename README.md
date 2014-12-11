BottomSheet
=======

One way to present a set of actions to a user is with bottom sheets, a sheet of paper that slides up from the bottom edge of the screen. Bottom sheets offer flexibility in the display of clear and simple actions that do not need explanation.

https://www.google.com/design/spec/components/bottom-sheets.html

This library works on android 2.1+

![Sample](https://github.com/soarcn/BottomSheet/blob/master/art/image.png?raw=true)
![Sample](https://github.com/soarcn/BottomSheet/blob/master/art/image1.png?raw=true)
![Sample](https://github.com/soarcn/BottomSheet/blob/master/art/image2.png?raw=true)
![Sample](https://github.com/soarcn/BottomSheet/blob/master/art/image3.png?raw=true)
![Sample](https://github.com/soarcn/BottomSheet/blob/master/art/image4.png?raw=true)
![Sample](https://github.com/soarcn/BottomSheet/blob/master/art/image5.png?raw=true)

How to use this library
=======

- Download this library, import to your IDE (eclipse...) as a library project.
- Using Gradle

```groovy
    compile 'com.cocosw:bottomsheet:0.+@aar' 
```
- Using Maven

```xml
<dependency>
    <groupId>com.cocosw</groupId>
    <artifactId>bottomsheet</artifactId>
    <version>0.x</version>
    <type>apklib</type>
</dependency>
```

- Compile you project with android sdk v21

API
=======

- Define actions in menu xml

```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@+id/share" android:title="@string/share" android:icon="@drawable/perm_group_messages"/>
    <item android:id="@+id/upload" android:title="@string/upload" android:icon="@drawable/perm_group_system_clock"/>
    <item android:id="@+id/call" android:title="@string/call" android:icon="@drawable/perm_group_phone_calls"/>
    <divider/>
    <item android:id="@+id/help" android:title="@string/help" android:icon="@drawable/perm_group_system_tools"/>
</menu>

```

- Call it just like you call a dialog

```java
new BottomSheet.Builder(this).title("title").sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case R.id.help:
                                q.toast("Help me!");
                                break;
                        }
                    }
                }).show();

```
- You can also add action items in builder.


Style
========

- Invoke darkTheme() to show build-in dark theme
- Or define your style in your application, and call bottomsheet by using new BottomSheet.Builder(this,R.style.BottomSheet_StyleDialog)....

```xml
    <style name="BottomSheet.StyleDialog" parent="BottomSheet.Dialog">
        <item name="android:backgroundDimAmount">0.5</item>
        <item name="android:windowAnimationStyle">@style/BottomSheet.Animation</item>
        <item name="android:textColorPrimary">#DDffffff</item>
        <item name="android:textColorSecondary">#8Affffff</item>
        <item name="android:textColorHint">#42ffffff</item>
        <item name="bs_dialogBackground">@color/abc_search_url_text_normal</item>
        <item name="bs_dividerColor">@color/abc_search_url_text_pressed</item>
        <item name="bs_numColumns">4</item>
        <item name="bs_listStyle">@style/BottomSheet.StyleList</item>
    </style>
```
- Or set bottomesheet style in your activity theme

```xml
    <style name="StyleTheme" parent="Theme.AppCompat">
        <item name="bottomSheetStyle">@style/BottomSheet.StyleDialog</item>
    </style>
```

Contribute
=======

- Feel free to fork it

TODO
=======
- (support) DialogFragment
- swipe up to pull up

License
=======

    Copyright 2011, 2014 Liao Kai

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
