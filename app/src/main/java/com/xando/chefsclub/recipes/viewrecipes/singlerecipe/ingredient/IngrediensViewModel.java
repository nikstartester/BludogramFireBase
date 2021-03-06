package com.xando.chefsclub.recipes.viewrecipes.singlerecipe.ingredient;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.xando.chefsclub.shoppinglist.db.IngredientEntity;

import java.util.List;


public class IngrediensViewModel extends AndroidViewModel {

    private MutableLiveData<List<IngredientEntity>> mIngrLiveData;

    private final IngredientsRepository mRepository;

    public IngrediensViewModel(@NonNull Application application) {
        super(application);

        mRepository = new IngredientsRepository(getApplication());
    }

    public void loadData(String recipeId) {
        mRepository.loadDataFromDb(mIngrLiveData, recipeId);
    }

    public MutableLiveData<List<IngredientEntity>> getData() {
        if (mIngrLiveData == null) mIngrLiveData = new MutableLiveData<>();

        return mIngrLiveData;
    }

    public void setData(List<IngredientEntity> data) {
        if (data == null) mIngrLiveData = new MutableLiveData<>();

        mIngrLiveData.setValue(data);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        mRepository.clear();
    }
}
