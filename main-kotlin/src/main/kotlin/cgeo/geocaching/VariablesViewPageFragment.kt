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

package cgeo.geocaching

import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.activity.TabbedViewPagerFragment
import cgeo.geocaching.databinding.CachedetailVariablesPageBinding
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.VariableListView
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.formulas.FormulaUtils
import cgeo.geocaching.utils.formulas.VariableMap

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils

class VariablesViewPageFragment : TabbedViewPagerFragment()<CachedetailVariablesPageBinding> {

    private CacheDetailActivity activity
    private Geocache cache
    private VariableListView.VariablesListAdapter adapter

    private var previousVarSize: Int = -1

    override     public CachedetailVariablesPageBinding createView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        val binding: CachedetailVariablesPageBinding = CachedetailVariablesPageBinding.inflate(inflater, container, false)
        val varList: VariableListView = binding.variables
        this.adapter = varList.getAdapter()
        adapter.setDisplay(VariableListView.DisplayType.ADVANCED, 1)
        adapter.setChangeCallback(v -> {
            binding.variablesAddnextchar.setEnabled(null != getAddNextChar())
            binding.variablesAddnextchar.setText(getAddNextCharText())
            if (activity != null && (previousVarSize < 0 || previousVarSize != v.size())) {
                activity.reinitializePage(-1); //this just reinits the title bar, not the variable tab content
                previousVarSize = v.size()
                cache.recalculateWaypoints()
            }
        })

        binding.variablesAdd.setOnClickListener(d -> {
            binding.variables.clearFocus()
            adapter.selectVariableName(null, (o, n) -> adapter.addVariable(n, ""))
        })
        binding.variablesAddscan.setOnClickListener(d -> {
            binding.variables.clearFocus()
            scanCache()
        })
        binding.variablesTidyup.setOnClickListener(d -> {
            binding.variables.clearFocus()
            adapter.tidyUp(null)
        })

        binding.variablesDeleteall.setOnClickListener(d -> {
            binding.variables.clearFocus()
            if (!adapter.getVariables().isEmpty()) {
                SimpleDialog.of(activity).setTitle(TextParam.id(R.string.variables_deleteall))
                        .setMessage(TextParam.id(R.string.variables_deleteall_confirm_text)).confirm(() -> adapter.clearAllVariables())
            }
        })

        binding.variablesAddnextchar.setEnabled(null != getAddNextChar())
        binding.variablesAddnextchar.setText(getAddNextCharText())
        binding.variablesAddnextchar.setOnClickListener(d -> {
            val lmc: Character = adapter.getVariables().getLowestMissingChar()
            if (lmc != null) {
                adapter.addVariable("" + lmc, null)
            }
        })

        binding.variablesInfo.setOnClickListener(d -> ShareUtils.openUrl(
                this.getContext(), LocalizationUtils.getString(R.string.formula_syntax_url), false))

        binding.chipCompletedVariables.setChecked(!Settings.getHideCompletedVariables())
        binding.chipCompletedVariables.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Settings.setHideCompletedVariables(!isChecked)
            activity.reinitializePage(getPageId())
        })

        return binding
    }

    private Character getAddNextChar() {
        if (adapter == null || adapter.getVariables() == null) {
            return null
        }
        return adapter.getVariables().getLowestMissingChar()
    }

    private String getAddNextCharText() {
        val nextChar: Character = getAddNextChar()
        return nextChar == null ? "-" : "" + nextChar
    }

    override     public Long getPageId() {
        return CacheDetailActivity.Page.VARIABLES.id
    }

    @SuppressLint("NotifyDataSetChanged")
    override     public Unit setContent() {

        checkUnsavedChanges()

        // retrieve activity and cache - if either if this is null, something is really wrong...
        this.activity = (CacheDetailActivity) getActivity()
        if (activity == null) {
            return
        }
        this.cache = activity.getCache()
        if (cache == null) {
            return
        }
        updateVariableList(cache)

        //register for changes of variableslist -> calculated waypoints may have changed
        cache.getVariables().addChangeListener(this, s -> activity.runOnUiThread(() -> updateVariableList(cache)))
        binding.getRoot().setVisibility(View.VISIBLE)
    }

    private Unit updateVariableList(final Geocache cache) {
        adapter.setVariableList(cache.getVariables())
        adapter.setVisibleVariables()
        adapter.sortVariables(TextUtils.COLLATOR::compare)
    }

    private Unit checkUnsavedChanges() {
        if (cache != null && cache.getVariables().wasModified()) {
            //make call asynchronous due to possible fragment transaction conflicts, see #12977
            binding.getRoot().post(() -> {
                cache.getVariables().saveState()
                activity.ensureSaved()
            })
        }
    }

    private Unit scanCache() {
        val toScan: List<String> = ArrayList<>()
        toScan.add(TextUtils.stripHtml(activity.getCache().getDescription()))
        toScan.add(activity.getCache().getHint())
        for (Waypoint w : activity.getCache().getWaypoints()) {
            toScan.add(w.getNote())
        }
        val existingFormulas: List<String> = ArrayList<>()
        for (VariableMap.VariableState state : adapter.getItems()) {
            if (state != null && !StringUtils.isBlank(state.getFormulaString())) {
                existingFormulas.add(state.getFormulaString())
            }
        }
        val patterns: List<String> = FormulaUtils.scanForFormulas(toScan, existingFormulas)
        if (patterns.isEmpty()) {
            ActivityMixin.showShortToast(activity, R.string.variables_scanlisting_nopatternfound)
        } else {
            final SimpleDialog.ItemSelectModel<String> model = SimpleDialog.ItemSelectModel<>()
            model.setItems(patterns).setDisplayMapper((s) -> TextParam.text("`" + s + "`").setMarkdown(true))

            SimpleDialog.of(activity).setTitle(TextParam.id(R.string.variables_scanlisting_choosepattern_title))
                    .selectMultiple(model, set -> {
                        for (String s : set) {
                            adapter.addVariable(null, s)
                        }
                    })
        }
    }

    override     public Unit onPause() {
        super.onPause()
        //called e.g. when user swipes away from variables tab
        checkUnsavedChanges()
    }

    override     public Unit onDestroyView() {
        super.onDestroyView()
        //called e.g. when user closes cache detail view
        checkUnsavedChanges()
    }

    override     public Unit onDestroy() {
        super.onDestroy()
        //called e.g. when user closes cache detail view (after "onDestroy" is called)
        checkUnsavedChanges()

        if (cache != null) {
            cache.getVariables().removeChangeListener(this)
        }
    }

    override     public Unit onDetach() {
        super.onDetach()
        //called e.g. when user closes cache detail view (after "onDestroyView" is called)
        checkUnsavedChanges()
    }
}
