package cgeo.geocaching;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cgeo.geocaching.activity.Keyboard;

public

class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    private List<SearchActivityCard> data;
    private SearchActivity activity;

    public CardAdapter(final SearchActivity activity, final List<SearchActivityCard> data) {
        this.activity = activity;
        this.data = data;
    }

    @NonNull
    @Override
    public CardAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.search_card, parent, false);
        return new CardAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final CardAdapter.ViewHolder holder, final  int p) {
        SearchActivityCard card = data.get(p);

        holder.textView.setText(card.getTitle());
        holder.imageView.setImageDrawable(AppCompatResources.getDrawable(activity, card.getIcon()));
        holder.cardView.setOnClickListener(v -> {
            activity.setTitle(activity.getString(R.string.search) + ": " + activity.getString(card.getTitle()));
            activity.ACTIVITY_USED = true;

            View searchActivity = (((View)((View)v.getParent()).getParent()));

            View ll = searchActivity.findViewById(card.getSearchFieldId());

            Button searchButton = ll.findViewWithTag("searchButton");
            if (searchButton != null) {
                searchButton.performClick();
                if (searchButton.getId() == R.id.search_filter) {
                    return;
                }
            }

            View cards = searchActivity.findViewById(R.id.recyclerView);
            cards.setVisibility(View.GONE);
            ll.setVisibility(View.VISIBLE);
            searchActivity.requestLayout();

            AutoCompleteTextView searchField = ll.findViewWithTag("searchField");
            if (searchField != null) {
                searchField.setSelection(searchField.getText().length());
                Keyboard.show(activity, searchField);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imageView;
        TextView textView;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            imageView = itemView.findViewById(R.id.icon);
            textView = itemView.findViewById(R.id.text);
        }
    }
}
