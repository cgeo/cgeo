// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.helper

import cgeo.geocaching.R
import cgeo.geocaching.databinding.UsefulappsItemBinding
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.NonNull
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView

import java.util.Arrays
import java.util.List

class HelperAppAdapter : RecyclerView().Adapter<HelperAppAdapter.ViewHolder> {

    private final List<HelperApp> helperApps
    private final HelperAppClickListener clickListener
    private final Context context

    protected static class ViewHolder : AbstractRecyclerViewHolder() {
        private final UsefulappsItemBinding binding

        ViewHolder(final View rowView) {
            super(rowView)
            binding = UsefulappsItemBinding.bind(rowView)
        }
    }

    HelperAppAdapter(final Context context, final HelperApp[] objects, final HelperAppClickListener clickListener) {
        this.context = context
        this.clickListener = clickListener
        helperApps = Arrays.asList(objects)
    }

    override     public Int getItemCount() {
        return helperApps.size()
    }

    override     public ViewHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
        val view: View = LayoutInflater.from(parent.getContext()).inflate(R.layout.usefulapps_item, parent, false)
        val viewHolder: ViewHolder = ViewHolder(view)
        viewHolder.itemView.setOnClickListener(view1 -> {
            val app: HelperApp = helperApps.get(viewHolder.getAdapterPosition())
            clickListener.onClickHelperApp(app)
        })

        return viewHolder
    }

    override     public Unit onBindViewHolder(final ViewHolder holder, final Int position) {
        val app: HelperApp = helperApps.get(position)
        val resources: Resources = context.getResources()
        holder.binding.title.setText(resources.getString(app.titleId))
        holder.binding.image.setImageDrawable(ResourcesCompat.getDrawable(resources, app.iconId, null))
        holder.binding.description.setText(HtmlCompat.fromHtml(resources.getString(app.descriptionId), HtmlCompat.FROM_HTML_MODE_LEGACY))
    }

}

