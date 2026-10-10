package cgeo.geocaching.log;

import cgeo.geocaching.databinding.LogsItemBinding;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class LogViewHolder extends RecyclerView.ViewHolder {
    final LogsItemBinding binding;

    LogViewHolder(final @NonNull LogsItemBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

}
