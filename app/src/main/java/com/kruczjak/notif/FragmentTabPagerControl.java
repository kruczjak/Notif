/**
 *
 */
package com.kruczjak.notif;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * @author Jakub
 */
public class FragmentTabPagerControl extends FragmentPagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

    private final ActionBar mAB;
    private final ViewPager mVP;
    private MessageOverviewFragment MessageOverviewFragment = new MessageOverviewFragment();
    private NotifFragment NotifFragment = new NotifFragment();
    private final Context mContext;

    public FragmentTabPagerControl(SherlockFragmentActivity activity, ViewPager pager) {
        super(activity.getSupportFragmentManager());
        mAB = activity.getSupportActionBar();

        mContext = activity;

        mVP = pager;
        mVP.setAdapter(this);
        mVP.setOnPageChangeListener(this);

        ActionBar.Tab tab = mAB.newTab().setText("Messages");
        tab.setTabListener(this);
        mAB.addTab(tab);

        tab = mAB.newTab().setText("Notifications");
        tab.setTabListener(this);
        mAB.addTab(tab);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentPagerAdapter#getItem(int)
     */
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return MessageOverviewFragment;
        } else {
            return NotifFragment;
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.view.PagerAdapter#getCount()
     */
    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPageSelected(int arg0) {
        mAB.setSelectedNavigationItem(arg0);
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        if (tab.getPosition() == 0) {
            mVP.setCurrentItem(0);
            ((Starter) mContext).unlockDrawer();
        } else {
            mVP.setCurrentItem(1);
            ((Starter) mContext).lockCloseDrawer();
        }
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub

    }

}
