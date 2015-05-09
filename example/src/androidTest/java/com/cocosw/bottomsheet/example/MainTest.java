package com.cocosw.bottomsheet.example;

import android.test.ActivityInstrumentationTestCase2;

public class MainTest extends ActivityInstrumentationTestCase2<Main> {

    public MainTest() {
        super(Main.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testDisplayed() {
    //    onView(withId(R.id.listView)).check(matches(isDisplayed()));
    }


    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
}