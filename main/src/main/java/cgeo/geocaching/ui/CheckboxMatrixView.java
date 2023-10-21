package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.CheckboxmatrixViewBinding;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Action2;
import cgeo.geocaching.utils.functions.Action3;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

/** This view displays and maintains a matrix of checkboxes */
public class CheckboxMatrixView extends androidx.constraintlayout.widget.ConstraintLayout {

    private CheckboxmatrixViewBinding binding;
    private GridLayout matrixGrid;
    private String[] rows = null;
    private String[] cols = null;

    private boolean[][] data = null;
    private CheckBox[][] cbData = null;
    private CheckBox[] cbRowAll = null;
    private CheckBox[] cbColAll = null;
    private CheckBox cbAllAll = null;
    private boolean changeBlocked = false;
    private int[] cntRow = null;
    private int[] cntCol = null;
    private int cntAll;

    public CheckboxMatrixView(final Context context) {
        super(context);
        init();
    }

    public CheckboxMatrixView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CheckboxMatrixView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CheckboxMatrixView(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        final ContextThemeWrapper ctw = new ContextThemeWrapper(getContext(), R.style.cgeo);
        inflate(ctw, R.layout.checkboxmatrix_view, this);
        binding = CheckboxmatrixViewBinding.bind(this);
        matrixGrid = binding.matrix;
    }

    /**
     * Change data of the matrix. if clear=true, then matrix will be cleared before change is executed.
     * Use the given changeAction to execute changes. Call it with parameters (row, column, check)
     * to set field in a row/column to value "check"
     */
    public void changeData(final boolean clear, final Action1<Action3<Integer, Integer, Boolean>> changeAction) {
        final Action1<Action3<Integer, Integer, Boolean>> realChangeAction = clear ?
                changer -> {
                    executeFor(-1, -1, (r, c) -> changer.call(r, c, false));
                    changeAction.call(changer);
                } :
              changeAction;
        changeDataInternal(realChangeAction);
    }

    /**
     * Convevience method to execute an action on multiple matrix fields.
     * - When row and col are >=0, change will be executed on specific field.
     * - When row or col is <0 and the other one is >=0 then change will be executed for all fields in specific row/col
     * - When both row and col are <0 then change will be executed for all fields
     */
    public void executeFor(final int row, final int col, final Action2<Integer, Integer> action) {
        for (int r = (Math.max(row, 0)); r < (row < 0 ? rows.length : row + 1); r++) {
            for (int c = (Math.max(col, 0)); c < (col < 0 ? cols.length : col + 1); c++) {
                action.call(r, c);
            }
        }
    }

    /** returns actual matrix data. Do not change the returned array, it is for reading only! */
    public boolean[][] getData() {
        return data;
    }

    public void setLabels(final String rowLabel, final String columnLabel) {
        binding.rowsLabel.setText(rowLabel);
        binding.colsLabel.setText(columnLabel);
        rotateTextView(binding.rowsLabel);
    }

    private static void rotateTextView(final TextView textView) {
        //measure normal text width
        textView.setRotation(0);
        textView.measure(0, 0);
        final int width = textView.getMeasuredWidth();
        textView.setRotation(-90);
        textView.setTranslationX((float) -(width - ViewUtils.spToPixel(14)) / 2);
        textView.requestLayout();
    }

