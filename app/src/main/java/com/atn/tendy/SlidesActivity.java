package com.atn.tendy;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.atn.tendy.login.LoginActivity;
import com.atn.tendy.utils.Utils;

public class SlidesActivity extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    ImageView[] dots = new ImageView[3];
    final static float ALPHA_DISABLED = 0.43f;
    final static float ALPHA_FULL = 1f;
    Button gotIt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slides);

        setupDots();

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                for (int i=0;i< dots.length;i++)
                    dots[i].setAlpha(ALPHA_DISABLED);
                dots[position].setAlpha(ALPHA_FULL);
                gotIt.setVisibility(position == 2 ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    public static class SlideFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "slide_number";

        public SlideFragment() {
        }

        public static SlideFragment newInstance(int sectionNumber) {
            SlideFragment fragment = new SlideFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            int slideRes = R.layout.slide1;
            if(getArguments().getInt(ARG_SECTION_NUMBER) == 2)
                slideRes = R.layout.slide2;
            if(getArguments().getInt(ARG_SECTION_NUMBER) == 3)
                slideRes = R.layout.slide3;
            View rootView = inflater.inflate(slideRes, container, false);
            Utils.setFontToViewGroup(getActivity(), (ViewGroup) rootView, "open");
            return rootView;
        }
    }


    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return SlideFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "slide 1";
                case 1:
                    return "slide 2";
                case 2:
                    return "slide 3";
            }
            return null;
        }
    }

    private void setupDots() {
        LinearLayout dotsContainer = (LinearLayout) findViewById(R.id.dotsContainer);
        dots[0] = (ImageView) dotsContainer.findViewById(R.id.dot1);
        dots[1] = (ImageView) dotsContainer.findViewById(R.id.dot2);
        dots[2] = (ImageView) dotsContainer.findViewById(R.id.dot3);
        dots[0].setAlpha(ALPHA_FULL);
        dots[1].setAlpha(ALPHA_DISABLED);
        dots[2].setAlpha(ALPHA_DISABLED);
        gotIt = (Button) findViewById(R.id.gotIt);
        gotIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceManager.getDefaultSharedPreferences(SlidesActivity.this).edit().putBoolean("shouldShowSlides", false).commit();
                startActivity(new Intent(SlidesActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}
