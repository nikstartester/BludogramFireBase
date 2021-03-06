package com.xando.chefsclub.profiles.editprofile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.xando.chefsclub.FirebaseReferences;
import com.xando.chefsclub.R;
import com.xando.chefsclub.basescreen.fragment.BaseFragmentWithImageChoose;
import com.xando.chefsclub.constants.Constants;
import com.xando.chefsclub.dataworkers.BaseRepository;
import com.xando.chefsclub.dataworkers.ParcResourceByParc;
import com.xando.chefsclub.helper.FirebaseHelper;
import com.xando.chefsclub.helper.MatisseHelper;
import com.xando.chefsclub.helper.NetworkHelper;
import com.xando.chefsclub.image.data.ImageData;
import com.xando.chefsclub.image.loaders.GlideImageLoader;
import com.xando.chefsclub.image.viewimages.ViewImagesActivity;
import com.xando.chefsclub.main.MainActivity;
import com.xando.chefsclub.profiles.data.ProfileData;
import com.xando.chefsclub.profiles.upload.ProfileUploaderService;
import com.xando.chefsclub.profiles.upload.exception.ExistLoginException;
import com.xando.chefsclub.profiles.viewmodel.ProfileViewModel;
import com.xando.chefsclub.settings.SettingsCacheFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.xando.chefsclub.constants.Constants.Login.KEY_IS_ALREADY_REGISTERED;


public class EditProfileFragment extends BaseFragmentWithImageChoose implements View.OnClickListener {

    public static final int MIN_CHAR_FOR_LOGIN = 5;
    private static final String KEY_GENDER = "gender";
    private static final String KEY_IMAGE_PATH = "imagePath";
    private static final String KEY_IS_IN_PROGRESS = "isProgress";
    @BindView(R.id.edt_firstName)
    protected EditText edtFirstName;

    @BindView(R.id.edt_lastName)
    protected EditText edtLastName;

    @BindView(R.id.edt_login)
    protected EditText edtLogin;

    @BindView(R.id.radBtn_male)
    protected RadioButton rdbMale;

    @BindView(R.id.radBtn_female)
    protected RadioButton rdnFemale;

    @BindView(R.id.circularImageView)
    protected ImageView circularImageView;

    @BindView(R.id.filter)
    protected RelativeLayout filterForProgress;

    @BindView(R.id.progress_login)
    protected ProgressBar progressLogin;

    private String mLastLoginToCheck = "";

    private String gender;

    private boolean isInProgress;

    private ProfileUploaderBroadcastReceiver mBroadcastReceiver;

    private String mImagePath;

    private OnSuccessListener mOnSuccessListener;

    private ProfileViewModel mProfileViewModel;

    public static boolean isProfileAlreadyCreated(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.Settings.APP_PREFERENCES,
                Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_IS_ALREADY_REGISTERED, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mImagePath = savedInstanceState.getString(KEY_IMAGE_PATH);
            gender = savedInstanceState.getString(KEY_GENDER);
        } else {
            gender = ProfileData.GENDER_NONE;
        }

        mProfileViewModel = ViewModelProviders.of(getActivity()).get(ProfileViewModel.class);

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        ButterKnife.bind(this, v);

        rdbMale.setOnClickListener(this);
        rdnFemale.setOnClickListener(this);
        circularImageView.setOnClickListener(this);

        if (savedInstanceState != null) {
            setImage(mImagePath, Constants.ImageConstants.DEF_TIME);

            isInProgress = savedInstanceState.getBoolean(KEY_IS_IN_PROGRESS);

            if (isInProgress) {
                showProgress();
            }
        }

        IntentFilter intentFilter = new IntentFilter(
                ProfileUploaderService.ACTION_RESPONSE);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        getActivity().registerReceiver(mBroadcastReceiver = new ProfileUploaderBroadcastReceiver(),
                intentFilter);

