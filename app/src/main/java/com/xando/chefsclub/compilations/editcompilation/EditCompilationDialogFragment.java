package com.xando.chefsclub.compilations.editcompilation;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.xando.chefsclub.FirebaseReferences;
import com.xando.chefsclub.R;
import com.xando.chefsclub.compilations.data.CompilationData;
import com.xando.chefsclub.compilations.upload.CompilationUploader;
import com.xando.chefsclub.dataworkers.ParcResourceByParc;
import com.xando.chefsclub.helper.NetworkHelper;
import com.xando.chefsclub.recipes.data.RecipeData;
import com.xando.chefsclub.repository.CompilationsTransactions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.CompositeDisposable;

import static com.xando.chefsclub.helper.FirebaseHelper.getUid;
import static com.xando.chefsclub.recipes.repository.RecipeRepository.CHILD_RECIPES;


public class EditCompilationDialogFragment extends AppCompatDialogFragment {

    private static final String KEY_RECIPE_DATA = "RECIPE_DATA";
    private static final String KEY_COMPILATION_TO_EDIT = "COMPILATION_TO_EDIT";

    @BindView(R.id.name)
    protected EditText name;

    private RecipeData mRecipeData;

    private CompilationData mCompilationToEdit;

    private CompositeDisposable disposer = new CompositeDisposable();

    public static Bundle getArguments(CompilationData compilationToEdit) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_COMPILATION_TO_EDIT, compilationToEdit);

        return bundle;
    }

    public static Bundle getArguments(RecipeData recipeData) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_RECIPE_DATA, recipeData);

        return bundle;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogFragmentStyle);

        if (getArguments() != null) {
            mRecipeData = getArguments().getParcelable(KEY_RECIPE_DATA);
            mCompilationToEdit = getArguments().getParcelable(KEY_COMPILATION_TO_EDIT);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        String tittle = mCompilationToEdit != null ? "Rename" : "New compilation";

        dialog.setTitle(tittle);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_create_new_compilation, container, false);

        ButterKnife.bind(this, view);

        if (mCompilationToEdit != null) {
            name.setText(mCompilationToEdit.name);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        disposer.dispose();
        super.onDestroy();
    }

    @OnClick(R.id.btn_cancel)
    protected void cancel() {
        dismiss();
    }

    @OnClick(R.id.btn_save)
    protected void save() {
        if (NetworkHelper.isConnected(getActivity())) {
            if (isValidate()) {
                startSave();
            }
        } else {
            Toast.makeText(getActivity(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidate() {
        boolean isNameEmpty = TextUtils.isEmpty(name.getText().toString());

        if (isNameEmpty) {
            name.setError(getString(R.string.error_field_required));
        } else {
            name.setError(null);
        }

        return !isNameEmpty;
    }

    private void startSave() {
        if (mCompilationToEdit != null) {
            mCompilationToEdit.name = name.getText().toString();
        } else {
            mCompilationToEdit = new CompilationData(name.getText().toString(),
                    getUid(), 0);
        }

        new CompilationUploader().start(mCompilationToEdit, resCompTittle -> {
            if (resCompTittle.status == ParcResourceByParc.Status.SUCCESS) {
                if (mRecipeData != null) {
                    startAddToCompilationWithCheck(resCompTittle.data);

                } else onSuccessCreated();
            } else if (resCompTittle.status == ParcResourceByParc.Status.ERROR) {
                onError(resCompTittle.exception);
            }
        });
    }

    private void startAddToCompilationWithCheck(CompilationData compilationData) {
        DatabaseReference ref = FirebaseReferences.getDataBaseReference();

        ref.child(CHILD_RECIPES).child(mRecipeData.recipeKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                RecipeData recipeData = dataSnapshot.getValue(RecipeData.class);

                if (recipeData != null) {
                    startAddToCompilation(compilationData);

                    onSuccessRecipeAdded(compilationData);
                } else {
                    Toast.makeText(getActivity(),
                            "Action is impossible: recipe has been deleted", Toast.LENGTH_SHORT).show();

                    dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void startAddToCompilation(CompilationData data) {
        disposer.add(CompilationsTransactions.INSTANCE.onAddRecipeToCompilation(
                data.compilationKey,
                mRecipeData.recipeKey
        ));
    }

    private void onSuccessRecipeAdded(CompilationData compilationData) {
        Toast.makeText(getActivity(), "Recipe added to \"" + compilationData.name + "\"", Toast.LENGTH_SHORT)
                .show();
        dismiss();
    }

    private void onSuccessCreated() {
        dismiss();
    }

    private void onError(Exception ex) {
        Toast.makeText(getActivity(), ex.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
