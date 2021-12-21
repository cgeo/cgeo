package cgeo.geocaching;

import cgeo.geocaching.activity.TabbedViewPagerFragment;
import cgeo.geocaching.databinding.CachedetailVariablesPageBinding;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.VariableListView;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.formulas.FormulaUtils;
import cgeo.geocaching.utils.formulas.VariableMap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class VariablesViewPageFragment extends TabbedViewPagerFragment<CachedetailVariablesPageBinding> {

    private CacheDetailActivity activity;
    private Geocache cache;
    private VariableListView.VariablesListAdapter adapter;

    private int previousVarSize = -1;

    @Override
    public CachedetailVariablesPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final CachedetailVariablesPageBinding binding = CachedetailVariablesPageBinding.inflate(inflater, container, false);
        final VariableListView varList = binding.variables;
        this.adapter = varList.getAdapter();
        adapter.setChangeCallback(v -> {
            binding.variablesAddnextchar.setText(getAddNextCharText());
            if (activity != null && (previousVarSize < 0 || previousVarSize != v.size())) {
                activity.reinitializePage(-1); //this just reinits the title bar, not the variable tab content
                previousVarSize = v.size();
            }
        });

        //Experimental warning
        TextParam.text("**Experimental New Feature** \n" +
            "* Expect disruptive changes in the future\n" +
            "* No internationalization yet")
            .setMarkdown(true).applyTo(binding.variablesExperimentalWarning);
        binding.variablesExperimentalWarning.setOnClickListener(d -> {
            if (adapter.getDisplayType() == VariableListView.DisplayType.MINIMALISTIC) {
                adapter.setDisplay(VariableListView.DisplayType.ADVANCED, 1);
            } else {
                adapter.setDisplay(VariableListView.DisplayType.MINIMALISTIC, 2);
            }
        });

        binding.variablesAdd.setOnClickListener(d -> {
            binding.variables.clearFocus();
            adapter.selectVariableName(null, (o, n) -> adapter.addVariable(n, ""));
        });
        binding.variablesAddscan.setOnClickListener(d -> {
            binding.variables.clearFocus();
            scanCache();
        });
        binding.variablesTidyup.setOnClickListener(d -> {
            binding.variables.clearFocus();
            adapter.tidyUp(null);
        });

        binding.variablesDeleteall.setOnClickListener(d -> {
            binding.variables.clearFocus();
            if (!adapter.getVariables().isEmpty()) {
                SimpleDialog.of(activity).setTitle(TextParam.text("Delete all"))
                    .setMessage(TextParam.text("Really delete all variables?")).confirm((dd, i) -> adapter.clearAllVariables());
            }
        });

        binding.variablesAddnextchar.setText(getAddNextCharText());
        binding.variablesAddnextchar.setOnClickListener(d -> {
            final Character lmc = adapter.getVariables().getLowestMissingChar();
            if (lmc != null) {
                adapter.addVariable("" + lmc, null);
            }
        });

        binding.variablesInfo.setOnClickListener(d -> ShareUtils.openUrl(
            this.getContext(), "https://github.com/cgeo/cgeo/wiki/Calculator-Formula-Syntax", false));

        return binding;
    }

    private String getAddNextCharText() {
        if (adapter == null || adapter.getVariables() == null) {
            return "-";
        }
        final Character lmc = adapter.getVariables().getLowestMissingChar();
        return lmc == null ? "-" : "" + lmc;
    }

    @Override
    public long getPageId() {
        return CacheDetailActivity.Page.VARIABLES.id;
    }

    @Override
    public void setContent() {

        checkUnsavedChanges();

        // retrieve activity and cache - if either if this is null, something is really wrong...
        this.activity = (CacheDetailActivity) getActivity();
        if (activity == null) {
            return;
        }
        this.cache = activity.getCache();
        if (cache == null) {
            return;
        }
        adapter.setVariableList(cache.getVariables());
        binding.getRoot().setVisibility(View.VISIBLE);
    }

    private void checkUnsavedChanges() {
        if (cache != null && cache.getVariables() != null && cache.getVariables().wasModified()) {
            activity.ensureSaved();
            cache.getVariables().saveState();
        }
    }

    private void scanCache() {
        final List<String> toScan = new ArrayList<>();
        toScan.add(activity.getCache().getDescription());
        toScan.add(activity.getCache().getHint());
        for (Waypoint w : activity.getCache().getWaypoints()) {
            toScan.add(w.getNote());
        }
        final List<String> existingFormulas = new ArrayList<>();
        for (VariableMap.VariableState state : adapter.getItems()) {
            if (state != null && !StringUtils.isBlank(state.getFormulaString())) {
                existingFormulas.add(state.getFormulaString());
            }
        }
        final List<String> patterns = FormulaUtils.scanForFormulas(toScan, existingFormulas);
        SimpleDialog.of(activity).setTitle(TextParam.text("Choose found pattern"))
            .selectMultiple(patterns, (s, i) -> TextParam.text("`" + s + "`").setMarkdown(true), null, set -> {
                for (String s : set) {
                    adapter.addVariable(null, s);
                }
            });
    }

    @Override
    public void onPause() {
        super.onPause();
        //called e.g. when user swipes away from variables tab
        checkUnsavedChanges();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //called e.g. when user closes cache detail view
        checkUnsavedChanges();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //called e.g. when user closes cache detail view (after "onDestroy" is called)
        checkUnsavedChanges();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //called e.g. when user closes cache detail view (after "onDestroyView" is called)
        checkUnsavedChanges();
    }
}
