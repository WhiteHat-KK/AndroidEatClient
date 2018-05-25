package com.techbytecare.kk.androideatclient;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.techbytecare.kk.androideatclient.Common.Common;
import com.techbytecare.kk.androideatclient.Database.Database;
import com.techbytecare.kk.androideatclient.Interface.ItemClickListener;
import com.techbytecare.kk.androideatclient.Model.Favourites;
import com.techbytecare.kk.androideatclient.Model.Food;
import com.techbytecare.kk.androideatclient.Model.Order;
import com.techbytecare.kk.androideatclient.ViewHolder.FoodViewHolder;

import java.util.ArrayList;
import java.util.List;

import static com.techbytecare.kk.androideatclient.Common.Common.currentUser;

public class SearchActivity extends AppCompatActivity {

    //search function
    FirebaseRecyclerAdapter<Food,FoodViewHolder> searchAdapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseRecyclerAdapter<Food,FoodViewHolder> adapter;

    FirebaseDatabase database;
    DatabaseReference foodList;

    Database localDB;

    //facebook share
    CallbackManager callbackManager;
    ShareDialog shareDialog;

    Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            //create photo frm bitmap
            SharePhoto photo = new SharePhoto.Builder().setBitmap(bitmap).build();

            if (ShareDialog.canShow(SharePhotoContent.class))   {
                SharePhotoContent content = new SharePhotoContent.Builder().addPhoto(photo).build();

                shareDialog.show(content);
            }
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        //init facebook
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);

        database = FirebaseDatabase.getInstance();
        foodList = database.getReference("Foods");

        localDB = new Database(this);

        recyclerView = (RecyclerView)findViewById(R.id.recycler_search);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        materialSearchBar = (MaterialSearchBar)findViewById(R.id.searchBar);
        materialSearchBar.setHint("Enter the Food");

        loadSuggest();

        materialSearchBar.setCardViewElevation(10);

        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                List<String> suggest = new ArrayList<String>();
                for (String search: suggestList){
                    if(search.toLowerCase().contains(materialSearchBar.getText().toLowerCase())){
                        suggest.add(search);
                    }
                }
                materialSearchBar.setLastSuggestions(suggest);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                // When Search Bar is close
                // Restore original suggest adapter
                if (!enabled){
                    recyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                // When Search finish
                // Show result of search adapter
                startSearch(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });

        loadAllFoods();
    }

    private void loadAllFoods() {
        Query searchByName = foodList;

        FirebaseRecyclerOptions<Food> foodOptions = new FirebaseRecyclerOptions.Builder<Food>()
                .setQuery(searchByName,Food.class).build();

        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(foodOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final FoodViewHolder viewHolder, final int position, @NonNull final Food model) {
                viewHolder.food_name.setText(model.getName());
                viewHolder.food_price.setText(String.format("$ %s",model.getPrice().toString()));

                Picasso.with(getBaseContext()).load(model.getImage()).into(viewHolder.food_image);

                //quick cart
                final boolean isExists = new Database(getBaseContext()).checkFoodExist(adapter.getRef(position).getKey(), currentUser.getPhone());

                viewHolder.quick_cart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (!isExists) {

                            new Database(getBaseContext()).addToCart(new Order(
                                    currentUser.getPhone(),
                                    adapter.getRef(position).getKey(),
                                    model.getName(),
                                    "1",
                                    model.getPrice(),
                                    model.getDiscount(),
                                    model.getImage()));
                        }
                        else    {
                            new Database(getBaseContext()).increaseCart(Common.currentUser.getPhone(),adapter.getRef(position).getKey());
                        }
                        Toast.makeText(SearchActivity.this, "Added To Cart", Toast.LENGTH_SHORT).show();
                    }
                });

                //add favourites
                if (localDB.isFavourites(adapter.getRef(position).getKey(),Common.currentUser.getPhone()))    {
                    viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);
                }

                //click to share
                viewHolder.share_image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Picasso.with(getBaseContext()).load(model.getImage()).into(target);
                    }
                });

                //click to change status of the favourite
                viewHolder.fav_image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Favourites favourites = new Favourites();
                        favourites.setFoodId(adapter.getRef(position).getKey());
                        favourites.setFoodName(model.getName());
                        favourites.setFoodDescription(model.getDescription());
                        favourites.setFoodDiscount(model.getDiscount());
                        favourites.setFoodImage(model.getImage());
                        favourites.setFoodPrice(model.getPrice());
                        favourites.setFoodMenuId(model.getMenuId());
                        favourites.setUserPhone(Common.currentUser.getPhone());

                        if (!localDB.isFavourites(adapter.getRef(position).getKey(), currentUser.getPhone()))   {

                            localDB.addToFavourites(favourites);
                            viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);

                            Toast.makeText(SearchActivity.this, ""+model.getName()+" was added to Favourites", Toast.LENGTH_SHORT).show();
                        }
                        else    {
                            localDB.removeFromFavourites(adapter.getRef(position).getKey(), currentUser.getPhone());
                            viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                            Toast.makeText(SearchActivity.this, ""+model.getName()+" was Removed From Favourites", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                final Food local = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Toast.makeText(FoodList.this, ""+local.getName(), Toast.LENGTH_SHORT).show();

                        //start the activity
                        Intent foodDetail = new Intent(SearchActivity.this,FoodDetail.class);
                        foodDetail.putExtra("FoodId",adapter.getRef(position).getKey());    //send food to new activity
                        startActivity(foodDetail);
                    }
                });
            }

            @Override
            public FoodViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemViw = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_item,parent,false);

                return new FoodViewHolder(itemViw);
            }
        };
        //set adapter
        adapter.startListening();
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
    }

    private void startSearch(CharSequence text) {
        Query searchByName = foodList.orderByChild("name").equalTo(text.toString());

        FirebaseRecyclerOptions<Food> foodOptions = new FirebaseRecyclerOptions.Builder<Food>()
                .setQuery(searchByName,Food.class).build();

        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(foodOptions) {
            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder viewHolder, int position, @NonNull final Food model) {

                viewHolder.food_name.setText(model.getName());
                Picasso.with(getBaseContext()).load(model.getImage()).into(viewHolder.food_image);

                final Food local = model;

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Toast.makeText(FoodList.this, "" + model.getName(), Toast.LENGTH_SHORT).show();
                        // start Food detail activity
                        Intent intentFoodDetail = new Intent(SearchActivity.this, FoodDetail.class);
                        intentFoodDetail.putExtra("FoodId", searchAdapter.getRef(position).getKey());

                        startActivity(intentFoodDetail);
                    }
                });
            }

            @Override
            public FoodViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_item,parent,false);

                return new FoodViewHolder(itemView);
            }
        };
        searchAdapter.startListening();
        recyclerView.setAdapter(searchAdapter);
    }

    @Override
    protected void onStop() {
        if (adapter != null)    {
            adapter.stopListening();
        }
        if (searchAdapter != null)  {
            searchAdapter.stopListening();
        }
        super.onStop();
    }

    private void loadSuggest() {
        foodList.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for(DataSnapshot postSnapshot : dataSnapshot.getChildren()){
                    Food item = postSnapshot.getValue(Food.class);
                    suggestList.add(item.getName());
                }
                materialSearchBar.setLastSuggestions(suggestList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