        mProfileViewModel.getResourceLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource != null) {
                if (resource.status == ParcResourceByParc.Status.SUCCESS) {
                    onSuccessLoaded(resource);
                }
            }
        });
        if (mProfileViewModel.getResourceLiveData().getValue() == null)
            mProfileViewModel.loadDataWithSaver(FirebaseHelper.getUid());

        edtLogin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= MIN_CHAR_FOR_LOGIN && !s.toString().equals(mLastLoginToCheck)) {
                    startCheckLogin();

                    mLastLoginToCheck = s.toString();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnSuccessListener) {
            mOnSuccessListener = (OnSuccessListener) context;
        }
    }

    private void onSuccessLoaded(ParcResourceByParc<ProfileData> resource) {
        ProfileData profileData = resource.data;
        if (profileData != null) {
            setData(profileData);
            if (profileData.imageURL != null) {
                setImage(profileData.imageURL, profileData.lastTimeUpdate);

                mImagePath = profileData.imageURL;
            }
        }
    }

    private void setData(ProfileData profileData) {
        edtLogin.setText(profileData.login);
        edtFirstName.setText(profileData.firstName);
        edtLastName.setText(profileData.secondName);

        if (profileData.gender.equals(ProfileData.GENDER_MALE)) {
            radioButtonMaleClick();
        } else {
            radioButtonFemaleClick();
        }
    }

    private void startCheckLogin() {
        showLoginProgress();

        DatabaseReference ref = FirebaseReferences.getDataBaseReference();

        String login = edtLogin.getText().toString().toLowerCase();

        ref.child("users").orderByChild("loginLowerCase").equalTo(login)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<ProfileData> profileDataList = new ArrayList<>(1);

                        for (DataSnapshot snap : dataSnapshot.getChildren()) {
                            profileDataList.add(snap.getValue(ProfileData.class));
                        }

                        ProfileData profileWithExistLogin = profileDataList.size() > 0 ? profileDataList.get(0) : null;

                        if (profileWithExistLogin == null
                                || (FirebaseHelper.getUid() != null
                                && profileWithExistLogin.userUid.equals(FirebaseHelper.getUid()))) {

                            //nothing
                        } else {
                            setLoginExistError();
                        }

                        hideLoginProgress();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_done) {
            startEditProfile();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.radBtn_male:
                radioButtonMaleClick();
                break;
            case R.id.radBtn_female:
                radioButtonFemaleClick();
                break;
            case R.id.circularImageView:
                //super.startMatisseGallery(1);
                super.showChooseDialog(1, isHavePhoto());
                break;
        }
    }

    private boolean isHavePhoto() {
        return mImagePath != null;
    }

    private void showProgress() {
        isInProgress = true;
        if (filterForProgress != null) {
            filterForProgress.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgress() {
        isInProgress = false;
        if (filterForProgress != null) {
            filterForProgress.setVisibility(View.INVISIBLE);
        }
    }

    private void radioButtonMaleClick() {
        rdbMale.setChecked(true);
        rdnFemale.setChecked(false);
        gender = ProfileData.GENDER_MALE;
    }

    private void radioButtonFemaleClick() {
        rdbMale.setChecked(false);
        rdnFemale.setChecked(true);
        gender = ProfileData.GENDER_FEMALE;
    }

    @Override
    protected String getParentDirectoryPath() {
        return Constants.Files.getDirectoryForEditProfileImages(getActivity());
    }

    @Override
    protected void onGalleryFinish(List<Uri> selected) {
        super.addToDeleteIfCapture(mImagePath);

        mImagePath = MatisseHelper.getRealPathFromURIPath(getActivity(), selected.get(0));

        setImage(mImagePath, Constants.ImageConstants.DEF_TIME);

        super.deleteOldCaptures();
    }

    @Override
    protected void onPreviewImage() {
        startActivity(ViewImagesActivity.getIntent(getContext(), new ImageData(mImagePath, Constants.ImageConstants.DEF_TIME)));
    }

    @Override
    protected void onDeleteImage() {
        super.addToDeleteIfCapture(mImagePath);

        mImagePath = null;

        showEmptyImage();

        super.deleteOldCaptures();
    }

    private void showEmptyImage() {
        circularImageView.setImageResource(Constants.ImageConstants.DRAWABLE_ADD_PHOTO_PLACEHOLDER);
    }

    private void setImage(String imagePath, long lastTimeUpdate) {
        if (imagePath != null) {
            ImageData imageData = new ImageData(imagePath, lastTimeUpdate);

            GlideImageLoader.getInstance().loadNormalCircularImage(getActivity(),
                    circularImageView,
                    imageData);
        } else showEmptyImage();
    }

    private void startEditProfile() {
        if (isValidateForm()) {
            startEdit();
        }
    }

    private boolean isValidateForm() {
        boolean isValidate = false;

        String firstName = edtFirstName.getText().toString();
        String lastName = edtLastName.getText().toString();
        String login = edtLogin.getText().toString();

        String requiredError = getResources().getString(R.string.error_field_required);

        if (TextUtils.isEmpty(firstName)) {
            edtFirstName.setError(requiredError);
        } else {
            edtFirstName.setError(null);
            isValidate = true;
        }

        if (TextUtils.isEmpty(lastName)) {
            edtLastName.setError(requiredError);
            isValidate = false;
        } else {
            edtFirstName.setError(null);
        }

        if (TextUtils.isEmpty(login)) {
            edtLogin.setError(requiredError);
            isValidate = false;
        } else if (login.length() < MIN_CHAR_FOR_LOGIN) {
            edtLogin.setError("Min length is 5 characters");
            isValidate = false;
        } else edtLogin.setError(null);

        if (gender.equals(ProfileData.GENDER_NONE)) {
            indicateToChoiceGender();
            isValidate = false;

        } else {
            removeIndicateToChoiceGender();
        }
        return isValidate;
    }

    private void indicateToChoiceGender() {
        Toast.makeText(getActivity(), "Choose gender!", Toast.LENGTH_LONG).show();
    }

    private void removeIndicateToChoiceGender() {

    }

    private void startEdit() {
        if (NetworkHelper.isConnected(getActivity())) {
            ProfileData profileData = createProfileData();
            if (profileData != null) {
                getActivity().startService(ProfileUploaderService.getIntent(getActivity(), profileData));
            } else {
                internetErrorSnackbar();
            }
        } else {
            internetErrorSnackbar();
        }

    }

    private void internetErrorSnackbar() {
        String error = getString(R.string.network_error);
        Snackbar.make(edtLogin, error, Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.try_again), (v) -> {
                    startEdit();
                })
                .show();
    }

    private ProfileData createProfileData() {
        if (mProfileViewModel == null || mProfileViewModel.getResourceLiveData().getValue() == null)
            return null;

        ParcResourceByParc<ProfileData> res = mProfileViewModel.getResourceLiveData().getValue();

        ProfileData profileData = null;

        if (res.status == ParcResourceByParc.Status.SUCCESS) {
            profileData = res.data;

            profileData.firstName = edtFirstName.getText().toString();
            profileData.secondName = edtLastName.getText().toString();
            profileData.login = edtLogin.getText().toString();
            profileData.gender = gender;
            profileData.localImagePath = mImagePath;

            profileData.imageURL = null;
        } else if (res.status == ParcResourceByParc.Status.ERROR
                && res.exception instanceof BaseRepository.NothingFoundFromServerException) {

            String firstName = edtFirstName.getText().toString();
            String lastName = edtLastName.getText().toString();
            String login = edtLogin.getText().toString();

            profileData = new ProfileData(firstName, lastName, login, gender, mImagePath);
        }

        return profileData;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_IMAGE_PATH, mImagePath);
        outState.putString(KEY_GENDER, gender);
        outState.putBoolean(KEY_IS_IN_PROGRESS, isInProgress);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.only_done_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    public void onSuccessUploaded(boolean isAlreadyCreated) {

        SettingsCacheFragment.deleteDir(new File(Constants.Files.getDirectoryForEditProfileImages(getActivity())));

        if (!isAlreadyCreated) {

            SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Constants.Settings.APP_PREFERENCES,
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean(KEY_IS_ALREADY_REGISTERED, true);

            editor.apply();

            startMainActivity();

        } else getActivity().onBackPressed();
    }

    public void onErrorUploaded(ParcResourceByParc<ProfileData> resource) {
        if (resource.exception instanceof ExistLoginException) {
            setLoginExistError();

            hideLoginProgress();
        }
    }

    private void setLoginExistError() {
        edtLogin.setError(getString(R.string.login_already_exist));
    }

    @VisibleForTesting
    private void showLoginProgress() {
        //progressLogin.setVisibility(View.VISIBLE);

        //nothing
    }

    @VisibleForTesting
    private void hideLoginProgress() {
        //progressLogin.setVisibility(View.INVISIBLE);

        //nothing
    }

    private void startMainActivity() {
        Intent intent = new Intent(getActivity(),
                MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        getActivity().finish();
    }

    interface OnSuccessListener {
        void onSuccess();
    }

    private class ProfileUploaderBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ParcResourceByParc<ProfileData> dataResource = intent.getParcelableExtra(ProfileUploaderService.EXTRA_RESOURCE);
            if (dataResource != null && dataResource.status == ParcResourceByParc.Status.SUCCESS) {
                onSuccess(dataResource);
            } else if (dataResource != null && dataResource.status == ParcResourceByParc.Status.LOADING) {
                showProgress();
            } else if (dataResource != null && dataResource.status == ParcResourceByParc.Status.ERROR) {
                onError(dataResource);
            } else {
                hideProgress();
            }
        }

        private void onError(ParcResourceByParc<ProfileData> dataResource) {
            hideProgress();

            EditProfileFragment.this.onErrorUploaded(dataResource);
        }

        private void onSuccess(ParcResourceByParc<ProfileData> resource) {
            hideProgress();

            if (mOnSuccessListener != null) {
                mOnSuccessListener.onSuccess();
            }
            EditProfileFragment.this.onSuccessUploaded(isProfileAlreadyCreated(getActivity()));
        }
    }
}