    /** (re-)initializes the matrix with given data. Matrix data is deleted in the process */
    public void setRowsColumns(final String[] rows, final String[] cols) {

        final int addCells = 3;
        this.rows = rows;
        this.cols = cols;

        //initialize the data fields
        matrixGrid.setRowCount(rows.length + addCells);
        matrixGrid.setColumnCount(cols.length + addCells);
        this.data = new boolean[rows.length][cols.length];
        this.cbData = new CheckBox[rows.length][cols.length];
        this.cbRowAll = new CheckBox[rows.length];
        this.cbColAll = new CheckBox[cols.length];
        this.cbAllAll = null;
        this.cntRow = new int[rows.length];
        this.cntCol = new int[cols.length];
        this.cntAll = 0;

        //initialize the matrix view
        matrixGrid.removeAllViews();
        //top row: column labels
        this.addTextView("");
        this.addIconView(R.drawable.ic_menu_selectall);
        this.addVerticalSeparatorView();
        for (String c : cols) {
            this.addTextView(c);
        }
        //second row: selects for whole cols
        this.addIconView(R.drawable.ic_menu_selectall);
        this.cbAllAll = this.addCheckbox();
        this.cbAllAll.setOnCheckedChangeListener((v, ch) -> changeDataInternal(changer -> executeFor(-1, -1, (r, c) -> changer.call(r, c, ch))));
        this.addVerticalSeparatorView();
        for (int c = 0; c < cols.length; c++) {
            final int fC = c;
            this.cbColAll[c] = this.addCheckbox();
            this.cbColAll[c].setOnCheckedChangeListener((v, ch) -> changeDataInternal(changer -> executeFor(-1, fC, (rr, cc) -> changer.call(rr, fC, ch))));
        }
        //third row: separators
        for (int c = 0; c < cols.length + addCells; c++) {
            this.addHorizontalSeparatorView();
        }

        //fourth and more rows: data rows
        for (int r = 0; r < rows.length; r++) {
            final int fR = r;
            this.addTextView(rows[r]);
            this.cbRowAll[r] = this.addCheckbox();
            this.cbRowAll[r].setOnCheckedChangeListener((v, ch) -> changeDataInternal(changer -> executeFor(fR, -1, (rr, cc) -> changer.call(fR, cc, ch))));
            this.addVerticalSeparatorView();
            for (int c = 0; c < cols.length; c++) {
                final int fC = c;
                this.cbData[r][c] = this.addCheckbox();
                this.cbData[r][c].setOnCheckedChangeListener((v, ch) -> changeDataInternal(changer -> changer.call(fR, fC, ch)));
            }
        }
    }

    private void addTextView(final String text) {
        final TextView tv = ViewUtils.createTextItem(getContext(), R.style.text_label, TextParam.text(text));
        tv.setGravity(Gravity.CENTER);
        matrixGrid.addView(tv, createLayoutParams());
    }

    private void addIconView(final int iconId) {
        final ImageView iv = ViewUtils.createIconView(getContext(), iconId);
        iv.setMaxWidth(ViewUtils.spToPixel(12));
        iv.setMaxHeight(ViewUtils.spToPixel(12));
        matrixGrid.addView(iv, createLayoutParams());
    }

    private CheckBox addCheckbox() {
        final CheckBox cb = new CheckBox(getContext());
        matrixGrid.addView(cb, createLayoutParams());
        return cb;
    }

    private void addVerticalSeparatorView() {
        addHorizontalSeparatorView();
    }

    private void addHorizontalSeparatorView() {
        matrixGrid.addView(ViewUtils.createExpandableSeparatorPixelView(getContext()), createLayoutParams());
    }

    private GridLayout.LayoutParams createLayoutParams() {
        return new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL), GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL));
    }


    // methods for data change execution. Includes correct visualization/handling of "all" checkboxes

    private void changeDataInternal(final Action1<Action3<Integer, Integer, Boolean>> changeAction) {
        if (changeBlocked) {
            return;
        }
        //block excution of other "onChange"-actions on Checkboxes during the changeData-processing
        changeBlocked = true;

        //change the actual data
        changeAction.call(this::changeDataField);

        //change the "all" checkboxes
        for (int r = 0; r < rows.length; r++) {
            cbRowAll[r].setChecked(cntRow[r] == rows.length);
        }
        for (int c = 0; c < cols.length; c++) {
            cbColAll[c].setChecked(cntCol[c] == cols.length);
        }
        cbAllAll.setChecked(cntAll == cols.length * rows.length);

        //the onchange-actions of the "setChecked"-calls above are executed asynchronously.
        // -> Disable block after those have executed
        this.post(() -> changeBlocked = false);
    }

    private void changeDataField(final int row, final int col, final boolean check) {
        if (row < 0 || row >= rows.length || col < 0 || col >= cols.length) {
            return;
        }
        if (data[row][col] == check) {
            return;
        }
        data[row][col] = check;
        cntRow[row] += check ? 1 : -1;
        cntCol[col] += check ? 1 : -1;
        cntAll += check ? 1 : -1;

        cbData[row][col].setChecked(check);
    }

}
