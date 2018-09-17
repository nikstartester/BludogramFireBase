package com.example.nikis.bludogramfirebase.UserCookBook;

import com.example.nikis.bludogramfirebase.Recipe.ViewRecipe.RecipesListFragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;


public class UserFavoriteRecipes extends RecipesListFragment {
    @Override
    public Query getQuery(DatabaseReference databaseReference) {
        return databaseReference.child("recipes").orderByChild("stars/" + getUid()).equalTo(true);
    }
}
