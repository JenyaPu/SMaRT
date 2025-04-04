package com.dm.smart;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dm.smart.items.Subject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecyclerViewAdapterSubjects extends RecyclerView.Adapter<RecyclerViewAdapterSubjects.ViewHolder> {

    static final int SUBJECT_CHANGE_NAME = Menu.FIRST;
    static final int SUBJECT_DELETE = Menu.FIRST + 1;
    private final List<Subject> mSubjects;
    private final LayoutInflater mInflater;
    private final Context mContext;
    private final String stringSubject;
    public int selectedSubjectPosition = 0;
    private ItemClickListener mClickListener;
    private boolean show_names;

    RecyclerViewAdapterSubjects(Context context, List<Subject> subjects, boolean show_names) {
        this.mInflater = LayoutInflater.from(context);
        this.mSubjects = subjects;
        this.mContext = context;
        this.show_names = show_names;
        this.stringSubject = context.getResources().getString(R.string.subject);
    }

    public void setShowNames(boolean show_names) {
        this.show_names = show_names;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ViewHolder(view);
    }

    // Bind the data to the TextView in each row
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Subject subject = mSubjects.get(position);
        if (show_names)
            holder.myTextView.setText(String.format("%s", subject.getName()));
        else
            holder.myTextView.setText(String.format(stringSubject + " %s", subject.getId()));
        if (selectedSubjectPosition == position) {
            holder.myTextView.setTextColor(ContextCompat.getColor(mContext, R.color.gray_dark));
            holder.myTextView.setTypeface(null, Typeface.BOLD);
        } else {
            holder.myTextView.setTextColor(ContextCompat.getColor(mContext, R.color.gray_light));
            holder.myTextView.setTypeface(null, Typeface.NORMAL);
        }
    }

    @Override
    public int getItemCount() {
        return mSubjects.size();
    }

    // Allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public Subject getItem(int position) {
        return mSubjects.get(position);
    }


    // Parent activity implements this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(int position);
    }

    // Store and recycle views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnCreateContextMenuListener {
        final TextView myTextView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.subject_name);
            itemView.setOnClickListener(this);
            itemView.setLongClickable(true);
            itemView.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) {
                mClickListener.onItemClick(getBindingAdapterPosition());
                DBAdapter DBAdapter = new DBAdapter(mContext);
                DBAdapter.open();
                Cursor cursorSinglePatient =
                        DBAdapter.getSubjectById(mSubjects.get(getBindingAdapterPosition()).getId());
                cursorSinglePatient.moveToFirst();
                MainActivity.currentlySelectedSubject = SubjectFragment.extractSubjectFromTheDB(cursorSinglePatient);
                cursorSinglePatient.close();
                DBAdapter.close();
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                        ContextMenu.ContextMenuInfo contextMenuInfo) {
            selectedSubjectPosition = getBindingAdapterPosition();
            int patientId = mSubjects.get(selectedSubjectPosition).getId();
            String bodyScheme = mSubjects.get(selectedSubjectPosition).getBodyScheme();
            String config = mSubjects.get(selectedSubjectPosition).getConfig();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(mSubjects.get(selectedSubjectPosition).getTimestamp());
            SimpleDateFormat formatter =
                    new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String dateString = formatter.format(cal.getTime());
            contextMenu.setHeaderTitle(patientId + ". " + mContext.getString(R.string.menu_config) + ": " + config + ", "
                    + bodyScheme + ",\n " + mContext.getString(R.string.menu_created) + ": " + dateString);
            contextMenu.add(0, SUBJECT_CHANGE_NAME, Menu.NONE,
                    mContext.getString(R.string.menu_change_name));
            contextMenu.add(0, SUBJECT_DELETE, Menu.NONE,
                    mContext.getString(R.string.menu_remove_subject));

        }
    }
}