package com.xando.chefsclub.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.xando.chefsclub.R;
import com.xando.chefsclub.SingleChangingFragmentActivity;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SettingsActivity extends SingleChangingFragmentActivity implements SettingsFragment.ChangeFragment {

    @BindView(R.id.toolbar)
    protected Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(v -> super.onBackPressed());

        if (savedInstanceState == null) {
            super.setStartFragment(new SettingsFragment(), "Settings");
        } else {
            getSupportActionBar().setTitle(super.getCurrentTitle());
        }
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_fragment;
    }

    @Override
    public void toChangeFragment(@NonNull Fragment fragment, String title) {
        super.changeFragment(fragment, title);
    }
}
